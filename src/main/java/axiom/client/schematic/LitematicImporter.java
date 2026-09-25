package axiom.client.schematic;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Dependency-free Litematica .litematic importer. */
public final class LitematicImporter {
    public SchematicEngine load(Path path) throws IOException {
        try (InputStream in=Files.newInputStream(path)) { return load(in, strip(path.getFileName().toString())); }
    }
    public SchematicEngine load(InputStream in,String name) throws IOException {
        NbtReader.NbtCompound root=NbtReader.readGzip(in);
        NbtReader.NbtCompound regions=root.compound("Regions");
        if(regions==null) throw new IOException("Missing Regions tag");
        List<SchematicBlock> blocks=new ArrayList<>();
        for(String regionName:regions.keys()) {
            NbtReader.NbtCompound region=regions.compound(regionName); if(region==null) continue;
            int sx=region.intValue("SizeX",region.intValue("Size",0));
            int sy=region.intValue("SizeY",0), sz=region.intValue("SizeZ",0);
            NbtReader.NbtCompound size=region.compound("Size"); if(size!=null){sx=size.intValue("x",sx);sy=size.intValue("y",sy);sz=size.intValue("z",sz);}
            NbtReader.NbtCompound pos=region.compound("Position"); int px=pos==null?0:pos.intValue("x",0), py=pos==null?0:pos.intValue("y",0), pz=pos==null?0:pos.intValue("z",0);
            int ax=Math.abs(sx), ay=Math.abs(sy), az=Math.abs(sz); if(ax==0||ay==0||az==0) continue;
            NbtReader.NbtList palette=region.list("BlockStatePalette"); if(palette==null) palette=region.list("Palette");
            long[] states=region.longArray("BlockStates"); if(palette==null||states==null) continue;
            List<String> paletteStates=new ArrayList<>();
            for(Object entry:palette.values()) paletteStates.add(paletteState((NbtReader.NbtCompound)entry));
            if(paletteStates.isEmpty()) continue;
            int bits=Math.max(2,32-Integer.numberOfLeadingZeros(paletteStates.size()-1)); if(bits>32) throw new IOException("Invalid palette width");
            long totalLong=(long)ax*ay*az; if(totalLong>10_000_000L) throw new IOException("Schematic is too large: "+totalLong+" blocks"); int total=(int)totalLong; for(int index=0;index<total;index++) {
                int paletteIndex=readPacked(states,index,bits); if(paletteIndex<0||paletteIndex>=paletteStates.size()) continue;
                String state=paletteStates.get(paletteIndex); if(state.startsWith("minecraft:air")) continue;
                int x=index%ax; int yz=index/ax; int z=yz%az; int y=yz/az;
                if(sx<0)x=-x-1; if(sy<0)y=-y-1; if(sz<0)z=-z-1;
                blocks.add(new SchematicBlock(px+x,py+y,pz+z,state));
            }
        }
        return new SchematicEngine(name,blocks);
    }
    private static int readPacked(long[] data,int index,int bits){ long bit=(long)index*bits; int word=(int)(bit>>>6), offset=(int)(bit&63); long value=data[word]>>>offset; int used=64-offset; if(used<bits && word+1<data.length)value|=data[word+1]<<(used); return (int)(value&((1L<<bits)-1)); }
    private static String paletteState(NbtReader.NbtCompound p){
        String name=p.string("Name"); if(name==null) name="minecraft:air";
        NbtReader.NbtCompound props=p.compound("Properties"); if(props==null||props.keys().isEmpty()) return name;
        List<String> keys=new ArrayList<>(props.keys()); Collections.sort(keys); StringBuilder b=new StringBuilder(name).append('['); for(int i=0;i<keys.size();i++){if(i>0)b.append(','); String k=keys.get(i); b.append(k).append('=').append(String.valueOf(props.get(k)));} return b.append(']').toString();
    }
    private static String strip(String n){return n.endsWith(".litematic")?n.substring(0,n.length()-9):n;}
}
