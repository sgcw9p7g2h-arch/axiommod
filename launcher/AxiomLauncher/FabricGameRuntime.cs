using System.Diagnostics;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.ProcessBuilder;

namespace AxiomLauncher;

internal sealed class FabricGameRuntime : IAxiomGameRuntime
{
    public async Task<Process> LaunchAsync(
        MinecraftLauncher launcher,
        string runtimeVersion,
        MSession session,
        int maximumRamMb)
    {
        var options = new MLaunchOption
        {
            Session = session,
            MaximumRamMb = maximumRamMb
        };

        return await launcher.BuildProcessAsync(runtimeVersion, options);
    }
}
