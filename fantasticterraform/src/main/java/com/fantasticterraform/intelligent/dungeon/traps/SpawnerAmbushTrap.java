package com.fantasticterraform.intelligent.dungeon.traps;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Emboscada con spawner: un spawner del mob del tema queda oculto cerca del punto, y un
 * comparador lee un cofre adyacente alimentando una lampara de redstone (circuito real
 * de deteccion). El spawner, por proximidad, lanza la emboscada al acercarse al loot.
 */
public final class SpawnerAmbushTrap implements Trap {

    @Override
    public String id() {
        return "spawner";
    }

    @Override
    public List<Placement> build(ServerLevel level, BlockPos walkPoint, Direction facing, RandomSource rnd, DungeonTheme theme) {
        List<Placement> out = new ArrayList<>();

        // Spawner del mob del tema, oculto en el techo del punto.
        EntityType<?> mob = theme.spawnerMobs().isEmpty() ? EntityType.ZOMBIE : theme.spawnerMobs().get(0);
        String mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob) == null
                ? "minecraft:zombie" : ForgeRegistries.ENTITY_TYPES.getKey(mob).toString();
        out.add(new Placement(walkPoint.above(3), Blocks.SPAWNER.defaultBlockState(),
                RedstoneCircuitBuilder.spawnerData(mobId)));

        // Cofre cebo + comparador que lo lee + lampara (circuito de deteccion real).
        BlockPos chest = walkPoint;
        out.add(Placement.of(chest, Blocks.CHEST.defaultBlockState()));
        BlockPos comparatorPos = walkPoint.relative(facing.getOpposite());
        BlockState comparator = Blocks.COMPARATOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing.getOpposite());
        out.add(Placement.of(comparatorPos, comparator));
        out.add(Placement.of(walkPoint.relative(facing.getOpposite(), 2), Blocks.REDSTONE_LAMP.defaultBlockState()));
        return out;
    }
}
