using System.Text.Json;

namespace AxiomLauncher;

internal sealed class AxiomRuntimeManifest
{
    public int SchemaVersion { get; set; } = 2;
    public string ClientVersion { get; set; } = string.Empty;
    public string MinecraftVersion { get; set; } = string.Empty;
    public string FabricLoaderVersion { get; set; } = string.Empty;
    public string FabricApiVersion { get; set; } = string.Empty;
    public string ClientAsset { get; set; } = string.Empty;
    public string ClientAssetSha256 { get; set; } = string.Empty;
    public string FabricApiAsset { get; set; } = string.Empty;
    public string FabricApiAssetSha256 { get; set; } = string.Empty;
    public DateTime InstalledAtUtc { get; set; }

    public static AxiomRuntimeManifest FromRuntimeState(
        ClientRuntimeState state,
        string fabricApiAsset,
        string fabricApiAssetSha256) => new()
    {
        ClientVersion = state.ClientVersion,
        MinecraftVersion = state.MinecraftVersion,
        FabricLoaderVersion = state.FabricLoaderVersion,
        FabricApiVersion = state.FabricApiVersion,
        ClientAsset = state.ClientAsset,
        ClientAssetSha256 = state.ClientAssetSha256,
        FabricApiAsset = fabricApiAsset,
        FabricApiAssetSha256 = fabricApiAssetSha256,
        InstalledAtUtc = state.InstalledAtUtc
    };

    public bool Matches(
        string clientVersion,
        string minecraftVersion,
        string fabricLoaderVersion,
        string fabricApiVersion,
        string clientAsset,
        string clientAssetSha256,
        string fabricApiAsset,
        string fabricApiAssetSha256) =>
        SchemaVersion == 2 &&
        string.Equals(ClientVersion, clientVersion, StringComparison.Ordinal) &&
        string.Equals(MinecraftVersion, minecraftVersion, StringComparison.Ordinal) &&
        string.Equals(FabricLoaderVersion, fabricLoaderVersion, StringComparison.Ordinal) &&
        string.Equals(FabricApiVersion, fabricApiVersion, StringComparison.Ordinal) &&
        string.Equals(ClientAsset, clientAsset, StringComparison.Ordinal) &&
        string.Equals(ClientAssetSha256, clientAssetSha256, StringComparison.OrdinalIgnoreCase) &&
        string.Equals(FabricApiAsset, fabricApiAsset, StringComparison.Ordinal) &&
        string.Equals(FabricApiAssetSha256, fabricApiAssetSha256, StringComparison.OrdinalIgnoreCase) &&
        InstalledAtUtc != default;

    public static async Task<AxiomRuntimeManifest?> LoadAsync(string path)
    {
        if (!File.Exists(path))
            return null;

        try
        {
            await using var stream = File.OpenRead(path);
            return await JsonSerializer.DeserializeAsync<AxiomRuntimeManifest>(stream);
        }
        catch
        {
            return null;
        }
    }

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
