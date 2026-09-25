namespace AxiomLauncher;

internal sealed class ClientManifest
{
    public string ClientVersion { get; set; } = "0.1.0";
    public string MinecraftVersion { get; set; } = string.Empty;
    public string FabricLoaderVersion { get; set; } = string.Empty;
    public string FabricApiVersion { get; set; } = string.Empty;
    public string ClientAsset { get; set; } = string.Empty;
    public string Runtime { get; set; } = "fabric";
    public string LauncherAsset { get; set; } = string.Empty;
}

internal sealed class LauncherSettings
{
    public LauncherProfile[] Profiles { get; set; } = Array.Empty<LauncherProfile>();
    public int SelectedProfile { get; set; }

    // Legacy fields are retained so older settings files can still be migrated.
    public string? GameDirectory { get; set; }
    public int RamMb { get; set; }
}

internal sealed class LauncherProfile
{
    public string Name { get; set; } = "Profile";
    public string GameDirectory { get; set; } = string.Empty;
    public int RamMb { get; set; } = 4096;
}
