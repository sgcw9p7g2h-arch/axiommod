package axiom.client.ui;

import axiom.client.AxiomClient;
import axiom.client.online.OnlineSchematicService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.*;

public final class OnlineSchematicScreen extends Screen {
    private EditBox search;
    private final List<OnlineSchematicService.Result> results=new ArrayList<>();
    private int selected=-1; private String status="Search public GitHub schematic repositories"; private boolean busy;
    private final OnlineSchematicService service=new OnlineSchematicService(AxiomClient.SCHEMATICS.directory());
    public OnlineSchematicScreen(){super(Component.literal("Axiom Online Schematics"));}
    @Override protected void init(){
        search=new EditBox(font,width/2-150,30,300,20,Component.literal("Search")); search.setMaxLength(80); addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Search"),b->doSearch()).bounds(width/2-150,55,95,20).build());
        addRenderableWidget(Button.builder(Component.literal("Download"),b->download()).bounds(width/2-50,55,95,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(width/2+50,55,100,20).build());
    }
    private void doSearch(){ if(busy)return; String q=search.getValue().trim(); if(q.isEmpty()){status="Enter a search term";return;} busy=true;status="Searching...";
        Thread.startVirtualThread(()->{try{List<OnlineSchematicService.Result> found=service.search(q); Minecraft.getInstance().execute(()->{results.clear();results.addAll(found);selected=found.isEmpty()?-1:0;busy=false;status=found.isEmpty()?"No public .litematic files found":"Found "+found.size()+" results";});}catch(Exception e){Minecraft.getInstance().execute(()->{busy=false;status="Search failed: "+e.getMessage();});}});
    }
    private void download(){ if(busy||selected<0||selected>=results.size())return; busy=true; status="Downloading..."; OnlineSchematicService.Result r=results.get(selected);
        Thread.startVirtualThread(()->{try{Path p=service.download(r); Minecraft.getInstance().execute(()->{AxiomClient.SCHEMATICS.loadAll();busy=false;status="Downloaded: "+p.getFileName();});}catch(Exception e){Minecraft.getInstance().execute(()->{busy=false;status="Download failed: "+e.getMessage();});}});
    }
    @Override public void render(GuiGraphics g,int mx,int my,float d){renderBackground(g,mx,my,d);g.drawCenteredString(font,title,width/2,12,0xFFFFFF);g.drawString(font,status,10,height-20,0xAAAAAA);
        for(int i=0;i<Math.min(results.size(),10);i++){int y=85+i*22; int c=i==selected?0xA060FFFF:0xFFFFFF;g.drawString(font,(i==selected?"> ":"  ")+results.get(i).display(),20,y,c);} super.render(g,mx,my,d);}
    @Override public boolean keyPressed(KeyEvent key){ int code=key.key(); if(code==264){selected=Math.min(results.size()-1,selected+1);return true;} if(code==265){selected=Math.max(0,selected-1);return true;} if(code==257&&!search.isFocused()){download();return true;} return super.keyPressed(key); }
}
