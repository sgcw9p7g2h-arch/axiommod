using System;
using System.IO;
using System.Diagnostics;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text.Json;
using System.Linq;
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
    private const int MaxProfiles = 5;
    private readonly LauncherProfile[] _profiles = Enumerable.Range(1, MaxProfiles)
        .Select(i => new LauncherProfile { Name = $"Profile {i}" })
        .ToArray();
    private int _selectedProfileIndex;

    public MainWindow()
    {
        InitializeComponent();
        _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("AxiomLauncher/0.1");
        _httpClient.DefaultRequestHeaders.Accept.ParseAdd("application/vnd.github+json");

        var gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".axiom");
        GameDirBox.Text = gameDir;
        _settingsFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Axiom", "launcher-settings.json");
        VersionBox.SelectedIndex = 0;
        InitializeProfiles();
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
            MessageBox.Show($"We couldn't sign you in.\n\n{ex.Message}", "Axiom — Microsoft Login",
                MessageBoxButton.OK, MessageBoxImage.Error);
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
            SaveCurrentProfile();
            var gameDirectory = GameDirBox.Text.Trim();
            if (string.IsNullOrWhiteSpace(gameDirectory))
                throw new InvalidOperationException("Choose a Minecraft game directory before launching.");

            Directory.CreateDirectory(gameDirectory);
            GameDirBox.Text = gameDirectory;
            SaveSettings();
            var path = new MinecraftPath(gameDirectory);
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
            if (string.Equals(localDigest, remoteDigest.Replace("sha256:", "", StringComparison.OrdinalIgnoreCase),
                StringComparison.OrdinalIgnoreCase))
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

    private void InitializeProfiles()
    {
        ProfileBox.Items.Clear();
        for (var i = 0; i < MaxProfiles; i++)
            ProfileBox.Items.Add(_profiles[i].Name);
        ProfileBox.SelectedIndex = 0;
    }

    private void ProfileBox_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
    {
        if (!IsLoaded || ProfileBox.SelectedIndex < 0 || ProfileBox.SelectedIndex >= MaxProfiles) return;
        SaveCurrentProfile();
        _selectedProfileIndex = ProfileBox.SelectedIndex;
        var profile = _profiles[_selectedProfileIndex];
        GameDirBox.Text = profile.GameDirectory;
        RamBox.SelectedIndex = profile.RamMb switch { 2048 => 0, 4096 => 1, 6144 => 2, 8192 => 3, _ => 1 };
    }

    private void SaveProfileButton_Click(object sender, RoutedEventArgs e)
    {
        SaveCurrentProfile();
        SaveSettings();
        StatusText.Text = $"{_profiles[_selectedProfileIndex].Name} saved.";
    }

    private void SaveCurrentProfile()
    {
        if (ProfileBox.SelectedIndex < 0 || ProfileBox.SelectedIndex >= MaxProfiles) return;
        _selectedProfileIndex = ProfileBox.SelectedIndex;
        var profile = _profiles[_selectedProfileIndex];
        profile.GameDirectory = GameDirBox.Text;
        profile.RamMb = GetSelectedRamMb();
    }

    private void LoadSettings()
    {
        try
        {
            if (!File.Exists(_settingsFile)) return;
            var settings = JsonSerializer.Deserialize<LauncherSettings>(File.ReadAllText(_settingsFile));
            if (settings == null) return;
            if (settings.Profiles?.Length == MaxProfiles)
            {
                for (var i = 0; i < MaxProfiles; i++)
                {
                    _profiles[i] = settings.Profiles[i];
                    ProfileBox.Items[i] = _profiles[i].Name;
                }
            }
            if (settings.Profiles?.Length != MaxProfiles && (settings.GameDirectory is not null || settings.RamMb > 0))
            {
                // Migrate the pre-profile settings format into Profile 1.
                _profiles[0].GameDirectory = string.IsNullOrWhiteSpace(settings.GameDirectory)
                    ? GameDirBox.Text
                    : settings.GameDirectory;
                _profiles[0].RamMb = settings.RamMb is 2048 or 4096 or 6144 or 8192
                    ? settings.RamMb
                    : 4096;
                ProfileBox.Items[0] = _profiles[0].Name;
            }

            _selectedProfileIndex = Math.Clamp(settings.SelectedProfile, 0, MaxProfiles - 1);
            ProfileBox.SelectedIndex = _selectedProfileIndex;
            var selected = _profiles[_selectedProfileIndex];
            GameDirBox.Text = selected.GameDirectory;
            RamBox.SelectedIndex = selected.RamMb switch { 2048 => 0, 4096 => 1, 6144 => 2, 8192 => 3, _ => 1 };
        }
        catch { }
    }

    private void SaveSettings()
    {
        try
        {
            var directory = Path.GetDirectoryName(_settingsFile);
            if (!string.IsNullOrEmpty(directory)) Directory.CreateDirectory(directory);
            SaveCurrentProfile();
            var settings = new LauncherSettings { Profiles = _profiles, SelectedProfile = _selectedProfileIndex };
            File.WriteAllText(_settingsFile, JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch { }
    }

    private sealed class LauncherSettings
    {
        public LauncherProfile[] Profiles { get; set; } = Array.Empty<LauncherProfile>();
        public int SelectedProfile { get; set; }

        // Legacy settings fields retained so older Axiom installs migrate cleanly.
        public string? GameDirectory { get; set; }
        public int RamMb { get; set; }
    }

    private sealed class LauncherProfile
    {
        public string Name { get; set; } = "Profile";
        public string GameDirectory { get; set; } = string.Empty;
        public int RamMb { get; set; } = 4096;
    }

    private void CurseForgeButton_Click(object sender, RoutedEventArgs e)
    {
        OpenExternal("https://www.curseforge.com/minecraft/search?class=mc-mods");
        ContentStatusText.Text = "Opened CurseForge Minecraft mods.";
    }

    private void ModrinthButton_Click(object sender, RoutedEventArgs e)
    {
        OpenExternal("https://modrinth.com/discover/mods");
        ContentStatusText.Text = "Opened Modrinth Minecraft mods.";
    }

    private void TexturePacksButton_Click(object sender, RoutedEventArgs e)
    {
        OpenExternal("https://modrinth.com/discover/resourcepacks");
        ContentStatusText.Text = "Opened Modrinth resource packs. A direct pack.com connector can be added once the exact service is confirmed.";
    }

    private void DiscordButton_Click(object sender, RoutedEventArgs e)
    {
        OpenExternal("https://discord.com/");
        CommunityStatusText.Text = "Discord connection setup opened. OAuth linking will be enabled with the Axiom service.";
    }

    private void TikTokButton_Click(object sender, RoutedEventArgs e)
    {
        OpenExternal("https://www.tiktok.com/");
        CommunityStatusText.Text = "TikTok connection setup opened. OAuth linking will be enabled with the Axiom service.";
    }

    private static void OpenExternal(string url)
    {
        Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
    }

    private void SettingsButton_Click(object sender, RoutedEventArgs e)
    {
        SaveSettings();
        MessageBox.Show("Your Axiom game directory and RAM setting are saved automatically.", "Axiom Launcher");
    }
}