using System.IO;
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
        IsValid() &&
        string.Equals(ClientVersion, clientVersion, StringComparison.Ordinal) &&
        string.Equals(MinecraftVersion, minecraftVersion, StringComparison.Ordinal) &&
        string.Equals(FabricLoaderVersion, fabricLoaderVersion, StringComparison.Ordinal) &&
        string.Equals(FabricApiVersion, fabricApiVersion, StringComparison.Ordinal) &&
        string.Equals(ClientAsset, clientAsset, StringComparison.Ordinal) &&
        string.Equals(ClientAssetSha256, clientAssetSha256, StringComparison.OrdinalIgnoreCase);

    private bool IsValid() =>
        HasValue(ClientVersion) &&
        HasValue(MinecraftVersion) &&
        HasValue(FabricLoaderVersion) &&
        HasValue(FabricApiVersion) &&
        IsSafeAssetName(ClientAsset) &&
        ClientAsset.EndsWith(".jar", StringComparison.OrdinalIgnoreCase) &&
        IsSha256(ClientAssetSha256) &&
        InstalledAtUtc != default &&
        InstalledAtUtc.Kind == DateTimeKind.Utc;

    private static bool HasValue(string value) =>
        !string.IsNullOrWhiteSpace(value) && value.Length <= 128;

    private static bool IsSafeAssetName(string value) =>
        !string.IsNullOrWhiteSpace(value) &&
        value.Length <= 128 &&
        value.IndexOfAny(new[] { '/', '\' }) < 0 &&
        value != "." &&
        value != "..";

    private static bool IsSha256(string value) =>
        value.Length == 64 && value.All(c =>
            (c >= '0' && c <= '9') ||
            (c >= 'a' && c <= 'f') ||
            (c >= 'A' && c <= 'F'));

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
        try
        {
            await using (var stream = new FileStream(
                temporary,
                FileMode.Create,
                FileAccess.Write,
                FileShare.None,
                81920,
                FileOptions.SequentialScan))
            {
                await JsonSerializer.SerializeAsync(
                    stream,
                    this,
                    new JsonSerializerOptions { WriteIndented = true });
                await stream.FlushAsync();
            }

            File.Move(temporary, path, true);
        }
        catch
        {
            try
            {
                if (File.Exists(temporary))
                    File.Delete(temporary);
            }
            catch { }

            throw;
        }
    }
}
