using System.Text.Json;

namespace AxiomLauncher;

internal sealed class AxiomRuntimeManifest
{
    public int SchemaVersion { get; set; } = 1;
    public string ClientVersion { get; set; } = string.Empty;
    public string MinecraftVersion { get; set; } = string.Empty;
    public string FabricLoaderVersion { get; set; } = string.Empty;
    public string FabricApiVersion { get; set; } = string.Empty;
    public string ClientAsset { get; set; } = string.Empty;
    public string ClientAssetSha256 { get; set; } = string.Empty;
    public DateTime InstalledAtUtc { get; set; }

    public static AxiomRuntimeManifest FromRuntimeState(ClientRuntimeState state) => new()
    {
        ClientVersion = state.ClientVersion,
        MinecraftVersion = state.MinecraftVersion,
        FabricLoaderVersion = state.FabricLoaderVersion,
        FabricApiVersion = state.FabricApiVersion,
        ClientAsset = state.ClientAsset,
        ClientAssetSha256 = state.ClientAssetSha256,
        InstalledAtUtc = state.InstalledAtUtc
    };

    public async Task SaveAsync(string path)
    {
        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory))
            Directory.CreateDirectory(directory);

        var temporary = path + ".tmp";
        await using (var stream = File.Create(temporary))
        {
            await JsonSerializer.SerializeAsync(stream, this, new JsonSerializerOptions { WriteIndented = true });
            await stream.FlushAsync();
        }

        File.Move(temporary, path, true);
    }
}
