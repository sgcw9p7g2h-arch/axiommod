using System.Text.Json;

namespace AxiomLauncher;

internal sealed class ClientRuntimeState
{
    public string ClientVersion { get; set; } = string.Empty;
    public string MinecraftVersion { get; set; } = string.Empty;
    public string FabricLoaderVersion { get; set; } = string.Empty;
    public string FabricApiVersion { get; set; } = string.Empty;
    public string ClientAsset { get; set; } = string.Empty;
    public string ClientAssetSha256 { get; set; } = string.Empty;
    public DateTime InstalledAtUtc { get; set; }

    public static ClientRuntimeState FromManifest(
        string clientVersion,
        string minecraftVersion,
        string fabricLoaderVersion,
        string fabricApiVersion,
        string clientAsset,
        string clientAssetSha256) =>
        new()
        {
            ClientVersion = clientVersion,
            MinecraftVersion = minecraftVersion,
            FabricLoaderVersion = fabricLoaderVersion,
            FabricApiVersion = fabricApiVersion,
            ClientAsset = clientAsset,
            ClientAssetSha256 = clientAssetSha256,
            InstalledAtUtc = DateTime.UtcNow
        };

    public bool Matches(
        string clientVersion,
        string minecraftVersion,
        string fabricLoaderVersion,
        string fabricApiVersion,
        string clientAsset,
        string clientAssetSha256) =>
        string.Equals(ClientVersion, clientVersion, StringComparison.Ordinal) &&
        string.Equals(MinecraftVersion, minecraftVersion, StringComparison.Ordinal) &&
        string.Equals(FabricLoaderVersion, fabricLoaderVersion, StringComparison.Ordinal) &&
        string.Equals(FabricApiVersion, fabricApiVersion, StringComparison.Ordinal) &&
        string.Equals(ClientAsset, clientAsset, StringComparison.Ordinal) &&
        string.Equals(ClientAssetSha256, clientAssetSha256, StringComparison.OrdinalIgnoreCase);

    public static async Task<ClientRuntimeState?> LoadAsync(string path)
    {
        if (!File.Exists(path))
            return null;

        try
        {
            await using var stream = File.OpenRead(path);
            return await JsonSerializer.DeserializeAsync<ClientRuntimeState>(stream);
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
