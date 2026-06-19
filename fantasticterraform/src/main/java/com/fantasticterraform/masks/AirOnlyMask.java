package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Solo coloca donde actualmente hay aire. */
public final class AirOnlyMask implements Mask {

    @Override
    public boolean test(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }
}
