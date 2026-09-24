using System;
using System.IO;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text.Json;
using System.Windows;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.Auth.Microsoft;
using CmlLib.Core.ModLoaders.FabricMC;
using CmlLib.Core.ProcessBuilder;

namespace AxiomLauncher;

public partial class MainWindow : Window
{
    private const string MinecraftVersion = "1.21.11";
    private const string FabricLoaderVersion = "0.18.1";
    private const string FabricApiVersion = "0.141.3+1.21.11";
    private const string AxiomAssetName = "axiom.jar";
    private const string LatestReleaseApi = "https://api.github.com/repos/sgcw9p7g2h-arch/axiommod/releases/latest";

    private MinecraftLauncher? _launcher;
    private MSession? _session;
    private readonly JELoginHandler _loginHandler = JELoginHandlerBuilder.BuildDefault();
    private readonly HttpClient _httpClient = new();
    private readonly string _settingsFile;

    public MainWindow()
    {
        InitializeComponent();
        _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("AxiomLauncher/0.1");
        _httpClient.DefaultRequestHeaders.Accept.ParseAdd("application/vnd.github+json");

        var gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".axiom");
        GameDirBox.Text = gameDir;
        _settingsFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Axiom", "launcher-settings.json");
        VersionBox.SelectedIndex = 0;
        LoadSettings();
    }

    private async void LoginButton_Click(object sender, RoutedEventArgs e)
    {
        LoginButton.IsEnabled = false;
        LaunchButton.IsEnabled = false;
        StatusText.Text = "Opening Microsoft sign-in...";

        try
        {
            var session = await _loginHandler.Authenticate();
            _session = session;
            AccountText.Text = $"Signed in as {session.Username}";
            StatusText.Text = "Microsoft account connected.";
            LoginButton.Content = "SIGNED IN";
        }
        catch (OperationCanceledException)
        {
            StatusText.Text = "Microsoft sign-in canceled.";
            AccountText.Text = "Not signed in";
        }
        catch (Exception ex)
        {
            StatusText.Text = "Microsoft sign-in failed.";
            AccountText.Text = "Not signed in";
            MessageBox.Show(
                $"We couldn't sign you in.\n\n{ex.Message}",
                "Axiom — Microsoft Login",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
        }
        finally
        {
            LoginButton.IsEnabled = true;
            LaunchButton.IsEnabled = _session != null;
        }
    }

    private async void LaunchButton_Click(object sender, RoutedEventArgs e)
    {
        if (_session == null)
        {
            MessageBox.Show("Sign in with Microsoft first.", "Axiom Launcher",
                MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        LaunchButton.IsEnabled = false;
        LoginButton.IsEnabled = false;
        StatusText.Text = "Preparing Axiom...";

        try
        {
            SaveSettings();
            var path = new MinecraftPath(GameDirBox.Text);
            _launcher = new MinecraftLauncher(path);

            StatusText.Text = $"Installing Minecraft {MinecraftVersion}...";
            await _launcher.InstallAsync(MinecraftVersion);

            StatusText.Text = $"Installing Fabric Loader {FabricLoaderVersion}...";
            var fabricInstaller = new FabricInstaller(_httpClient);
            var fabricVersionName = await fabricInstaller.Install(MinecraftVersion, FabricLoaderVersion, path);

            StatusText.Text = "Installing Fabric API...";
            await EnsureFabricApiAsync(path);

            StatusText.Text = "Installing Axiom Client...";
            await EnsureLatestAxiomModAsync(path);

            StatusText.Text = "Starting Axiom...";
            var options = new MLaunchOption { Session = _session, MaximumRamMb = GetSelectedRamMb() };
            var process = await _launcher.BuildProcessAsync(fabricVersionName, options);
            process.Start();

            StatusText.Text = "Axiom started.";
            Close();
        }
        catch (Exception ex)
        {
            StatusText.Text = "Launch failed.";
            MessageBox.Show(ex.Message, "Axiom Launcher", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            LaunchButton.IsEnabled = _session != null;
            LoginButton.IsEnabled = true;
        }
    }

    private async Task EnsureFabricApiAsync(MinecraftPath path)
    {
        var modsDirectory = Path.Combine(path.BasePath, "mods");
        Directory.CreateDirectory(modsDirectory);
        var fileName = $"fabric-api-{FabricApiVersion}.jar";
        var destination = Path.Combine(modsDirectory, fileName);
        var url = $"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{FabricApiVersion}/{fileName}";
        if (!File.Exists(destination))
            await DownloadFileAsync(url, destination);
    }

    private async Task EnsureLatestAxiomModAsync(MinecraftPath path)
    {
        var releaseJson = await _httpClient.GetStringAsync(LatestReleaseApi);
        using var document = JsonDocument.Parse(releaseJson);
        if (!document.RootElement.TryGetProperty("assets", out var assets))
            throw new InvalidOperationException("The Axiom release has no downloadable assets.");

        JsonElement asset = default;
        foreach (var candidate in assets.EnumerateArray())
        {
            if (candidate.TryGetProperty("name", out var name) &&
                string.Equals(name.GetString(), AxiomAssetName, StringComparison.OrdinalIgnoreCase))
            {
                asset = candidate;
                break;
            }
        }

        if (asset.ValueKind == JsonValueKind.Undefined)
            throw new InvalidOperationException("The latest Axiom release does not contain axiom.jar yet.");

        var downloadUrl = asset.GetProperty("browser_download_url").GetString();
        if (string.IsNullOrWhiteSpace(downloadUrl))
            throw new InvalidOperationException("The Axiom release asset has no download URL.");

        var remoteDigest = asset.TryGetProperty("digest", out var digestElement) ? digestElement.GetString() : null;
        var modsDirectory = Path.Combine(path.BasePath, "mods");
        Directory.CreateDirectory(modsDirectory);
        var destination = Path.Combine(modsDirectory, AxiomAssetName);

        if (!string.IsNullOrWhiteSpace(remoteDigest) && File.Exists(destination))
        {
            var localDigest = await ComputeSha256Async(destination);
            if (string.Equals(localDigest, remoteDigest.Replace("sha256:", "", StringComparison.OrdinalIgnoreCase), StringComparison.OrdinalIgnoreCase))
                return;
        }

        await DownloadFileAsync(downloadUrl, destination);

        if (!string.IsNullOrWhiteSpace(remoteDigest))
        {
            var actualDigest = await ComputeSha256Async(destination);
            var expectedDigest = remoteDigest.Replace("sha256:", "", StringComparison.OrdinalIgnoreCase);
            if (!string.Equals(actualDigest, expectedDigest, StringComparison.OrdinalIgnoreCase))
            {
                File.Delete(destination);
                throw new InvalidOperationException("The downloaded Axiom client failed its SHA-256 integrity check.");
            }
        }
    }

    private async Task DownloadFileAsync(string url, string destination)
    {
        using var response = await _httpClient.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
        response.EnsureSuccessStatusCode();
        await using var input = await response.Content.ReadAsStreamAsync();
        await using var output = File.Create(destination);
        await input.CopyToAsync(output);
    }

    private static async Task<string> ComputeSha256Async(string path)
    {
        await using var stream = File.OpenRead(path);
        return Convert.ToHexString(await SHA256.HashDataAsync(stream)).ToLowerInvariant();
    }

    private int GetSelectedRamMb() => RamBox.SelectedIndex switch
    {
        0 => 2048, 1 => 4096, 2 => 6144, 3 => 8192, _ => 4096
    };

    private void LoadSettings()
    {
        try
        {
            if (!File.Exists(_settingsFile)) return;
            var settings = JsonSerializer.Deserialize<LauncherSettings>(File.ReadAllText(_settingsFile));
            if (settings == null) return;
            if (!string.IsNullOrWhiteSpace(settings.GameDirectory)) GameDirBox.Text = settings.GameDirectory;
            RamBox.SelectedIndex = settings.RamMb switch { 2048 => 0, 4096 => 1, 6144 => 2, 8192 => 3, _ => 1 };
        }
        catch { }
    }

    private void SaveSettings()
    {
        try
        {
            var directory = Path.GetDirectoryName(_settingsFile);
            if (!string.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);
            var settings = new LauncherSettings { GameDirectory = GameDirBox.Text, RamMb = GetSelectedRamMb() };
            File.WriteAllText(_settingsFile, JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch { }
    }

    private sealed class LauncherSettings
    {
        public string GameDirectory { get; set; } = string.Empty;
        public int RamMb { get; set; } = 4096;
    }

    private void SettingsButton_Click(object sender, RoutedEventArgs e)
    {
        SaveSettings();
        MessageBox.Show("Your Axiom game directory and RAM setting are saved automatically.", "Axiom Launcher");
    }
}