package axiom.client.schematic;

/** A block plus its serialized Minecraft block-state properties. */
public record SchematicBlock(int x, int y, int z, String blockState) {
    public String blockId() {
        int bracket = blockState.indexOf('[');
        return bracket < 0 ? blockState : blockState.substring(0, bracket);
    }
}
