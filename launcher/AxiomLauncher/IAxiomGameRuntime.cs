using System.Diagnostics;

namespace AxiomLauncher;

internal sealed record AxiomGameLaunchRequest(
    string GameDirectory,
    string RuntimeVersion,
    string Username,
    string AccessToken,
    int MaximumRamMb);

internal interface IAxiomGameRuntime
{
    Task<Process> LaunchAsync(
        AxiomGameLaunchRequest request,
        CancellationToken cancellationToken = default);
}
