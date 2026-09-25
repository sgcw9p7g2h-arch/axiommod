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
    private const string LatestReleaseApi = "https://api.github.com/repos/sgcw9p7g2h-arch/axiommod/releases/latest";
    private const string ClientManifestAssetName = "axiom-client.json";
    private const string LauncherAssetName = "AxiomLauncher.exe";
    private ClientManifest? _clientManifest;

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
        _httpClient.Timeout = TimeSpan.FromMinutes(10);
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
            System.Windows.MessageBox.Show($"We couldn't sign you in.\n\n{ex.Message}", "Axiom — Microsoft Login",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            LoginButton.IsEnabled = true;
            LaunchButton.IsEnabled = _session != null;
        }
    }

    private async void RepairButton_Click(object sender, RoutedEventArgs e)
    {
        RepairButton.IsEnabled = false;
        LaunchButton.IsEnabled = false;
        LoginButton.IsEnabled = false;
        StatusText.Text = "Checking Axiom installation...";

        try
        {
            SaveCurrentProfile();
            var gameDirectory = GameDirBox.Text.Trim();
            if (string.IsNullOrWhiteSpace(gameDirectory))
                throw new InvalidOperationException("Choose a Minecraft game directory before repairing.");

            Directory.CreateDirectory(gameDirectory);
            SaveSettings();

            var path = new MinecraftPath(gameDirectory);
            _launcher = new MinecraftLauncher(path);
            _clientManifest = await LoadClientManifestAsync();

            VersionBox.Items.Clear();
            VersionBox.Items.Add(_clientManifest.MinecraftVersion);
            VersionBox.SelectedIndex = 0;

            StatusText.Text = $"Repairing Axiom {_clientManifest.ClientVersion}...";
            await _launcher.InstallAsync(_clientManifest.MinecraftVersion);

            StatusText.Text = $"Installing Fabric Loader {_clientManifest.FabricLoaderVersion}...";
            var fabricInstaller = new FabricInstaller(_httpClient);
            await fabricInstaller.Install(_clientManifest.MinecraftVersion, _clientManifest.FabricLoaderVersion, path);

            StatusText.Text = "Checking Fabric API...";
            await EnsureFabricApiAsync(path, _clientManifest);

            StatusText.Text = $"Checking Axiom {_clientManifest.ClientVersion}...";
            await EnsureLatestAxiomModAsync(path, _clientManifest);

            await WriteRuntimeStateAsync(path, _clientManifest);

            StatusText.Text = "Axiom installation repaired.";
        }
        catch (Exception ex)
        {
            StatusText.Text = "Repair failed.";
            System.Windows.MessageBox.Show(ex.Message, "Axiom Repair", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            RepairButton.IsEnabled = true;
            LaunchButton.IsEnabled = true;
            LoginButton.IsEnabled = true;
        }
    }

    private async Task SetRuntimeStatusAsync(MinecraftPath path, ClientManifest manifest)
    {
        var ready = await IsRuntimeReadyAsync(path, manifest);
        StatusText.Text = ready ? $"Axiom {manifest.ClientVersion} • Ready" : $"Axiom {manifest.ClientVersion} • Needs repair";
    }

    private async void LaunchButton_Click(object sender, RoutedEventArgs e)
    {
        if (_session == null)
        {
            System.Windows.MessageBox.Show("Sign in with Microsoft first.", "Axiom Launcher",
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
            _launcher.FileProgressChanged += (_, args) =>
            {
                Dispatcher.Invoke(() =>
                {
                    var total = args.TotalTasks <= 0 ? 1 : args.TotalTasks;
                    StatusText.Text = $"{args.Name} ({args.ProgressedTasks}/{total})";
                });
            };

            _clientManifest = await LoadClientManifestAsync();
            VersionBox.Items.Clear();
            VersionBox.Items.Add(_clientManifest.MinecraftVersion);
            VersionBox.SelectedIndex = 0;
            StatusText.Text = $"Axiom {_clientManifest.ClientVersion} • Minecraft {_clientManifest.MinecraftVersion}";
            await _launcher.InstallAsync(_clientManifest.MinecraftVersion);

            StatusText.Text = $"Installing Fabric Loader {_clientManifest.FabricLoaderVersion}...";
            var fabricInstaller = new FabricInstaller(_httpClient);
            var fabricVersionName = await fabricInstaller.Install(_clientManifest.MinecraftVersion, _clientManifest.FabricLoaderVersion, path);

            StatusText.Text = "Installing Fabric API...";
            await EnsureFabricApiAsync(path, _clientManifest);

            StatusText.Text = $"Installing Axiom {_clientManifest.ClientVersion}...";
            await EnsureLatestAxiomModAsync(path, _clientManifest);

            if (!await IsRuntimeReadyAsync(path, _clientManifest))
            {
                StatusText.Text = "Repairing Axiom runtime...";
                await EnsureFabricApiAsync(path, _clientManifest);
                await EnsureLatestAxiomModAsync(path, _clientManifest);
                await WriteRuntimeStateAsync(path, _clientManifest);
            }

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
            System.Windows.MessageBox.Show(ex.Message, "Axiom Launcher", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            LaunchButton.IsEnabled = _session != null;
            LoginButton.IsEnabled = true;
        }
    }

    private async Task EnsureFabricApiAsync(MinecraftPath path, ClientManifest manifest)
    {
        var modsDirectory = Path.Combine(path.BasePath, "mods");
        Directory.CreateDirectory(modsDirectory);
        var fileName = $"fabric-api-{manifest.FabricApiVersion}.jar";
        var destination = Path.Combine(modsDirectory, fileName);
        var url = $"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{manifest.FabricApiVersion}/{fileName}";
        if (!File.Exists(destination))
            await DownloadFileAsync(url, destination);
    }

    private async Task EnsureLatestAxiomModAsync(MinecraftPath path, ClientManifest manifest)
    {
        if (!IsSafeAssetName(manifest.ClientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        var releaseJson = await _httpClient.GetStringAsync(LatestReleaseApi);
        using var document = JsonDocument.Parse(releaseJson);
        if (!document.RootElement.TryGetProperty("assets", out var assets))
            throw new InvalidOperationException("The Axiom release has no downloadable assets.");

        JsonElement asset = default;
        foreach (var candidate in assets.EnumerateArray())
        {
            if (candidate.TryGetProperty("name", out var name) &&
                string.Equals(name.GetString(), manifest.ClientAsset, StringComparison.OrdinalIgnoreCase))
            {
                asset = candidate;
                break;
            }
        }

        if (asset.ValueKind == JsonValueKind.Undefined)
            throw new InvalidOperationException($"The latest Axiom release does not contain {manifest.ClientAsset} yet.");

        var downloadUrl = asset.GetProperty("browser_download_url").GetString();
        if (string.IsNullOrWhiteSpace(downloadUrl))
            throw new InvalidOperationException("The Axiom release asset has no download URL.");

        var remoteDigest = asset.TryGetProperty("digest", out var digestElement) ? digestElement.GetString() : null;
        var modsDirectory = Path.Combine(path.BasePath, "mods");
        Directory.CreateDirectory(modsDirectory);
        var destination = Path.Combine(modsDirectory, manifest.ClientAsset);

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

    private async Task WriteRuntimeStateAsync(MinecraftPath path, ClientManifest manifest)
    {
        if (!IsSafeAssetName(manifest.ClientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        var assetPath = Path.Combine(path.BasePath, "mods", manifest.ClientAsset);
        if (!File.Exists(assetPath))
            throw new InvalidOperationException("The Axiom client asset is missing after installation.");

        var digest = await ComputeSha256Async(assetPath);
        var state = ClientRuntimeState.FromManifest(
            manifest.ClientVersion,
            manifest.MinecraftVersion,
            manifest.FabricLoaderVersion,
            manifest.FabricApiVersion,
            manifest.ClientAsset,
            digest);

        await state.SaveAsync(Path.Combine(path.BasePath, ".axiom", "client-state.json"));
    }

    private async Task<bool> IsRuntimeReadyAsync(MinecraftPath path, ClientManifest manifest)
    {
        if (!IsSafeAssetName(manifest.ClientAsset))
            return false;

        var statePath = Path.Combine(path.BasePath, ".axiom", "client-state.json");
        var state = await ClientRuntimeState.LoadAsync(statePath);
        if (state == null)
            return false;

        var assetPath = Path.Combine(path.BasePath, "mods", manifest.ClientAsset);
        if (!File.Exists(assetPath))
            return false;

        var digest = await ComputeSha256Async(assetPath);
        return state.Matches(
            manifest.ClientVersion,
            manifest.MinecraftVersion,
            manifest.FabricLoaderVersion,
            manifest.FabricApiVersion,
            manifest.ClientAsset,
            digest);
    }

    private async Task<ClientManifest> LoadClientManifestAsync()
    {
        var releaseJson = await _httpClient.GetStringAsync(LatestReleaseApi);
        using var document = JsonDocument.Parse(releaseJson);
        if (!document.RootElement.TryGetProperty("assets", out var assets))
            throw new InvalidOperationException("The latest Axiom release has no assets.");

        JsonElement asset = default;
        foreach (var candidate in assets.EnumerateArray())
        {
            if (candidate.TryGetProperty("name", out var name) &&
                string.Equals(name.GetString(), ClientManifestAssetName, StringComparison.OrdinalIgnoreCase))
            {
                asset = candidate;
                break;
            }
        }

        if (asset.ValueKind == JsonValueKind.Undefined)
            throw new InvalidOperationException("The latest Axiom release is missing its client manifest.");

        var downloadUrl = asset.GetProperty("browser_download_url").GetString();
        if (string.IsNullOrWhiteSpace(downloadUrl))
            throw new InvalidOperationException("The Axiom client manifest has no download URL.");

        var manifestJson = await _httpClient.GetStringAsync(downloadUrl);
        var remoteDigest = asset.TryGetProperty("digest", out var digestElement) ? digestElement.GetString() : null;
        if (!string.IsNullOrWhiteSpace(remoteDigest))
        {
            var actualDigest = Convert.ToHexString(SHA256.HashData(System.Text.Encoding.UTF8.GetBytes(manifestJson))).ToLowerInvariant();
            var expectedDigest = remoteDigest.Replace("sha256:", "", StringComparison.OrdinalIgnoreCase);
            if (!string.Equals(actualDigest, expectedDigest, StringComparison.OrdinalIgnoreCase))
                throw new InvalidOperationException("The downloaded Axiom client manifest failed its SHA-256 integrity check.");
        }

        var manifest = JsonSerializer.Deserialize<ClientManifest>(manifestJson);
        if (manifest == null ||
            !IsValidVersion(manifest.ClientVersion) ||
            !IsValidVersion(manifest.MinecraftVersion) ||
            !IsValidVersion(manifest.FabricLoaderVersion) ||
            !IsValidVersion(manifest.FabricApiVersion) ||
            !IsSafeAssetName(manifest.ClientAsset) ||
            !manifest.ClientAsset.EndsWith(".jar", StringComparison.OrdinalIgnoreCase) ||
            !IsSafeAssetName(manifest.LauncherAsset) ||
            !manifest.LauncherAsset.EndsWith(".exe", StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("The Axiom client manifest is invalid.");

        return manifest;
    }

    private static bool IsValidVersion(string value)
    {
        if (string.IsNullOrWhiteSpace(value) || value.Length > 64)
            return false;

        foreach (var character in value)
        {
            if (!(char.IsLetterOrDigit(character) || character is '.' or '-' or '_' or '+'))
                return false;
        }

        return true;
    }

    private static bool IsSafeAssetName(string value) =>
        !string.IsNullOrWhiteSpace(value) &&
        value.Length <= 128 &&
        value.IndexOfAny(new[] { '/', '\\' }) < 0 &&
        value != "." &&
        value != "..";

    private async Task DownloadFileAsync(string url, string destination)
    {
        var directory = Path.GetDirectoryName(destination);
        if (!string.IsNullOrEmpty(directory))
            Directory.CreateDirectory(directory);

        var temporary = destination + ".download";
        try
        {
            using var response = await _httpClient.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
            response.EnsureSuccessStatusCode();
            await using var input = await response.Content.ReadAsStreamAsync();
            await using var output = new FileStream(temporary, FileMode.Create, FileAccess.Write, FileShare.None);
            await input.CopyToAsync(output);
            await output.FlushAsync();

            File.Move(temporary, destination, true);
        }
        catch
        {
            try
            {
                if (File.Exists(temporary)) File.Delete(temporary);
            }
            catch { }
            throw;
        }
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

    private void RenameProfileButton_Click(object sender, RoutedEventArgs e)
    {
        if (_selectedProfileIndex < 0 || _selectedProfileIndex >= MaxProfiles) return;

        var current = _profiles[_selectedProfileIndex].Name;
        var dialog = new Window
        {
            Title = "Rename Axiom Profile",
            Width = 420,
            Height = 190,
            WindowStartupLocation = WindowStartupLocation.CenterOwner,
            Owner = this,
            ResizeMode = ResizeMode.NoResize,
            Background = (System.Windows.Media.Brush)FindResource("AxiomPanel"),
            Foreground = (System.Windows.Media.Brush)FindResource("AxiomText")
        };

        var root = new System.Windows.Controls.StackPanel { Margin = new Thickness(20) };
        root.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = "Profile name",
            FontSize = 16,
            FontWeight = FontWeights.SemiBold
        });

        var input = new System.Windows.Controls.TextBox
        {
            Text = current,
            Height = 36,
            Margin = new Thickness(0, 10, 0, 14)
        };
        root.Children.Add(input);

        var buttons = new System.Windows.Controls.StackPanel
        {
            Orientation = System.Windows.Controls.Orientation.Horizontal,
            HorizontalAlignment = System.Windows.HorizontalAlignment.Right
        };
        var cancel = new System.Windows.Controls.Button { Content = "CANCEL", Padding = new Thickness(14, 7, 14, 7), Margin = new Thickness(0, 0, 8, 0) };
        cancel.Click += (_, _) => dialog.DialogResult = false;
        var save = new System.Windows.Controls.Button { Content = "SAVE", Padding = new Thickness(14, 7, 14, 7) };
        save.Click += (_, _) =>
        {
            var name = input.Text.Trim();
            if (name.Length == 0)
            {
                System.Windows.MessageBox.Show("Enter a profile name.", "Axiom Profile", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            _profiles[_selectedProfileIndex].Name = name;
            ProfileBox.Items[_selectedProfileIndex] = name;
            ProfileBox.SelectedIndex = _selectedProfileIndex;
            SaveSettings();
            StatusText.Text = $"Profile renamed to {name}.";
            dialog.DialogResult = true;
        };
        buttons.Children.Add(cancel);
        buttons.Children.Add(save);
        root.Children.Add(buttons);

        dialog.Content = root;
        input.Focus();
        input.SelectAll();
        dialog.ShowDialog();
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
            var temporary = _settingsFile + ".tmp";
            File.WriteAllText(temporary, JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true }));
            File.Move(temporary, _settingsFile, true);
        }
        catch
        {
            try
            {
                var temporary = _settingsFile + ".tmp";
                if (File.Exists(temporary)) File.Delete(temporary);
            }
            catch { }
        }
    }

    private sealed class ClientManifest
    {
        public string ClientVersion { get; set; } = "0.1.0";
        public string MinecraftVersion { get; set; } = string.Empty;
        public string FabricLoaderVersion { get; set; } = string.Empty;
        public string FabricApiVersion { get; set; } = string.Empty;
        public string ClientAsset { get; set; } = string.Empty;
        public string LauncherAsset { get; set; } = string.Empty;
    }

    private sealed class LauncherSettings
    {
        public LauncherProfile[] Profiles { get; set; } = Array.Empty<LauncherProfile>();
        public int SelectedProfile { get; set; }
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

    private void BrowseGameDirectoryButton_Click(object sender, RoutedEventArgs e)
    {
        using var dialog = new System.Windows.Forms.FolderBrowserDialog
        {
            Description = "Choose the Minecraft game directory",
            UseDescriptionForTitle = true,
            SelectedPath = Directory.Exists(GameDirBox.Text) ? GameDirBox.Text : string.Empty,
            ShowNewFolderButton = true
        };

        if (dialog.ShowDialog() != System.Windows.Forms.DialogResult.OK)
            return;

        GameDirBox.Text = dialog.SelectedPath;
        SaveCurrentProfile();
        SaveSettings();
        StatusText.Text = "Game directory updated.";
    }

    private void OpenGameDirectoryButton_Click(object sender, RoutedEventArgs e)
    {
        var directory = GameDirBox.Text.Trim();
        if (string.IsNullOrWhiteSpace(directory))
        {
            System.Windows.MessageBox.Show("Choose a Minecraft game directory first.", "Axiom Launcher", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        Directory.CreateDirectory(directory);
        Process.Start(new ProcessStartInfo("explorer.exe", directory) { UseShellExecute = true });
    }

    private void SettingsButton_Click(object sender, RoutedEventArgs e)
    {
        SaveCurrentProfile();
        SaveSettings();
        StatusText.Text = "Settings saved.";
        System.Windows.MessageBox.Show(
            $"Profile: {_profiles[_selectedProfileIndex].Name}\nRAM: {GetSelectedRamMb() / 1024} GB\nGame directory: {GameDirBox.Text}",
            "Axiom Settings", MessageBoxButton.OK, MessageBoxImage.Information);
    }
}