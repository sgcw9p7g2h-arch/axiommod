package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class VisualFeatures {
    private VisualFeatures() {}

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