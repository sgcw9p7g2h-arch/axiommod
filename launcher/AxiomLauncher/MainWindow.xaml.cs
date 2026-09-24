using System;
using System.IO;
using System.Text.Json;
using System.Windows;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.Auth.Microsoft;
using CmlLib.Core.ProcessBuilder;

namespace AxiomLauncher;

public partial class MainWindow : Window
{
    private MinecraftLauncher? _launcher;
    private MSession? _session;
    private readonly JELoginHandler _loginHandler = JELoginHandlerBuilder.BuildDefault();
    private readonly string _settingsFile;

    public MainWindow()
    {
        InitializeComponent();

        var gameDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".axiom");

        GameDirBox.Text = gameDir;
        _settingsFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Axiom", "launcher-settings.json");
        LoadSettings();
    }

    private async void LoginButton_Click(object sender, RoutedEventArgs e)
    {
        LoginButton.IsEnabled = false;
        LaunchButton.IsEnabled = false;
        StatusText.Text = "Opening Microsoft sign-in...";

        try
        {
            _session = await _loginHandler.Authenticate();
            AccountText.Text = $"Signed in as {_session.Username}";
            StatusText.Text = "Microsoft account connected.";
            LoginButton.Content = "SIGNED IN";
        }
        catch (Exception ex)
        {
            StatusText.Text = "Microsoft sign-in failed.";
            MessageBox.Show(ex.Message, "Axiom Launcher",
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
        StatusText.Text = "Preparing Axiom...";

        try
        {
            SaveSettings();

            var path = new MinecraftPath(GameDirBox.Text);
            _launcher = new MinecraftLauncher(path);

            var version = VersionBox.Text;
            StatusText.Text = $"Installing/checking Minecraft {version}...";
            await _launcher.InstallAsync(version);

            StatusText.Text = "Starting Axiom...";
            var options = new MLaunchOption
            {
                Session = _session,
                MaximumRamMb = GetSelectedRamMb()
            };

            var process = await _launcher.BuildProcessAsync(version, options);
            process.Start();

            StatusText.Text = "Axiom started.";
            Close();
        }
        catch (Exception ex)
        {
            StatusText.Text = "Launch failed.";
            MessageBox.Show(ex.Message, "Axiom Launcher",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            LaunchButton.IsEnabled = _session != null;
        }
    }

    private int GetSelectedRamMb()
    {
        return RamBox.SelectedIndex switch
        {
            0 => 2048,
            1 => 4096,
            2 => 6144,
            3 => 8192,
            _ => 4096
        };
    }

    private void LoadSettings()
    {
        try
        {
            if (!File.Exists(_settingsFile))
                return;

            var settings = JsonSerializer.Deserialize<LauncherSettings>(File.ReadAllText(_settingsFile));
            if (settings == null)
                return;

            if (!string.IsNullOrWhiteSpace(settings.GameDirectory))
                GameDirBox.Text = settings.GameDirectory;

            RamBox.SelectedIndex = settings.RamMb switch
            {
                2048 => 0,
                4096 => 1,
                6144 => 2,
                8192 => 3,
                _ => 1
            };
        }
        catch
        {
            // A corrupt settings file should never prevent the launcher from opening.
        }
    }

    private void SaveSettings()
    {
        try
        {
            var directory = Path.GetDirectoryName(_settingsFile);
            if (!string.IsNullOrEmpty(directory))
                Directory.CreateDirectory(directory);

            var settings = new LauncherSettings
            {
                GameDirectory = GameDirBox.Text,
                RamMb = GetSelectedRamMb()
            };

            File.WriteAllText(_settingsFile, JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch
        {
            // Settings persistence is non-critical to launching.
        }
    }

    private sealed class LauncherSettings
    {
        public string GameDirectory { get; set; } = string.Empty;
        public int RamMb { get; set; } = 4096;
    }

    private void SettingsButton_Click(object sender, RoutedEventArgs e)
    {
        SaveSettings();
        MessageBox.Show(
            "Your Axiom game directory and RAM setting are saved automatically.",
            "Axiom Launcher");
    }
}