package com.fantasticterraform.masks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** Solo afecta el tipo de bloque exacto especificado. */
public final class BlockMask implements Mask {

    private final Block block;

    public BlockMask(Block block) {
        this.block = block;
    }

    @Override
    public boolean test(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(block);
    }
}
