package axiom.client.build;

import axiom.client.AxiomClient;
import axiom.client.schematic.SchematicBlock;
import axiom.client.schematic.SchematicEngine;
import axiom.client.schematic.BlockStateResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Normal Minecraft-interaction schematic builder. No packet spoofing or anti-cheat bypasses. */
public final class AutoBuilder {
    private final BuildQueue queue = new BuildQueue();
    private final MaterialTracker materials = new MaterialTracker();
    private final BlockStateResolver stateResolver = new BlockStateResolver();
    private SchematicEngine schematic;
    private BlockPos origin;
    private boolean running;
    private boolean paused;
    private int cooldown;
    private int failedPlacementTicks;

    public BuildQueue queue() { return queue; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public BlockPos origin() { return origin; }
    public MaterialTracker materials() { return materials; }

    public void start(Minecraft client, SchematicEngine selected) {
        if (client.level == null || client.player == null || selected == null || origin == null) {
            AxiomClient.HUD.message("Select a schematic and origin first");
            return;
        }

        schematic = selected;
        queue.clear();
        materials.clear();

        for (SchematicBlock block : selected.transformedBlocks()) {
            queue.add(new BuildTask(origin.offset(block.x(), block.y(), block.z()), block.blockId()));
            materials.require(block.blockId());
        }

        materials.scan(client.player);
        running = true;
        paused = false;
        cooldown = 0;
        failedPlacementTicks = 0;
        AxiomClient.CONFIG.save();
        AxiomClient.HUD.message("Build started: " + selected.name());
    }

    public void tick(Minecraft client) {
        if (!running || paused || client.level == null || client.player == null || client.gameMode == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        BuildTask task = queue.peek();
        if (task == null) {
            finish("Build complete");
            return;
        }

        BlockState targetState = stateResolver.resolve(task.blockId());
        Block target = targetState == null ? null : targetState.getBlock();
        if (target == null) {
            queue.poll();
            queue.skip();
            failedPlacementTicks = 0;
            AxiomClient.HUD.message("Skipped unknown block: " + task.blockId());
            return;
        }

        // Air entries are common in exported schematics. Axiom's placement builder
        // deliberately does not treat them as a destructive operation.
        if (target.defaultBlockState().isAir()) {
            queue.poll();
            queue.skip();
            failedPlacementTicks = 0;
            return;
        }

        BlockState current = client.level.getBlockState(task.position());
        if (AxiomClient.CONFIG.skipExisting && current.equals(targetState)) {
            queue.poll();
            queue.complete();
            failedPlacementTicks = 0;
            return;
        }

        if (!current.isAir() && !current.canBeReplaced()) {
            queue.poll();
            queue.skip();
            failedPlacementTicks = 0;
            return;
        }

        double maxReach = AxiomClient.CONFIG.maxReach;
        if (client.player.distanceToSqr(task.position().getCenter()) > maxReach * maxReach) {
            pauseWithMessage("Paused: block is out of reach");
            return;
        }

        Item item = target.asItem();
        int slot = findHotbar(item, client.player);
        if (slot < 0) {
            pauseWithMessage("Paused: missing " + task.blockId());
            return;
        }

        if (place(client, client.player, task.position(), slot)) {
            failedPlacementTicks = 0;
            cooldown = Math.max(1, AxiomClient.CONFIG.tickDelay);
        } else {
            // A block may be valid but temporarily have no support because another
            // block in the same schematic layer has not been placed yet. Defer it
            // instead of immediately giving up on the whole build.
            failedPlacementTicks++;
            queue.defer();
            cooldown = 1;
            if (failedPlacementTicks >= Math.max(1, queue.remaining())) {
                pauseWithMessage("Paused: no progress; check placement/support");
            }
            return;
        }

        // Only consume the queue entry after the world confirms the target block.
        if (client.level.getBlockState(task.position()).equals(targetState)) {
            queue.poll();
            queue.complete();
        }
    }

    private int findHotbar(Item item, LocalPlayer player) {
        if (item == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) return slot;
        }
        return -1;
    }

    private boolean place(Minecraft client, LocalPlayer player, BlockPos position, int slot) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = position.relative(direction);
            BlockState neighborState = client.level.getBlockState(neighbor);
            if (neighborState.isAir()) continue;
            if (!neighborState.isFaceSturdy(client.level, neighbor, direction.getOpposite())) continue;

            player.getInventory().setSelectedSlot(slot);
            Direction face = direction.getOpposite();
            Vec3 hitLocation = neighbor.getCenter()
                    .add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.49));
            BlockHitResult hit = new BlockHitResult(
                    hitLocation, face, neighbor, false);

            InteractionResult result =
                    client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    public void selectOrigin(Minecraft client) {
        if (client.hitResult instanceof BlockHitResult hit) {
            origin = hit.getBlockPos().relative(hit.getDirection());
            AxiomClient.HUD.message("Origin set: " + origin);
        } else {
            AxiomClient.HUD.message("Look at a block to set the origin");
        }
    }

    public void pause() {
        if (running) paused = true;
    }

    public void resume(Minecraft client) {
        if (running) {
            paused = false;
            cooldown = Math.max(cooldown, 1);
            failedPlacementTicks = 0;
            AxiomClient.HUD.message("Build resumed");
        }
    }

    public void stop() {
        if (!running && queue.remaining() == 0) return;
        running = false;
        paused = false;
        cooldown = 0;
        failedPlacementTicks = 0;
        queue.clear();
        AxiomClient.HUD.message("Build cancelled");
    }

    private void pauseWithMessage(String message) {
        paused = true;
        AxiomClient.HUD.message(message);
    }

    private void finish(String message) {
        running = false;
        paused = false;
        cooldown = 0;
        failedPlacementTicks = 0;
        AxiomClient.HUD.message(message);
    }
}