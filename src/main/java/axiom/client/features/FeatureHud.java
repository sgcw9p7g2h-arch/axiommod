package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FeatureHud {
    private static boolean lastLeft, lastRight;
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static final Deque<Integer> fpsHistory = new ArrayDeque<>();
    private static int fpsSampleTicks;
    private static long playtimeTicks;

    private FeatureHud() {}

    public static void tick(Minecraft client) {
        if (client.getWindow() == null) return;
        if (client.player != null) playtimeTicks++;

        long handle = client.getWindow().handle();
        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.nanoTime();

        if (left && !lastLeft) leftClicks.addLast(now);
        if (right && !lastRight) rightClicks.addLast(now);
        lastLeft = left;
        lastRight = right;
        pruneClicks(leftClicks, now);
        pruneClicks(rightClicks, now);

        if (++fpsSampleTicks >= 5) {
            fpsHistory.addLast(client.getFps());
            while (fpsHistory.size() > 60) fpsHistory.removeFirst();
            fpsSampleTicks = 0;
        }
    }

    private static void pruneClicks(Deque<Long> clicks, long now) {
        long cutoff = now - 1_000_000_000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.removeFirst();
    }

    public static void render(GuiGraphics g) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        int y = 8;

        if (AxiomClient.FEATURES.isEnabled("client_name")) {
            g.drawString(client.font, "Axiom", 8, y, 0xFFFFFF);
            y += 12;
        }
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
            g.drawString(client.font, "CPS: " + leftClicks.size() + " | " + rightClicks.size(), 8, y, 0xFFFFFF);
            y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("direction")) {
            g.drawString(client.font, "Facing: " + direction(client.player.getYRot()), 8, y, 0xFFFFFF);
            y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("playtime")) {
            g.drawString(client.font, "Playtime: " + formatPlaytime(), 8, y, 0xFFFFFF);
            y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("server_address")) {
            String server = client.getCurrentServer() == null ? "Singleplayer" : client.getCurrentServer().ip;
            g.drawString(client.font, "Server: " + server, 8, y, 0xFFFFFF);
            y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("held_item")) {
            ItemStack held = client.player.getMainHandItem();
            String name = held.isEmpty() ? "Empty" : held.getHoverName().getString();
            g.drawString(client.font, "Held: " + name, 8, y, 0xFFFFFF);
        }

        if (AxiomClient.FEATURES.isEnabled("fps_graph")) renderFpsGraph(g, client);
        if (AxiomClient.FEATURES.isEnabled("keystrokes")) renderKeystrokes(g, client);
        if (AxiomClient.FEATURES.isEnabled("armor_hud")) renderArmor(g, client);
        if (AxiomClient.FEATURES.isEnabled("potion_effects")) renderPotionEffects(g, client);
    }

    private static String formatPlaytime() {
        long seconds = playtimeTicks / 20L;
        return String.format("%02d:%02d:%02d", seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L);
    }

    private static void renderFpsGraph(GuiGraphics g, Minecraft c) {
        if (fpsHistory.isEmpty()) return;
        int x = c.getWindow().getGuiScaledWidth() - 130, baseY = 78;
        int max = Math.max(1, fpsHistory.stream().max(Integer::compareTo).orElse(1));
        int i = 0;
        for (int fps : fpsHistory) {
            int height = Math.max(1, Math.min(50, fps * 50 / max));
            g.fill(x + i * 2, baseY - height, x + i * 2 + 1, baseY, 0xB0FFFFFF);
            i++;
        }
        g.drawString(c.font, "FPS graph", x, baseY + 4, 0xFFFFFF);
    }

    private static String direction(float yaw) {
        float normalized = ((yaw % 360.0f) + 360.0f) % 360.0f;
        if (normalized >= 315 || normalized < 45) return "South";
        if (normalized < 135) return "West";
        if (normalized < 225) return "North";
        return "East";
    }

    private static void renderKeystrokes(GuiGraphics g, Minecraft c) {
        int x = 8, y = c.getWindow().getGuiScaledHeight() - 78;
        drawKey(g, c, "W", c.options.keyUp, x + 22, y, 20);
        drawKey(g, c, "A", c.options.keyLeft, x, y + 22, 20);
        drawKey(g, c, "S", c.options.keyDown, x + 22, y + 22, 20);
        drawKey(g, c, "D", c.options.keyRight, x + 44, y + 22, 20);
        drawKey(g, c, "SP", c.options.keyJump, x, y + 44, 66);
    }

    private static void drawKey(GuiGraphics g, Minecraft c, String text, net.minecraft.client.KeyMapping key, int x, int y, int w) {
        g.fill(x, y, x + w, y + 20, key.isDown() ? 0xA0FFFFFF : 0x60303030);
        g.drawCenteredString(c.font, text, x + w / 2, y + 6, 0xFFFFFF);
    }

    private static void renderArmor(GuiGraphics g, Minecraft c) {
        Inventory inv = c.player.getInventory();
        int x = c.getWindow().getGuiScaledWidth() - 88;
        int y = c.getWindow().getGuiScaledHeight() - 24;
        for (int i = 0; i < 4; i++) g.renderItem(inv.getItem(36 + i), x + i * 20, y);
    }

    private static void renderPotionEffects(GuiGraphics g, Minecraft c) {
        int x = c.getWindow().getGuiScaledWidth() - 150;
        int y = 8;
        for (var effect : c.player.getActiveEffects()) {
            String name = effect.getEffect().value().getDisplayName().getString();
            int seconds = effect.getDuration() / 20;
            g.drawString(c.font, name + " " + formatDuration(seconds), x, y, 0xFFFFFF);
            y += 12;
        }
    }

    private static String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
