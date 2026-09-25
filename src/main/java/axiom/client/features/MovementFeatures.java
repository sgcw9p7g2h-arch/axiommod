package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;

public final class MovementFeatures {
    private static boolean sneakLatched;
    private static boolean lastSneakToggle;
    private static boolean sprintLatched;
    private static boolean lastForward;

    private MovementFeatures() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.screen != null) return;

        boolean forward = client.options.keyUp.isDown();
        if (AxiomClient.FEATURES.isEnabled("toggle_sprint")) {
            if (forward && !lastForward) sprintLatched = !sprintLatched;
            lastForward = forward;
            if (sprintLatched && forward && !client.player.isUsingItem() && !client.player.isPassenger()) {
                client.player.setSprinting(true);
            } else if (!forward || client.player.isUsingItem() || client.player.isPassenger()) {
                client.player.setSprinting(false);
            }
        } else {
            sprintLatched = false;
            lastForward = forward;
        }

        if (AxiomClient.FEATURES.isEnabled("toggle_sneak")) {
            boolean pressed = client.options.keyShift.isDown();
            if (pressed && !lastSneakToggle) sneakLatched = !sneakLatched;
            lastSneakToggle = pressed;
            if (sneakLatched) client.player.setShiftKeyDown(true);
            else if (!pressed) client.player.setShiftKeyDown(false);
        } else {
            sneakLatched = false;
            lastSneakToggle = false;
        }
    }
}
