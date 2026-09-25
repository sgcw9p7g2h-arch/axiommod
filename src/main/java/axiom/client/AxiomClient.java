package axiom.client;

import axiom.client.build.*;
import axiom.client.features.FeatureHud;
import axiom.client.features.FeatureManager;
import axiom.client.features.MovementFeatures;
import axiom.client.schematic.*;
import axiom.client.ui.AxiomFeaturesScreen;
import axiom.client.ui.SchematicBrowserScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class AxiomClient implements ClientModInitializer {
    public static final String MOD_ID="axiom";
    public static final SchematicFileManager SCHEMATICS=new SchematicFileManager();
    public static final BuildConfig CONFIG=new BuildConfig(FabricLoader.getInstance().getConfigDir().resolve("axiom.properties"));
    public static final AutoBuilder BUILDER=new AutoBuilder();
    public static final BuildHud HUD=new BuildHud();
    public static final FeatureManager FEATURES=new FeatureManager(FabricLoader.getInstance().getConfigDir());
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID,"controls"));
    private static KeyMapping menu,build,pause,origin,rotate,cancel,features;
    public static KeyMapping menuKey(){return menu;} public static KeyMapping buildKey(){return build;}
    public static KeyMapping pauseKey(){return pause;} public static KeyMapping originKey(){return origin;}
    public static KeyMapping rotateKey(){return rotate;} public static KeyMapping cancelKey(){return cancel;}
    public static KeyMapping featuresKey(){return features;}

    @Override public void onInitializeClient(){
        CONFIG.load(); FEATURES.load(); SCHEMATICS.loadAll(); SchematicPreview.register();
        menu=key("open_browser",GLFW.GLFW_KEY_M); build=key("build",GLFW.GLFW_KEY_B);
        pause=key("pause",GLFW.GLFW_KEY_P); origin=key("origin",GLFW.GLFW_KEY_O);
        rotate=key("rotate",GLFW.GLFW_KEY_R); cancel=key("cancel",GLFW.GLFW_KEY_X);
        features=key("open_features",GLFW.GLFW_KEY_F8);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick); HudHooks.register();
    }
    private KeyMapping key(String name,int code){
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.axiom."+name,InputConstants.Type.KEYSYM,code,CATEGORY));
    }
    private void tick(Minecraft client){
        while(menu.consumeClick()) client.setScreen(new SchematicBrowserScreen());
        while(features.consumeClick()) client.setScreen(new AxiomFeaturesScreen(client.screen));
        while(origin.consumeClick()) BUILDER.selectOrigin(client);
        while(rotate.consumeClick()){if(SCHEMATICS.selected()!=null)SCHEMATICS.selected().rotateClockwise();}
        while(cancel.consumeClick()) BUILDER.stop();
        while(pause.consumeClick()){if(BUILDER.isPaused())BUILDER.resume(client);else BUILDER.pause();}
        while(build.consumeClick()){if(BUILDER.isRunning()){if(BUILDER.isPaused())BUILDER.resume(client);else BUILDER.pause();}else BUILDER.start(client,SCHEMATICS.selected());}
        MovementFeatures.tick(client); FeatureHud.tick(client); BUILDER.tick(client); HUD.tick(BUILDER);
    }
}