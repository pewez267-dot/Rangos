package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Solo afecta bloques dentro de un rango de altura Y (inclusive). */
public final class HeightMask implements Mask {

    private final int minY;
    private final int maxY;

    public HeightMask(int minY, int maxY) {
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    @Override
    public boolean test(ServerLevel level, BlockPos pos) {
        return pos.getY() >= minY && pos.getY() <= maxY;
    }
}
