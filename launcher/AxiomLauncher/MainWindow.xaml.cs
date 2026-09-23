using System;
using System.IO;
using System.Windows;
using CmlLib.Core;

namespace AxiomLauncher;

public partial class MainWindow : Window
{
    private readonly string _gameDir;

    public MainWindow()
    {
        InitializeComponent();

        _gameDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".axiom");

        GameDirBox.Text = _gameDir;
    }

    private async void LaunchButton_Click(object sender, RoutedEventArgs e)
    {
        LaunchButton.IsEnabled = false;
        StatusText.Text = "Preparing Minecraft...";

        try
        {
            var path = new MinecraftPath(GameDirBox.Text);
            var launcher = new MinecraftLauncher(path);

            var version = VersionBox.Text;
            StatusText.Text = $"Installing/checking Minecraft {version}...";

            await launcher.InstallAsync(version);

            StatusText.Text = "Starting Minecraft...";

            var options = new MLaunchOption
            {
                Session = MSession.CreateOfflineSession("AxiomPlayer"),
                MaximumRamMb = 4096
            };

            var process = await launcher.BuildProcessAsync(version, options);
            process.Start();

            StatusText.Text = "Minecraft started.";
            Close();
        }
        catch (Exception ex)
        {
            StatusText.Text = "Launch failed.";
            MessageBox.Show(
                ex.Message,
                "Axiom Launcher",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
        }
        finally
        {
            LaunchButton.IsEnabled = true;
        }
    }

    private void SettingsButton_Click(object sender, RoutedEventArgs e)
    {
        MessageBox.Show(
            "Settings will control RAM, Java, profiles, updates, and the game directory.",
            "Axiom Launcher");
    }
}
