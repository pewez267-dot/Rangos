package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Catacumbas: piedra deteriorada, ladrillo agrietado, huesos. Zombies y esqueletos. */
public final class CatacombsTheme extends DungeonTheme {

    @Override
    public String id() {
        return "catacombs";
    }

    @Override
    public String displayName() {
        return "Catacumbas";
    }

    @Override
    public BlockState wall() {
        return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState floor() {
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState ceiling() {
        return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState pillar() {
        return Blocks.BONE_BLOCK.defaultBlockState();
    }

    @Override
    public BlockState light() {
        return Blocks.SOUL_LANTERN.defaultBlockState();
    }

    @Override
    public BlockState accent() {
        return Blocks.COBWEB.defaultBlockState();
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return List.of(EntityType.ZOMBIE, EntityType.SKELETON);
    }
}
