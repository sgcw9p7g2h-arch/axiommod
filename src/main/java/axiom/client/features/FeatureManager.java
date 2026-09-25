package axiom.client.features;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class FeatureManager {
    private final Map<String,AxiomFeature> features=new LinkedHashMap<>();
    private final Path file;
    public FeatureManager(Path configDir){file=configDir.resolve("axiom-features.properties");registerDefaults();}
    private void registerDefaults(){
        register("toggle_sprint","Toggle Sprint","Movement",true);
        register("toggle_sneak","Toggle Sneak","Movement",false);
        register("zoom","Zoom","Visual",true);
        register("keystrokes","Keystrokes","HUD",true);
        register("cps","CPS Counter","HUD",true);
        register("coordinates","Coordinates","HUD",true);
        register("armor_hud","Armor HUD","HUD",true);
        register("fps","FPS Counter","HUD",true);
        register("fullbright","Fullbright","Visual",false);
        register("crosshair","Crosshair","Visual",true);
        register("screenshot","Screenshot Tools","Utility",true);
        register("fps_graph","FPS Graph","HUD",false);
        register("direction","Direction","HUD",false);
        register("playtime","Playtime","HUD",false);
        register("server_address","Server Address","HUD",false);
        register("held_item","Held Item","HUD",false);
        register("potion_effects","Potion Effects","HUD",false);
        register("snaplook","Snaplook","Visual",false);
        register("freelook","Freelook","Visual",false);
    }
    public void register(String id,String name,String category,boolean enabled){features.putIfAbsent(id,new AxiomFeature(id,name,category,enabled));}
    public Collection<AxiomFeature> all(){return features.values();}
    public AxiomFeature get(String id){return features.get(id);}
    public boolean isEnabled(String id){AxiomFeature f=features.get(id);return f!=null&&f.enabled();}
    public void setEnabled(String id,boolean enabled){AxiomFeature f=features.get(id);if(f!=null)f.setEnabled(enabled);}
    public void load(){
        if(!Files.exists(file))return;
        Properties p=new Properties();
        try(var in=Files.newInputStream(file)){p.load(in);for(AxiomFeature f:features.values()){String v=p.getProperty(f.id());if(v!=null)f.setEnabled(Boolean.parseBoolean(v));}}catch(IOException ignored){}
    }
    public void save(){
        Properties p=new Properties(); for(AxiomFeature f:features.values())p.setProperty(f.id(),Boolean.toString(f.enabled()));
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try{
            Files.createDirectories(file.getParent());
            try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Axiom feature settings");}
            try{Files.move(tmp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}
            catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException ignored){try{Files.deleteIfExists(tmp);}catch(IOException ignored2){}}
    }
}