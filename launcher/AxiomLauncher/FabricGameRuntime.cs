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
        cancellationToken.ThrowIfCancellationRequested();

        var path = new MinecraftPath(request.GameDirectory);
        var launcher = new MinecraftLauncher(path);
        var session = new MSession(request.Username, request.AccessToken);

        var options = new MLaunchOption
        {
            Session = session,
            MaximumRamMb = request.MaximumRamMb
        };

        cancellationToken.ThrowIfCancellationRequested();
        return await launcher.BuildProcessAsync(request.RuntimeVersion, options);
    }
}
