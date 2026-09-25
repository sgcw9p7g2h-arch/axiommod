package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class FeatureHud {
    private static boolean lastLeft;
    private static boolean lastRight;
    private static int leftCps;
    private static int rightCps;
    private static int clickWindowTicks;

    private FeatureHud() {}

    public static void tick(Minecraft client) {
        if (client.getWindow() == null) return;
        long handle = client.getWindow().handle();
        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (left && !lastLeft) leftCps++;
        if (right && !lastRight) rightCps++;
        lastLeft = left;
        lastRight = right;
        if (++clickWindowTicks >= 20) {
            leftCps = 0;
            rightCps = 0;
            clickWindowTicks = 0;
        }
    }

    public static void render(GuiGraphics g) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        int y = 8;
        if (AxiomClient.FEATURES.isEnabled("fps")) {
            g.drawString(client.font, "FPS: " + client.getFps(), 8, y, 0xFFFFFF);
            y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("coordinates")) {
            var p = client.player.blockPosition();
            g.drawString(client.font, "XYZ: " + p.getX() + " " + p.getY() + " " + p.getZ(), 8, y, 0xFFFFFF);
            y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("cps")) {
            g.drawString(client.font, "CPS: " + leftCps + " | " + rightCps, 8, y, 0xFFFFFF);
        }

        if (AxiomClient.FEATURES.isEnabled("keystrokes")) {
            renderKeystrokes(g, client);
        }
        if (AxiomClient.FEATURES.isEnabled("armor_hud")) {
            renderArmor(g, client);
        }
    }

    private static void renderKeystrokes(GuiGraphics g, Minecraft client) {
        int x = 8;
        int y = client.getWindow().getGuiScaledHeight() - 78;
        drawKey(g, client, "W", client.options.keyUp, x + 22, y);
        drawKey(g, client, "A", client.options.keyLeft, x, y + 22);
        drawKey(g, client, "S", client.options.keyDown, x + 22, y + 22);
        drawKey(g, client, "D", client.options.keyRight, x + 44, y + 22);
        drawKey(g, client, "SP", client.options.keyJump, x, y + 44, 66);
    }

    private static void drawKey(GuiGraphics g, Minecraft client, String text, net.minecraft.client.KeyMapping key, int x, int y) {
        drawKey(g, client, text, key, x, y, 20);
    }

    private static void drawKey(GuiGraphics g, Minecraft client, String text, net.minecraft.client.KeyMapping key, int x, int y, int width) {
        int color = key.isDown() ? 0xA0FFFFFF : 0x60303030;
        g.fill(x, y, x + width, y + 20, color);
        g.drawCenteredString(client.font, text, x + width / 2, y + 6, 0xFFFFFF);
    }

    private static void renderArmor(GuiGraphics g, Minecraft client) {
        Inventory inv = client.player.getInventory();
        int x = client.getWindow().getGuiScaledWidth() - 88;
        int y = client.getWindow().getGuiScaledHeight() - 24;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = inv.armor.get(i);
            g.renderItem(stack, x + i * 20, y);
        }
    }
}
