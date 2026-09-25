package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class VisualFeatures {
    private static boolean zoomed;
    private static double zoomFov = 30.0;
    private static int brightnessTicks = 0;
    private VisualFeatures() {}
    public static void toggleZoom() { zoomed = !zoomed; }
    public static double fov(double vanillaFov) { return zoomed && AxiomClient.FEATURES.isEnabled("zoom") ? zoomFov : vanillaFov; }
    public static void tick(Minecraft client) {
        if (AxiomClient.FEATURES.isEnabled("fullbright")) brightnessTicks = 20;
        else if (brightnessTicks > 0) brightnessTicks--;
    }
    public static float gamma(float vanilla) {
        return AxiomClient.FEATURES.isEnabled("fullbright") ? 16.0f : vanilla;
    }

    public static void renderCrosshair(GuiGraphics g, Minecraft client) {
        if (client.player == null || client.options.hideGui || !AxiomClient.FEATURES.isEnabled("crosshair")) return;
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;
        g.fill(cx - 1, cy - 6, cx + 2, cy - 1, 0xFFFFFFFF);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 6, 0xFFFFFFFF);
        g.fill(cx - 6, cy - 1, cx - 1, cy + 2, 0xFFFFFFFF);
        g.fill(cx + 1, cy - 1, cx + 6, cy + 2, 0xFFFFFFFF);
    }
}