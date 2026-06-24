package com.fantasticterraform.intelligent.dungeon.multilevel;

import com.fantasticterraform.editing.Placement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * Pozo vertical con escalera de mano (ladder) que conecta dos niveles. Talla un hueco
 * y coloca escaleras pegadas a una pared de soporte real, de modo que sean trepables.
 */
public final class VerticalShaftBuilder {

    private VerticalShaftBuilder() {
    }

    public static void build(List<Placement> out, int x, int z, int yLow, int yHigh, BlockState wall) {
        int lo = Math.min(yLow, yHigh);
        int hi = Math.max(yLow, yHigh);
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        for (int y = lo; y <= hi; y++) {
            // Pared de soporte al norte y escalera mirando al sur (trepable).
            out.add(Placement.of(new BlockPos(x, y, z - 1), wall));
            out.add(Placement.of(new BlockPos(x, y, z), ladder));
            // Espacio libre frente a la escalera.
            out.add(Placement.of(new BlockPos(x, y, z + 1), Blocks.AIR.defaultBlockState()));
        }
    }
}
