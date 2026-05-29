package com.revivemod.util;

import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Pose-based "is this player bleeding/downed" check that works on BOTH the
 * server and the client (it only reads synced data: the SLEEPING_POSITION
 * tracker + the block at that position). The server-side DownManager state is
 * not replicated to clients, so mixins that need to run client-side (cancel the
 * sleep screen, suppress the black sleep overlay) must use this instead.
 *
 * A player is "bleeding" when they are in the sleeping state but their sleeping
 * position is NOT a real bed (which is how we lay a downed player down).
 */
public final class BleedPose {

    private BleedPose() {}

    public static boolean isBleeding(PlayerEntity p) {
        if (p == null || !p.isSleeping()) return false;
        BlockPos pos = p.getSleepingPosition().orElse(null);
        if (pos == null) return false;
        return !(p.getWorld().getBlockState(pos).getBlock() instanceof BedBlock);
    }
}
