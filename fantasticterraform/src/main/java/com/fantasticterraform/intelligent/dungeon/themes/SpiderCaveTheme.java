package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Cueva de Aracnidos: piedra natural, telaranas funcionales, musgo. Aranas. */
public final class SpiderCaveTheme extends DungeonTheme {

    @Override
    public String id() {
        return "spider_cave";
    }

    @Override
    public String displayName() {
        return "Cueva de Aracnidos";
    }

    @Override
    public BlockState wall() {
        return Blocks.STONE.defaultBlockState();
    }

    @Override
    public BlockState floor() {
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    @Override
    public BlockState ceiling() {
        return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    }

    @Override
    public BlockState pillar() {
        return Blocks.MOSS_BLOCK.defaultBlockState();
    }

    @Override
    public BlockState light() {
        return Blocks.GLOWSTONE.defaultBlockState();
    }

    @Override
    public BlockState accent() {
        return Blocks.COBWEB.defaultBlockState();
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return List.of(EntityType.SPIDER, EntityType.CAVE_SPIDER);
    }
}
