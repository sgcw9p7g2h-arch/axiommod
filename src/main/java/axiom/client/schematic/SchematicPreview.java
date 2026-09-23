package axiom.client.schematic;

import axiom.client.AxiomClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Lightweight wireframe preview using Minecraft 1.21.11's submit-node rendering path. */
public final class SchematicPreview {
    private static final int MAX_BLOCKS = 2500;
    private static final int COLOR = 0xA060FFFF;
    private static boolean enabled = true;

    private SchematicPreview() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(SchematicPreview::render);
    }

    public static boolean enabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        if (!enabled || AxiomClient.SCHEMATICS.selected() == null || AxiomClient.BUILDER.origin() == null) return;

        var matrices = context.matrices();
        var camera = context.gameRenderer().getMainCamera();
        Vec3 cameraPos = camera.position();
        List<SchematicBlock> blocks = AxiomClient.SCHEMATICS.selected().transformedBlocks();
        var origin = AxiomClient.BUILDER.origin();

        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        context.commandQueue().submitCustomGeometry(
                matrices,
                RenderTypes.lines(),
                (pose, consumer) -> drawPreview(pose, consumer, blocks, origin)
        );

        matrices.popPose();
    }

    private static void drawPreview(PoseStack.Pose pose, VertexConsumer consumer, List<SchematicBlock> blocks, net.minecraft.core.BlockPos origin) {
        int count = 0;
        int red = (COLOR >>> 24) & 0xFF;
        int green = (COLOR >>> 16) & 0xFF;
        int blue = (COLOR >>> 8) & 0xFF;
        int alpha = COLOR & 0xFF;

        for (SchematicBlock block : blocks) {
            if (count++ >= MAX_BLOCKS) break;
            var pos = origin.offset(block.x(), block.y(), block.z());
            float x = (float) pos.getX();
            float y = (float) pos.getY();
            float z = (float) pos.getZ();
            float x2 = x + 1.0f;
            float y2 = y + 1.0f;
            float z2 = z + 1.0f;

            line(pose, consumer, x, y, z, x2, y, z, red, green, blue, alpha);
            line(pose, consumer, x2, y, z, x2, y, z2, red, green, blue, alpha);
            line(pose, consumer, x2, y, z2, x, y, z2, red, green, blue, alpha);
            line(pose, consumer, x, y, z2, x, y, z, red, green, blue, alpha);
            line(pose, consumer, x, y2, z, x2, y2, z, red, green, blue, alpha);
            line(pose, consumer, x2, y2, z, x2, y2, z2, red, green, blue, alpha);
            line(pose, consumer, x2, y2, z2, x, y2, z2, red, green, blue, alpha);
            line(pose, consumer, x, y2, z2, x, y2, z, red, green, blue, alpha);
            line(pose, consumer, x, y, z, x, y2, z, red, green, blue, alpha);
            line(pose, consumer, x2, y, z, x2, y2, z, red, green, blue, alpha);
            line(pose, consumer, x2, y, z2, x2, y2, z2, red, green, blue, alpha);
            line(pose, consumer, x, y, z2, x, y2, z2, red, green, blue, alpha);
        }
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
    }
}

