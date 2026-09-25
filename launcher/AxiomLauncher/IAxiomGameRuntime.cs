using System.Diagnostics;
using CmlLib.Core;
using CmlLib.Core.Auth;

namespace AxiomLauncher;

internal interface IAxiomGameRuntime
{
    Task<Process> LaunchAsync(
        MinecraftLauncher launcher,
        string runtimeVersion,
        MSession session,
        int maximumRamMb);
}
