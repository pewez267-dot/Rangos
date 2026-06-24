package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Solo afecta bloques con linea de vision directa al cielo. */
public final class SkyExposedMask implements Mask {

    @Override
    public boolean test(ServerLevel level, BlockPos pos) {
        return level.canSeeSky(pos);
    }
}
