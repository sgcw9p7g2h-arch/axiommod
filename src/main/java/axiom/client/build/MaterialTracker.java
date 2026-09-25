package axiom.client.build;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MaterialTracker {
    private final Map<String, Integer> required = new HashMap<>();
    private final Map<String, Integer> available = new HashMap<>();

    public void clear() {
        required.clear();
        available.clear();
    }

    public void require(String id) {
        required.merge(id, 1, Integer::sum);
    }

    public void scan(LocalPlayer player) {
        available.clear();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                available.merge(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        stack.getCount(),
                        Integer::sum
                );
            }
        }
    }

    /**
     * Returns the number of inventory items still needed for a schematic block.
     * Required entries are keyed by serialized block state, while inventory is
     * keyed by item id, so the block state must be resolved before comparing them.
     */
    public int missingFor(String blockId) {
        Block block = resolveBlock(blockId);
        if (block == null || !(block.asItem() instanceof BlockItem item)) {
            return 0;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        int requiredCount = 0;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            Block requiredBlock = resolveBlock(entry.getKey());
            if (requiredBlock != null && requiredBlock.asItem() instanceof BlockItem requiredItem
                    && BuiltInRegistries.ITEM.getKey(requiredItem).toString().equals(itemId)) {
                requiredCount += entry.getValue();
            }
        }

        return Math.max(0, requiredCount - available.getOrDefault(itemId, 0));
    }

    public Map<String, Integer> required() {
        return Collections.unmodifiableMap(required);
    }

    private static Block resolveBlock(String encodedState) {
        if (encodedState == null || encodedState.isBlank()) {
            return null;
        }

        int bracket = encodedState.indexOf('[');
        String id = bracket >= 0 ? encodedState.substring(0, bracket) : encodedState;
        try {
            return BuiltInRegistries.BLOCK.get(Identifier.parse(id))
                    .map(Holder::value)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
