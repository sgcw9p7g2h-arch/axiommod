package axiom.client.features;

import axiom.client.AxiomClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class FeatureHud {
    private static boolean lastLeft, lastRight;
    private static int leftCps, rightCps, clickWindowTicks;
    private static long lastLeftClickNanos, lastRightClickNanos;

    private FeatureHud() {}

    public static void tick(Minecraft client) {
        if (client.getWindow() == null) return;
        long handle = client.getWindow().handle();
        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (left && !lastLeft) { leftCps++; lastLeftClickNanos = System.nanoTime(); }
        if (right && !lastRight) { rightCps++; lastRightClickNanos = System.nanoTime(); }
        lastLeft = left;
        lastRight = right;
        if (++clickWindowTicks >= 20) {
            leftCps = rightCps = 0;
            clickWindowTicks = 0;
        }
    }

    public static void render(GuiGraphics g) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        int y = 8;
        if (AxiomClient.FEATURES.isEnabled("fps")) {
            g.drawString(client.font, "FPS: " + client.getFps(), 8, y, 0xFFFFFF); y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("coordinates")) {
            var p = client.player.blockPosition();
            g.drawString(client.font, "XYZ: " + p.getX() + " " + p.getY() + " " + p.getZ(), 8, y, 0xFFFFFF); y += 12;
        }
        if (AxiomClient.FEATURES.isEnabled("cps"))
            g.drawString(client.font, "CPS: " + leftCps + " | " + rightCps, 8, y, 0xFFFFFF);
        if (AxiomClient.FEATURES.isEnabled("keystrokes")) renderKeystrokes(g, client);
        if (AxiomClient.FEATURES.isEnabled("armor_hud")) renderArmor(g, client);
    }

    private static void renderKeystrokes(GuiGraphics g, Minecraft c) {
        int x=8, y=c.getWindow().getGuiScaledHeight()-78;
        drawKey(g,c,"W",c.options.keyUp,x+22,y,20);
        drawKey(g,c,"A",c.options.keyLeft,x,y+22,20);
        drawKey(g,c,"S",c.options.keyDown,x+22,y+22,20);
        drawKey(g,c,"D",c.options.keyRight,x+44,y+22,20);
        drawKey(g,c,"SP",c.options.keyJump,x,y+44,66);
    }

    private static void drawKey(GuiGraphics g,Minecraft c,String text,net.minecraft.client.KeyMapping key,int x,int y,int w) {
        g.fill(x,y,x+w,y+20,key.isDown()?0xA0FFFFFF:0x60303030);
        g.drawCenteredString(c.font,text,x+w/2,y+6,0xFFFFFF);
    }

    private static void renderArmor(GuiGraphics g, Minecraft c) {
        Inventory inv=c.player.getInventory();
        int x=c.getWindow().getGuiScaledWidth()-88, y=c.getWindow().getGuiScaledHeight()-24;
        for(int i=0;i<4;i++){
            ItemStack stack=inv.getItem(36+i);
            g.renderItem(stack,x+i*20,y);
        }
    }
}