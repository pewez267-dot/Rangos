package com.fantasticterraform.intelligent.dungeon.traps;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;

/**
 * Foso oculto: una trampilla (trapdoor) cerrada hace de piso sobre un pozo vacio. La
 * placa de presion adyacente alimenta directamente la trampilla (las placas dan senal
 * a los bloques contiguos), abriendola al pisarla y haciendo caer al jugador al pozo.
 */
public final class HiddenPitTrap implements Trap {

    @Override
    public String id() {
        return "pit";
    }

    @Override
    public List<Placement> build(ServerLevel level, BlockPos walkPoint, Direction facing, RandomSource rnd, DungeonTheme theme) {
        List<Placement> out = new ArrayList<>();
        BlockPos plate = walkPoint;
        BlockPos hole = walkPoint.relative(facing);
        out.add(Placement.of(plate, Blocks.STONE_PRESSURE_PLATE.defaultBlockState()));

        // Trampilla cerrada (mitad superior) que actua de piso, adyacente a la placa.
        BlockState trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing.getOpposite())
                .setValue(BlockStateProperties.HALF, Half.TOP)
                .setValue(BlockStateProperties.OPEN, Boolean.FALSE);
        out.add(Placement.of(hole, trapdoor));

        // Pozo vacio debajo de la trampilla.
        for (int d = 1; d <= 4; d++) {
            out.add(Placement.of(hole.below(d), Blocks.AIR.defaultBlockState()));
        }
        return out;
    }
}
