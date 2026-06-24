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
 * Placa de presion + dispensador de flechas. Circuito real: placa -> polvo de redstone
 * -> dispensador adyacente (orientado hacia el jugador). Al pisar la placa, el polvo
 * alimenta el dispensador y dispara una flecha hacia el punto de paso.
 */
public final class PressurePlateArrowTrap implements Trap {

    @Override
    public String id() {
        return "arrow";
    }

    @Override
    public List<Placement> build(ServerLevel level, BlockPos walkPoint, Direction facing, RandomSource rnd, DungeonTheme theme) {
        List<Placement> out = new ArrayList<>();
        BlockPos plate = walkPoint;
        BlockPos dust = walkPoint.relative(facing);
        BlockPos disp = walkPoint.relative(facing, 2);
        out.add(Placement.of(plate, Blocks.STONE_PRESSURE_PLATE.defaultBlockState()));
        out.add(Placement.of(dust, Blocks.REDSTONE_WIRE.defaultBlockState()));
        BlockState dispenser = Blocks.DISPENSER.defaultBlockState()
                .setValue(BlockStateProperties.FACING, facing.getOpposite());
        out.add(new Placement(disp, dispenser, RedstoneCircuitBuilder.dispenserItems("minecraft:arrow", 64)));
        return out;
    }
}
