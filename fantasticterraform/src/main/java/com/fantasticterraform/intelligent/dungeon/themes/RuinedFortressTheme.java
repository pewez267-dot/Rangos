package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Fortaleza en Ruinas: piedra, piedra musgosa, hierro decorativo. Esqueletos. */
public final class RuinedFortressTheme extends DungeonTheme {

    @Override
    public String id() {
        return "ruined_fortress";
    }

    @Override
    public String displayName() {
        return "Fortaleza en Ruinas";
    }

    @Override
    public BlockState wall() {
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState floor() {
        return Blocks.STONE.defaultBlockState();
    }

    @Override
    public BlockState ceiling() {
        return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState pillar() {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public BlockState light() {
        return Blocks.LANTERN.defaultBlockState();
    }

    @Override
    public BlockState accent() {
        return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return List.of(EntityType.SKELETON, EntityType.ZOMBIE);
    }
}
