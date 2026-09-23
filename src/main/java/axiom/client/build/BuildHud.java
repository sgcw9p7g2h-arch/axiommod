package axiom.client.build;
import net.minecraft.client.gui.GuiGraphics;
public final class BuildHud { private String msg=""; private int ticks;
 public void tick(AutoBuilder b){if(ticks>0)ticks--;}
 public void message(String s){msg=s;ticks=80;} public void render(GuiGraphics g){var b=axiom.client.AxiomClient.BUILDER; if(b==null)return; g.drawString(net.minecraft.client.Minecraft.getInstance().font,"Axiom Auto Build",8,8,0xFFFFFF);g.drawString(net.minecraft.client.Minecraft.getInstance().font,(b.isRunning()?(b.isPaused()?"Paused":"Building"):"Idle")+"  Placed: "+b.queue().completed()+"  Remaining: "+b.queue().remaining(),8,20,0xFFFFFF);if(ticks>0)g.drawString(net.minecraft.client.Minecraft.getInstance().font,msg,8,32,0xFFFF55);}
}
