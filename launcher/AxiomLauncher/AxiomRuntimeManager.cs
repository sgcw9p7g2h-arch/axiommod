using System.Security.Cryptography;
using CmlLib.Core;

namespace AxiomLauncher;

internal sealed class AxiomRuntimeManager
{
    private const string StateDirectoryName = ".axiom";
    private const string RuntimeDirectoryName = "runtime";
    private const string StateFileName = "client-state.json";
    private const string ModsDirectoryName = "mods";

    private static string GetStatePath(MinecraftPath path) =>
        Path.Combine(path.BasePath, StateDirectoryName, StateFileName);

    private static string GetModsDirectory(MinecraftPath path) =>
        Path.Combine(path.BasePath, ModsDirectoryName);

    public async Task<bool> IsReadyAsync(MinecraftPath path, string clientVersion, string minecraftVersion,
        string fabricLoaderVersion, string fabricApiVersion, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            return false;

        var runtimeManifest = await AxiomRuntimeManifest.LoadAsync(GetRuntimeManifestPath(path));
        if (runtimeManifest == null)
            return false;

        var runtimeAssetPath = GetRuntimeAssetPath(path, clientAsset);
        if (!File.Exists(runtimeAssetPath))
            return false;

        var fabricApiAsset = $"fabric-api-{fabricApiVersion}.jar";
        var fabricApiPath = GetRuntimeAssetPath(path, fabricApiAsset);
        if (!File.Exists(fabricApiPath))
            return false;

        var digest = await ComputeSha256Async(runtimeAssetPath);
        var fabricApiDigest = await ComputeSha256Async(fabricApiPath);
        return runtimeManifest.Matches(
            clientVersion,
            minecraftVersion,
            fabricLoaderVersion,
            fabricApiVersion,
            clientAsset,
            digest,
            fabricApiAsset,
            fabricApiDigest);
    }

    public async Task SaveStateAsync(MinecraftPath path, string clientVersion, string minecraftVersion,
        string fabricLoaderVersion, string fabricApiVersion, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        var assetPath = GetRuntimeAssetPath(path, clientAsset);
        if (!File.Exists(assetPath))
            throw new InvalidOperationException("The Axiom runtime asset is missing after installation.");

        var digest = await ComputeSha256Async(assetPath);

        var fabricApiAsset = $"fabric-api-{fabricApiVersion}.jar";
        var fabricApiPath = GetRuntimeAssetPath(path, fabricApiAsset);
        if (!File.Exists(fabricApiPath))
            throw new InvalidOperationException("The Fabric API runtime asset is missing after installation.");

        var fabricApiDigest = await ComputeSha256Async(fabricApiPath);
        var state = ClientRuntimeState.FromManifest(
            clientVersion, minecraftVersion, fabricLoaderVersion, fabricApiVersion, clientAsset, digest);

        await state.SaveAsync(GetStatePath(path));

        var runtimeManifest = AxiomRuntimeManifest.FromRuntimeState(state, fabricApiAsset, fabricApiDigest);
        await runtimeManifest.SaveAsync(GetRuntimeManifestPath(path));
    }

    public async Task<FileStream> AcquireLockAsync(MinecraftPath path, CancellationToken cancellationToken = default)
    {
        var lockPath = GetRuntimeLockPath(path);
        var directory = Path.GetDirectoryName(lockPath);
        if (!string.IsNullOrEmpty(directory))
            Directory.CreateDirectory(directory);

        var deadline = DateTime.UtcNow + TimeSpan.FromSeconds(30);
        while (true)
        {
            cancellationToken.ThrowIfCancellationRequested();

            try
            {
                return new FileStream(lockPath, FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
            }
            catch (IOException) when (DateTime.UtcNow < deadline)
            {
                await Task.Delay(250, cancellationToken);
            }
        }
    }

    public static string GetClientAssetPath(MinecraftPath path, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        return Path.Combine(GetModsDirectory(path), clientAsset);
    }

    public static string GetRuntimeAssetPath(MinecraftPath path, string clientAsset)
    {
        if (!IsSafeAssetName(clientAsset))
            throw new InvalidOperationException("The Axiom client asset name is invalid.");

        return Path.Combine(GetRuntimeDirectory(path), clientAsset);
    }

    public static void StageRuntimeAsset(MinecraftPath path, string runtimeAssetName)
    {
        var runtimeAsset = GetRuntimeAssetPath(path, runtimeAssetName);
        if (!File.Exists(runtimeAsset))
            throw new FileNotFoundException("The Axiom runtime asset is missing.", runtimeAsset);

        var modsDirectory = GetModsDirectory(path);
        Directory.CreateDirectory(modsDirectory);
        var destination = GetClientAssetPath(path, runtimeAssetName);
        var temporary = destination + ".stage";
        File.Copy(runtimeAsset, temporary, true);
        File.Move(temporary, destination, true);
    }

    public static string GetRuntimeDirectory(MinecraftPath path) =>
        Path.Combine(path.BasePath, StateDirectoryName, RuntimeDirectoryName);

    public static string GetRuntimeStatePath(MinecraftPath path) =>
        Path.Combine(path.BasePath, StateDirectoryName, StateFileName);

    public static string GetRuntimeManifestPath(MinecraftPath path) =>
        Path.Combine(GetRuntimeDirectory(path), "runtime.json");

    public static string GetRuntimeLockPath(MinecraftPath path) =>
        Path.Combine(GetRuntimeDirectory(path), "runtime.lock");

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
