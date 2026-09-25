package axiom.client.online;

import axiom.client.schematic.LitematicImporter;
import axiom.client.schematic.SchematicEngine;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/** Public online schematic search using GitHub's public repository/tree APIs. */
public final class OnlineSchematicService {
    private static final String API="https://api.github.com";
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final long MAX_API_RESPONSE=2*1024*1024L;
    private static final long MAX_DOWNLOAD_BYTES=32*1024*1024L;
    private final Path dir;
    public OnlineSchematicService(Path dir){this.dir=dir;}
    public List<Result> search(String query) throws IOException,InterruptedException {
        String q=URLEncoder.encode(query+" litematic",java.nio.charset.StandardCharsets.UTF_8);
        String json=get(API+"/search/repositories?q="+q+"&per_page=8");
        List<Result> out=new ArrayList<>();
        Matcher m=Pattern.compile("\\\"full_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[\\s\\S]*?\\\"default_branch\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        while(m.find()&&out.size()<12){String repo=m.group(1),branch=m.group(2); for(String file:findFiles(repo,branch)){out.add(new Result(file.substring(file.lastIndexOf('/')+1),repo,branch,file)); if(out.size()>=12)break;}}
        return out;
    }
    private List<String> findFiles(String repo,String branch) throws IOException,InterruptedException {
        String json=get(API+"/repos/"+repo+"/git/trees/"+URLEncoder.encode(branch,java.nio.charset.StandardCharsets.UTF_8)+"?recursive=1");
        List<String> out=new ArrayList<>(); Matcher m=Pattern.compile("\\\"path\\\"\\s*:\\s*\\\"([^\\\"]+\\.litematic)\\\"").matcher(json);
        while(m.find()&&out.size()<4)out.add(m.group(1)); return out;
    }
    public Path download(Result r) throws IOException,InterruptedException {
        Files.createDirectories(dir); String safe=r.name().replaceAll("[^A-Za-z0-9._-]","_"); if(!safe.toLowerCase(Locale.ROOT).endsWith(".litematic"))safe+=".litematic";
        Path out=dir.resolve(safe).normalize(); if(!out.startsWith(dir.normalize()))throw new IOException("Invalid filename");
        String url="https://raw.githubusercontent.com/"+encodePath(r.repo())+"/"+encodePath(r.branch())+"/"+encodePath(r.path());
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("User-Agent","Axiom-Minecraft-Mod").GET().build();
        HttpResponse<byte[]> res=http.send(req,HttpResponse.BodyHandlers.ofByteArray()); if(res.statusCode()!=200)throw new IOException("Download failed: HTTP "+res.statusCode());
        if(res.body().length>MAX_DOWNLOAD_BYTES)throw new IOException("Schematic is larger than 32 MB"); Path tmp=out.resolveSibling(out.getFileName()+".download");
        try { Files.write(tmp,res.body(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING); Files.move(tmp,out,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException e){ Files.move(tmp,out,StandardCopyOption.REPLACE_EXISTING); }
        catch(IOException e){ try{Files.deleteIfExists(tmp);}catch(IOException ignored){} throw e; }
        return out;
    }
    public SchematicEngine importDownloaded(Path path) throws IOException{return new LitematicImporter().load(path);}
    private static String encodePath(String path) {
        StringBuilder out = new StringBuilder(path.length() + 16);
        for (String segment : path.split("/", -1)) {
            if (out.length() > 0) out.append('/');
            out.append(URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return out.toString();
    }

    private String get(String url) throws IOException,InterruptedException {
        HttpRequest r=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).header("Accept","application/vnd.github+json").header("User-Agent","Axiom-Minecraft-Mod").GET().build();
        HttpResponse<String> x=http.send(r,HttpResponse.BodyHandlers.ofString());
        if(x.statusCode()==403 && x.headers().firstValue("X-RateLimit-Remaining").orElse("").equals("0")) throw new IOException("GitHub API rate limit reached");
        if(x.statusCode()==429) throw new IOException("Online search rate limited; try again later");
        if(x.statusCode()!=200)throw new IOException("Online search failed: HTTP "+x.statusCode());
        if(x.body().length()>MAX_API_RESPONSE) throw new IOException("GitHub API response is too large");
        return x.body();
    }
    public record Result(String name,String repo,String branch,String path){public String display(){return name+"  •  "+repo;}}
}
