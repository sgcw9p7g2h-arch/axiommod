package axiom.client.schematic;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Minimal dependency-free NBT reader used only for Litematica imports. */
final class NbtReader {
    static final int END=0, BYTE=1, SHORT=2, INT=3, LONG=4, FLOAT=5, DOUBLE=6, BYTE_ARRAY=7, STRING=8, LIST=9, COMPOUND=10, INT_ARRAY=11, LONG_ARRAY=12;
    private final DataInputStream in;
    private NbtReader(InputStream input){in=new DataInputStream(new BufferedInputStream(input));}
    static NbtCompound readGzip(InputStream input) throws IOException { try(var gz=new GZIPInputStream(input)){ return new NbtReader(gz).readRoot(); } }
    private NbtCompound readRoot() throws IOException { int type=in.readUnsignedByte(); if(type!=COMPOUND) throw new IOException("Litematic root is not a compound"); in.readUTF(); return (NbtCompound) readPayload(type); }
    private Object readPayload(int type) throws IOException {
        return switch(type){
            case END -> null; case BYTE -> in.readByte(); case SHORT -> in.readShort(); case INT -> in.readInt(); case LONG -> in.readLong();
            case FLOAT -> in.readFloat(); case DOUBLE -> in.readDouble(); case BYTE_ARRAY -> { int n=in.readInt(); if(n<0||n>64*1024*1024) throw new IOException("invalid byte array length: "+n); byte[] a=new byte[n]; in.readFully(a); yield a; }
            case STRING -> in.readUTF();
            case LIST -> {
                int child=in.readUnsignedByte();
                int n=in.readInt();
                if(n<0||n>10_000_000) throw new IOException("invalid list length: "+n);
                if(child==END && n>0) throw new IOException("invalid NBT list: END element type with non-empty list");
                List<Object> list=new ArrayList<>(n);
                for(int i=0;i<n;i++) list.add(readPayload(child));
                yield new NbtList(child,list);
            }
            case COMPOUND -> { Map<String,Object> map=new LinkedHashMap<>(); while(true){ int t=in.readUnsignedByte(); if(t==END) break; String name=in.readUTF(); map.put(name,readPayload(t)); } yield new NbtCompound(map); }
            case INT_ARRAY -> { int n=in.readInt(); if(n<0||n>16*1024*1024) throw new IOException("invalid int array length: "+n); int[] a=new int[n]; for(int i=0;i<n;i++) a[i]=in.readInt(); yield a; }
            case LONG_ARRAY -> { int n=in.readInt(); if(n<0||n>16*1024*1024) throw new IOException("invalid long array length: "+n); long[] a=new long[n]; for(int i=0;i<n;i++) a[i]=in.readLong(); yield a; }
            default -> throw new IOException("unknown NBT tag "+type);
        };
    }
    record NbtList(int elementType,List<Object> values){}
    static final class NbtCompound {
        private final Map<String,Object> values;
        NbtCompound(){values=new LinkedHashMap<>();} NbtCompound(Map<String,Object> values){this.values=values;}
        Object get(String k){return values.get(k);} String string(String k){Object v=get(k); return v instanceof String s?s:null;}
        int intValue(String k,int d){Object v=get(k); return v instanceof Number n?n.intValue():d;}
        long longValue(String k,long d){Object v=get(k); return v instanceof Number n?n.longValue():d;}
        NbtCompound compound(String k){Object v=get(k); return v instanceof NbtCompound c?c:null;}
        NbtList list(String k){Object v=get(k); return v instanceof NbtList l?l:null;}
        long[] longArray(String k){Object v=get(k); return v instanceof long[] a?a:null;}
        Set<String> keys(){return values.keySet();}
    }
}
