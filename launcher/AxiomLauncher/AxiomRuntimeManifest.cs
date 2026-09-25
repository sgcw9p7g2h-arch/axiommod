using System.IO;
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
        IsValid() &&
        string.Equals(ClientVersion, clientVersion, StringComparison.Ordinal) &&
        string.Equals(MinecraftVersion, minecraftVersion, StringComparison.Ordinal) &&
        string.Equals(FabricLoaderVersion, fabricLoaderVersion, StringComparison.Ordinal) &&
        string.Equals(FabricApiVersion, fabricApiVersion, StringComparison.Ordinal) &&
        string.Equals(ClientAsset, clientAsset, StringComparison.Ordinal) &&
        string.Equals(ClientAssetSha256, clientAssetSha256, StringComparison.OrdinalIgnoreCase) &&
        string.Equals(FabricApiAsset, fabricApiAsset, StringComparison.Ordinal) &&
        string.Equals(FabricApiAssetSha256, fabricApiAssetSha256, StringComparison.OrdinalIgnoreCase);

    private bool IsValid() =>
        SchemaVersion == 2 &&
        IsSafeAssetName(ClientAsset) &&
        ClientAsset.EndsWith(".jar", StringComparison.OrdinalIgnoreCase) &&
        IsSafeAssetName(FabricApiAsset) &&
        FabricApiAsset.EndsWith(".jar", StringComparison.OrdinalIgnoreCase) &&
        IsSha256(ClientAssetSha256) &&
        IsSha256(FabricApiAssetSha256) &&
        InstalledAtUtc != default;

    private static bool IsSafeAssetName(string value) =>
        !string.IsNullOrWhiteSpace(value) &&
        value.Length <= 128 &&
        value.IndexOfAny(new[] { '/', '\\' }) < 0 &&
        value != "." &&
        value != "..";

    private static bool IsSha256(string value) =>
        value.Length == 64 && value.All(c =>
            (c >= '0' && c <= '9') ||
            (c >= 'a' && c <= 'f') ||
            (c >= 'A' && c <= 'F'));

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
