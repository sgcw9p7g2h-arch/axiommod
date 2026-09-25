package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;

public final class MovementFeatures {
    private static boolean sneakLatched;
    private static boolean lastSneakToggle;

    private MovementFeatures() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.screen != null) return;

        if (AxiomClient.FEATURES.isEnabled("toggle_sprint")) {
            if (client.options.keyUp.isDown() && !client.player.isUsingItem() && !client.player.isPassenger()) {
                client.player.setSprinting(true);
            }
        }

        if (AxiomClient.FEATURES.isEnabled("toggle_sneak")) {
            boolean pressed = client.options.keyShift.isDown();
            if (pressed && !lastSneakToggle) sneakLatched = !sneakLatched;
            lastSneakToggle = pressed;
            client.options.keyShift.setDown(sneakLatched);
        } else {
            sneakLatched = false;
            lastSneakToggle = false;
        }
    }
}