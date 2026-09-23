package axiom.client.ui;
import axiom.client.AxiomClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

public final class SchematicBrowserScreen extends Screen {
 private int selected;
 public SchematicBrowserScreen(){super(Component.literal("Axiom Schematics")); selectedIndexFromManager();}
 private void selectedIndexFromManager(){var current=AxiomClient.SCHEMATICS.selected();var list=AxiomClient.SCHEMATICS.all();selected=0;if(current!=null){int i=list.indexOf(current);if(i>=0)selected=i;}}
 protected void init(){
  addRenderableWidget(Button.builder(Component.literal("Select"),b->{AxiomClient.SCHEMATICS.select(selected);onClose();}).bounds(width/2-155,height-35,75,20).build());
  addRenderableWidget(Button.builder(Component.literal("Online"),b->minecraft.setScreen(new OnlineSchematicScreen())).bounds(width/2-75,height-35,75,20).build());
  addRenderableWidget(Button.builder(Component.literal("Preview"),b->{axiom.client.schematic.SchematicPreview.toggle();}).bounds(width/2+5,height-35,75,20).build());
  addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width/2+85,height-35,75,20).build());
 }
 public void render(GuiGraphics g,int mx,int my,float d){g.drawCenteredString(font,title,width/2,20,0xFFFFFF);var list=AxiomClient.SCHEMATICS.all();if(list.isEmpty())g.drawCenteredString(font,Component.literal("No .axschem or .litematic files found"),width/2,60,0xAAAAAA);else for(int i=0;i<list.size()&&i<20;i++)g.drawCenteredString(font,Component.literal((i==selected?"> ":"")+list.get(i).name()+"  ["+list.get(i).size()+"]"),width/2,55+i*18,0xFFFFFF);super.render(g,mx,my,d);}
 public boolean keyPressed(KeyEvent key){int k=key.key();if(k==264){selected=Math.min(selected+1,Math.max(0,AxiomClient.SCHEMATICS.all().size()-1));return true;}if(k==265){selected=Math.max(0,selected-1);return true;}if(k==257){AxiomClient.SCHEMATICS.select(selected);onClose();return true;}if(k==341&&minecraft!=null){minecraft.setScreen(new OnlineSchematicScreen());return true;}return super.keyPressed(key);}
}
