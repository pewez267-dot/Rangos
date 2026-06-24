package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Guarida Elfica/Mistica: madera clara, hojas, piedra musgosa, luz vegetal. */
public final class MysticElvenLairTheme extends DungeonTheme {

    @Override
    public String id() {
        return "mystic_elven";
    }

    @Override
    public String displayName() {
        return "Guarida Elfica/Mistica";
    }

    @Override
    public BlockState wall() {
        return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    }

    @Override
    public BlockState floor() {
        return Blocks.BIRCH_PLANKS.defaultBlockState();
    }

    @Override
    public BlockState ceiling() {
        return Blocks.OAK_LEAVES.defaultBlockState();
    }

    @Override
    public BlockState pillar() {
        return Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
    }

    @Override
    public BlockState light() {
        return Blocks.GLOWSTONE.defaultBlockState();
    }

    @Override
    public BlockState accent() {
        return Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return List.of(EntityType.WITCH, EntityType.SLIME);
    }
}
