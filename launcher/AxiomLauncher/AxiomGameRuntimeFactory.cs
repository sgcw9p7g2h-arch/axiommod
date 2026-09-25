namespace AxiomLauncher;

internal static class AxiomGameRuntimeFactory
{
    public static IAxiomGameRuntime Create(string runtime)
    {
        if (string.Equals(runtime, "fabric", StringComparison.OrdinalIgnoreCase))
            return new FabricGameRuntime();

        throw new InvalidOperationException($"The Axiom runtime '{runtime}' is not supported by this launcher.");
    }
}
