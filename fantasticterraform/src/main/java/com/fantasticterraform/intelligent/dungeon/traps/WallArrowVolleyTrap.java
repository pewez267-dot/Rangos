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

import java.util.ArrayList;
import java.util.List;

/**
 * Descarga de flechas de pared: tres dispensadores en linea activados por una fila de
 * placas, con un repetidor en el carril central para introducir un retardo (secuencia).
 * Cada carril es un circuito real placa -> (repetidor/polvo) -> dispensador.
 */
public final class WallArrowVolleyTrap implements Trap {

    @Override
    public String id() {
        return "volley";
    }

    @Override
    public List<Placement> build(ServerLevel level, BlockPos walkPoint, Direction facing, RandomSource rnd, DungeonTheme theme) {
        List<Placement> out = new ArrayList<>();
        Direction perp = facing.getClockWise();
        for (int k = -1; k <= 1; k++) {
            BlockPos base = walkPoint.relative(perp, k);
            BlockPos plate = base;
            BlockPos mid = base.relative(facing);
            BlockPos disp = base.relative(facing, 2);
            out.add(Placement.of(plate, Blocks.STONE_PRESSURE_PLATE.defaultBlockState()));
            if (k == 0) {
                BlockState repeater = Blocks.REPEATER.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.DELAY, 2);
                out.add(Placement.of(mid, repeater));
            } else {
                out.add(Placement.of(mid, Blocks.REDSTONE_WIRE.defaultBlockState()));
            }
            BlockState dispenser = Blocks.DISPENSER.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, facing.getOpposite());
            out.add(new Placement(disp, dispenser, RedstoneCircuitBuilder.dispenserItems("minecraft:arrow", 64)));
        }
        return out;
    }
}
