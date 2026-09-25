package axiom.client;

import axiom.client.features.FeatureHud;
import axiom.client.features.VisualFeatures;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class HudHooks {
    private HudHooks() {}
    public static void register() {
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            AxiomClient.HUD.render(graphics);
            FeatureHud.render(graphics);
            VisualFeatures.renderCrosshair(graphics, net.minecraft.client.Minecraft.getInstance());
        });
    }
}