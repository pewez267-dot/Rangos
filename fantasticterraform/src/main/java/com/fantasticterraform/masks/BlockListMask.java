package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

/** Solo afecta los tipos de bloque presentes en la lista. */
public final class BlockListMask implements Mask {

    private final Set<Block> blocks;

    public BlockListMask(Set<Block> blocks) {
        this.blocks = new HashSet<>(blocks);
    }

    @Override
    public boolean test(ServerLevel level, BlockPos pos) {
        return blocks.contains(level.getBlockState(pos).getBlock());
    }
}
