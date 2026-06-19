package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

/** Afecta todo EXCEPTO los tipos de bloque especificados. */
public final class ExclusionMask implements Mask {

    private final Set<Block> excluded;

    public ExclusionMask(Set<Block> excluded) {
        this.excluded = new HashSet<>(excluded);
    }

    @Override
    public boolean test(ServerLevel level, BlockPos pos) {
        return !excluded.contains(level.getBlockState(pos).getBlock());
    }
}
