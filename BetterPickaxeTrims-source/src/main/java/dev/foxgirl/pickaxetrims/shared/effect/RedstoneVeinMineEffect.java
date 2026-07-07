/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.NotNull
 */
package dev.foxgirl.pickaxetrims.shared.effect;

import dev.foxgirl.pickaxetrims.shared.OreDetectUtil;
import dev.foxgirl.pickaxetrims.shared.PickaxeTrimsImpl;
import dev.foxgirl.pickaxetrims.shared.effect.AbstractEffect;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class RedstoneVeinMineEffect
extends AbstractEffect {
    private final Queue<Runnable> pendingTasks = new ArrayDeque<Runnable>();

    private void findPositions(BlockPos pos, Block block, Level level, ServerPlayer player, List<BlockPos> positions, int depth) {
        if (depth-- < 0) {
            return;
        }
        if (block != level.getBlockState(pos).getBlock()) {
            return;
        }
        if (!positions.contains(pos)) {
            positions.add(pos);
            this.findPositions(pos.north(), block, level, player, positions, depth);
            this.findPositions(pos.south(), block, level, player, positions, depth);
            this.findPositions(pos.east(), block, level, player, positions, depth);
            this.findPositions(pos.west(), block, level, player, positions, depth);
            this.findPositions(pos.above(), block, level, player, positions, depth);
            this.findPositions(pos.below(), block, level, player, positions, depth);
        }
    }

    private void sortPositions(BlockPos pos, List<BlockPos> positions) {
        positions.sort((a, b) -> {
            double aDistance = a.distSqr((Vec3i)pos);
            double bDistance = b.distSqr((Vec3i)pos);
            return Double.compare(aDistance, bDistance);
        });
    }

    @Override
    public void onTickEnd(@NotNull MinecraftServer server) {
        Runnable task = this.pendingTasks.poll();
        if (task != null) {
            task.run();
        }
    }

    @Override
    public void onBlockBreak(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ServerPlayer player) {
        Block block = state.getBlock();
        if (OreDetectUtil.isOreBlock(block)) {
            ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
            this.findPositions(pos, block, level, player, positions, PickaxeTrimsImpl.getInstance().config.redstoneVeinMineDepth);
            this.sortPositions(pos, positions);
            for (BlockPos it : positions) {
                this.pendingTasks.add(() -> {
                    if (!RedstoneVeinMineEffect.canBreak(level, it, player)) {
                        return;
                    }
                    level.destroyBlock(it, true, (Entity)player);
                });
            }
        }
    }

    /**
     * Chequeo ESTRICTO de proteccion. Solo devuelve true si el jugador realmente puede romper el bloque.
     * Valida restriccion de accion (modo aventura / proteccion de spawn) y dispara BlockEvent.BreakEvent
     * (lo que cancelan ClaimBlocks / YAWP). Fail-closed: ante cualquier duda o error, NO se rompe.
     */
    private static boolean canBreak(Level level, BlockPos pos, ServerPlayer player) {
        try {
            if (level.isEmptyBlock(pos)) {
                return false;
            }
            net.minecraft.world.level.GameType gt = player.gameMode.getGameModeForPlayer();
            if (player.blockActionRestricted(level, pos, gt)) {
                return false;
            }
            return net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level, gt, player, pos) != -1;
        }
        catch (Throwable t) {
            return false;
        }
    }
}

