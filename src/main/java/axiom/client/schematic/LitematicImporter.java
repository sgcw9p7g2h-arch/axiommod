package axiom.client.schematic;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Dependency-free Litematica .litematic importer. */
public final class LitematicImporter {
    private static final long MAX_TOTAL_BLOCKS = 10_000_000L;

    public SchematicEngine load(Path path) throws IOException {
        try (InputStream in=Files.newInputStream(path)) {
            return load(in, strip(path.getFileName().toString()));
        }
    }

    public SchematicEngine load(InputStream in,String name) throws IOException {
        NbtReader.NbtCompound root=NbtReader.readGzip(in);
        NbtReader.NbtCompound regions=root.compound("Regions");
        if(regions==null) throw new IOException("Missing Regions tag");
        List<SchematicBlock> blocks=new ArrayList<>();
        long loadedBlocks=0;

        for(String regionName:regions.keys()) {
            NbtReader.NbtCompound region=regions.compound(regionName);
            if(region==null) continue;
            int sx=region.intValue("SizeX",region.intValue("Size",0));
            int sy=region.intValue("SizeY",0), sz=region.intValue("SizeZ",0);
            NbtReader.NbtCompound size=region.compound("Size");
            if(size!=null){sx=size.intValue("x",sx);sy=size.intValue("y",sy);sz=size.intValue("z",sz);}
            NbtReader.NbtCompound pos=region.compound("Position");
            int px=pos==null?0:pos.intValue("x",0), py=pos==null?0:pos.intValue("y",0), pz=pos==null?0:pos.intValue("z",0);
            int ax=Math.abs(sx), ay=Math.abs(sy), az=Math.abs(sz);
            if(ax==0||ay==0||az==0) continue;

            long totalLong=(long)ax*ay*az;
            if(totalLong>MAX_TOTAL_BLOCKS) throw new IOException("Schematic region is too large: "+totalLong+" blocks");
            if(loadedBlocks>MAX_TOTAL_BLOCKS-totalLong) throw new IOException("Schematic contains more than 10,000,000 blocks");

            NbtReader.NbtList palette=region.list("BlockStatePalette");
            if(palette==null) palette=region.list("Palette");
            long[] states=region.longArray("BlockStates");
            if(palette==null||states==null) continue;

            List<String> paletteStates=new ArrayList<>();
            for(Object entry:palette.values()) {
                if(!(entry instanceof NbtReader.NbtCompound compound)) throw new IOException("Invalid block-state palette entry");
                paletteStates.add(paletteState(compound));
            }
            if(paletteStates.isEmpty()) continue;

            int bits=Math.max(2,32-Integer.numberOfLeadingZeros(paletteStates.size()-1));
            if(bits>32) throw new IOException("Invalid palette width");
            long packedBits=totalLong*bits;
            long requiredLongs=(packedBits+63L)/64L;
            if(requiredLongs>Integer.MAX_VALUE || states.length<requiredLongs) throw new IOException("BlockStates array is truncated");

            int total=(int)totalLong;
            for(int index=0;index<total;index++) {
                int paletteIndex=readPacked(states,index,bits);
                if(paletteIndex<0||paletteIndex>=paletteStates.size()) continue;
                String state=paletteStates.get(paletteIndex);
                if(state.startsWith("minecraft:air")) continue;
                int x=index%ax, yz=index/ax, z=yz%az, y=yz/az;
                if(sx<0)x=-x-1; if(sy<0)y=-y-1; if(sz<0)z=-z-1;
                blocks.add(new SchematicBlock(px+x,py+y,pz+z,state));
            }
            loadedBlocks+=totalLong;
        }
        return new SchematicEngine(name,blocks);
    }

    private static int readPacked(long[] data,int index,int bits){
        long bit=(long)index*bits;
        int word=(int)(bit>>>6), offset=(int)(bit&63);
        long value=data[word]>>>offset;
        int used=64-offset;
        if(used<bits && word+1<data.length)value|=data[word+1]<<(used);
        return (int)(value&((1L<<bits)-1));
    }

    private static String paletteState(NbtReader.NbtCompound p){
        String name=p.string("Name"); if(name==null) name="minecraft:air";
        NbtReader.NbtCompound props=p.compound("Properties");
        if(props==null||props.keys().isEmpty()) return name;
        List<String> keys=new ArrayList<>(props.keys()); Collections.sort(keys);
        StringBuilder b=new StringBuilder(name).append('[');
        for(int i=0;i<keys.size();i++){if(i>0)b.append(',');String k=keys.get(i);b.append(k).append('=').append(String.valueOf(props.get(k)));}
        return b.append(']').toString();
    }

    private static String strip(String n){return n.endsWith(".litematic")?n.substring(0,n.length()-9):n;}
}
