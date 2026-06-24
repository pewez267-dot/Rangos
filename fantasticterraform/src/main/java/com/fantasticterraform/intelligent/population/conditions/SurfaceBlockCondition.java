package com.fantasticterraform.intelligent.population.conditions;

import com.fantasticterraform.intelligent.population.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** El bloque de superficie en la columna debe ser el especificado. */
public final class SurfaceBlockCondition implements Condition {

    private final Block block;

    public SurfaceBlockCondition(Block block) {
        this.block = block;
    }

    @Override
    public boolean evaluate(BlockPos surfacePos, ServerLevel level, double slope, double height, double moisture, double temperature) {
        return level.getBlockState(surfacePos).is(block);
    }
}
