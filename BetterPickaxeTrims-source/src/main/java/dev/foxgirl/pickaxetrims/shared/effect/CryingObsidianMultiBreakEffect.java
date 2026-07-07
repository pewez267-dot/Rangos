/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.NotNull
 */
package dev.foxgirl.pickaxetrims.shared.effect;

import dev.foxgirl.pickaxetrims.shared.PickaxeTrimsImpl;
import dev.foxgirl.pickaxetrims.shared.effect.AbstractEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class CryingObsidianMultiBreakEffect
extends AbstractEffect {
    @Override
    public void onTickEnd(@NotNull MinecraftServer server) {
    }

    private void breakIfMatches(ServerPlayer player, Level level, Block block, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() != block) {
            return;
        }
        if (!CryingObsidianMultiBreakEffect.canBreak(level, pos, player)) {
            return;
        }
        level.destroyBlock(pos, true, (Entity)player);
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

    @Override
    public void onBlockBreak(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ServerPlayer player) {
        int radius = PickaxeTrimsImpl.getInstance().config.cryingObsidianMultiBreakRadius;
        Block block = state.getBlock();
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -radius; y <= radius; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    this.breakIfMatches(player, level, block, pos.offset(x, y, z));
                }
            }
        }
    }
}

