package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Cripta Antigua: piedra lisa, cuarzo oscuro, oro decorativo. Esqueletos/wither. */
public final class AncientCryptTheme extends DungeonTheme {

    @Override
    public String id() {
        return "ancient_crypt";
    }

    @Override
    public String displayName() {
        return "Cripta Antigua";
    }

    @Override
    public BlockState wall() {
        return Blocks.SMOOTH_STONE.defaultBlockState();
    }

    @Override
    public BlockState floor() {
        return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    @Override
    public BlockState ceiling() {
        return Blocks.SMOOTH_STONE.defaultBlockState();
    }

    @Override
    public BlockState pillar() {
        return Blocks.QUARTZ_PILLAR.defaultBlockState();
    }

    @Override
    public BlockState light() {
        return Blocks.SEA_LANTERN.defaultBlockState();
    }

    @Override
    public BlockState accent() {
        return Blocks.GOLD_BLOCK.defaultBlockState();
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return List.of(EntityType.SKELETON, EntityType.WITHER_SKELETON);
    }
}
