package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Castillo Abandonado: piedra tallada, ladrillo, madera oscura. Variado. */
public final class AbandonedCastleTheme extends DungeonTheme {

    @Override
    public String id() {
        return "abandoned_castle";
    }

    @Override
    public String displayName() {
        return "Castillo Abandonado";
    }

    @Override
    public BlockState wall() {
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState floor() {
        return Blocks.DARK_OAK_PLANKS.defaultBlockState();
    }

    @Override
    public BlockState ceiling() {
        return Blocks.DARK_OAK_PLANKS.defaultBlockState();
    }

    @Override
    public BlockState pillar() {
        return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState light() {
        return Blocks.LANTERN.defaultBlockState();
    }

    @Override
    public BlockState accent() {
        return Blocks.DARK_OAK_LOG.defaultBlockState();
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER);
    }
}
