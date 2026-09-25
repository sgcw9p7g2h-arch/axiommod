using System.Diagnostics;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.ProcessBuilder;

namespace AxiomLauncher;

internal sealed class FabricGameRuntime : IAxiomGameRuntime
{
    public async Task<Process> LaunchAsync(
        AxiomGameLaunchRequest request,
        CancellationToken cancellationToken = default)
    {
        ValidateRequest(request);
        cancellationToken.ThrowIfCancellationRequested();

        var path = new MinecraftPath(request.GameDirectory);
        var launcher = new MinecraftLauncher(path);
        var session = new MSession(request.Username, request.AccessToken, request.Uuid);

        var options = new MLaunchOption
        {
            Session = session,
            MaximumRamMb = request.MaximumRamMb
        };

        cancellationToken.ThrowIfCancellationRequested();
        return await launcher.BuildProcessAsync(request.RuntimeVersion, options);
    }

    private static void ValidateRequest(AxiomGameLaunchRequest request)
    {
        ArgumentNullException.ThrowIfNull(request);

        if (string.IsNullOrWhiteSpace(request.GameDirectory))
            throw new InvalidOperationException("The Axiom game directory is required.");

        if (string.IsNullOrWhiteSpace(request.RuntimeVersion))
            throw new InvalidOperationException("The Axiom runtime version is required.");

        if (string.IsNullOrWhiteSpace(request.Username))
            throw new InvalidOperationException("The Microsoft account username is required.");

        if (string.IsNullOrWhiteSpace(request.AccessToken))
            throw new InvalidOperationException("The Microsoft access token is required.");

        if (string.IsNullOrWhiteSpace(request.Uuid))
            throw new InvalidOperationException("The Microsoft account UUID is required.");

        if (request.MaximumRamMb < 1024 || request.MaximumRamMb > 65536)
            throw new InvalidOperationException("Axiom RAM allocation must be between 1 GB and 64 GB.");
    }
}
