package axiom.client.build;
import java.io.*; import java.nio.file.*; import java.util.Properties;
public final class BuildConfig {
 private final Path path; public int tickDelay=2; public double maxReach=6.0; public boolean skipExisting=true;
 public BuildConfig(Path path){this.path=path;} public void load(){Properties p=new Properties();try{if(Files.exists(path))try(var r=Files.newBufferedReader(path)){p.load(r);} tickDelay=Integer.parseInt(p.getProperty("tickDelay","2"));maxReach=Double.parseDouble(p.getProperty("maxReach","6.0"));skipExisting=Boolean.parseBoolean(p.getProperty("skipExisting","true"));}catch(Exception ignored){} clamp();}
 public void save(){try{Files.createDirectories(path.getParent());Properties p=new Properties();p.setProperty("tickDelay",Integer.toString(tickDelay));p.setProperty("maxReach",Double.toString(maxReach));p.setProperty("skipExisting",Boolean.toString(skipExisting));try(var w=Files.newBufferedWriter(path)){p.store(w,"Axiom build settings");}}catch(IOException ignored){}}
 public void clamp(){tickDelay=Math.max(0,Math.min(20,tickDelay));if(!Double.isFinite(maxReach)) maxReach=6.0; else maxReach=Math.max(1,Math.min(6, maxReach));}
}
