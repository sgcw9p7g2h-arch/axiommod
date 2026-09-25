using System.Security.Cryptography;
using CmlLib.Core;

namespace AxiomLauncher;

internal sealed class AxiomRuntimeManager
{
    private readonly string _clientStateFileName = Path.Combine(".axiom", "client-state.json");

    public async Task<bool> IsReadyAsync(MinecraftPath path, string clientVersion, string minecraftVersion,
        string fabricLoaderVersion, string fabricApiVersion, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            return false;

        var statePath = Path.Combine(path.BasePath, _clientStateFileName);
        var state = await ClientRuntimeState.LoadAsync(statePath);
        if (state == null)
            return false;

        var assetPath = GetClientAssetPath(path, clientAsset);
        if (!File.Exists(assetPath))
            return false;

        var fabricApiPath = Path.Combine(path.BasePath, "mods", $"fabric-api-{fabricApiVersion}.jar");
        if (!File.Exists(fabricApiPath))
            return false;

        var digest = await ComputeSha256Async(assetPath);
        return state.Matches(clientVersion, minecraftVersion, fabricLoaderVersion, fabricApiVersion, clientAsset, digest);
    }

    public async Task SaveStateAsync(MinecraftPath path, string clientVersion, string minecraftVersion,
        string fabricLoaderVersion, string fabricApiVersion, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        var assetPath = GetClientAssetPath(path, clientAsset);
        if (!File.Exists(assetPath))
            throw new InvalidOperationException("The Axiom client asset is missing after installation.");

        var digest = await ComputeSha256Async(assetPath);
        var state = ClientRuntimeState.FromManifest(
            clientVersion, minecraftVersion, fabricLoaderVersion, fabricApiVersion, clientAsset, digest);

        await state.SaveAsync(Path.Combine(path.BasePath, _clientStateFileName));
    }

    public static string GetClientAssetPath(MinecraftPath path, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        return Path.Combine(path.BasePath, "mods", clientAsset);
    }

    private static bool IsSafeAssetName(string value) =>
        !string.IsNullOrWhiteSpace(value) &&
        value.Length <= 128 &&
        value.IndexOfAny(new[] { '/', '\\' }) < 0 &&
        value != "." &&
        value != "..";

    private static async Task<string> ComputeSha256Async(string path)
    {
        await using var stream = File.OpenRead(path);
        return Convert.ToHexString(await SHA256.HashDataAsync(stream)).ToLowerInvariant();
    }
}
