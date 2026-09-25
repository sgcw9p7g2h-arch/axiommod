package axiom.client.schematic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

/** Converts serialized block-state strings into live Minecraft BlockState objects. */
public final class BlockStateResolver {
    private static final int CACHE_LIMIT = 2048;
    private final LinkedHashMap<String, BlockState> cache =
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, BlockState> eldest) {
                    return size() > CACHE_LIMIT;
                }
            };

    public BlockState resolve(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        if (cache.containsKey(encoded)) return cache.get(encoded);

        BlockState resolved = resolveUncached(encoded);
        cache.put(encoded, resolved);
        return resolved;
    }

    public void clearCache() {
        cache.clear();
    }

    private BlockState resolveUncached(String encoded) {
        int bracket=encoded.indexOf('[');
        String id=bracket<0?encoded:encoded.substring(0,bracket);
        Block block;
        try {
            block=BuiltInRegistries.BLOCK.get(Identifier.parse(id)).map(Holder::value).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (block == null) return null;

        BlockState state=block.defaultBlockState();
        if (bracket<0) return state;

        int end=encoded.lastIndexOf(']');
        if(end<bracket) return state;

        String body=encoded.substring(bracket+1,end);
        for(String part:split(body)) {
            int eq=part.indexOf('=');
            if(eq<=0) continue;
            String name=part.substring(0,eq).trim(), value=part.substring(eq+1).trim();
            Property<?> property=find(state,name);
            if(property==null) continue;
            state=apply(state,property,value);
        }
        return state;
    }

    private static Property<?> find(BlockState state,String name){
        for(Property<?> p:state.getProperties()) if(p.getName().equals(name)) return p;
        return null;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static BlockState apply(BlockState state,Property property,String value){
        Optional parsed=property.getValue(value);
        return parsed.isPresent()?state.setValue(property,(Comparable)parsed.get()):state;
    }

    private static List<String> split(String s){
        List<String> out=new ArrayList<>();
        int start=0;
        for(int i=0;i<s.length();i++) if(s.charAt(i)==','){
            out.add(s.substring(start,i));
            start=i+1;
        }
        out.add(s.substring(start));
        return out;
    }
}
