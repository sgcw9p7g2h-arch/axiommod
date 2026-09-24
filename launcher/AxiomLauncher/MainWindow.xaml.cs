using System;
using System.IO;
using System.Windows;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.Auth.Microsoft;

namespace AxiomLauncher;

public partial class MainWindow : Window
{
    private MinecraftLauncher? _launcher;
    private MSession? _session;
    private readonly JELoginHandler _loginHandler = JELoginHandlerBuilder.BuildDefault();

    public MainWindow()
    {
        InitializeComponent();

        var gameDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".axiom");

        GameDirBox.Text = gameDir;
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
            var path = new MinecraftPath(GameDirBox.Text);
            _launcher = new MinecraftLauncher(path);

            var version = VersionBox.Text;
            StatusText.Text = $"Installing/checking Minecraft {version}...";
            await _launcher.InstallAsync(version);

            StatusText.Text = "Starting Axiom...";
            var options = new MLaunchOption
            {
                Session = _session,
                MaximumRamMb = 4096
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

    private void SettingsButton_Click(object sender, RoutedEventArgs e)
    {
        MessageBox.Show(
            "Axiom settings will control RAM, Java, profiles, updates, and the game directory.",
            "Axiom Launcher");
    }
}