package axiom.client;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
public final class HudHooks { private HudHooks(){} public static void register(){ HudRenderCallback.EVENT.register((graphics,delta)->AxiomClient.HUD.render(graphics)); } }
