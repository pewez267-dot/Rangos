package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Tema personalizado: paleta y mobs definidos libremente por el OP desde el HUD. */
public final class CustomTheme extends DungeonTheme {

    private final BlockState wall;
    private final BlockState floor;
    private final BlockState ceiling;
    private final BlockState pillar;
    private final BlockState light;
    private final BlockState accent;
    private final List<EntityType<?>> mobs;

    public CustomTheme(BlockState wall, BlockState floor, BlockState ceiling, BlockState pillar,
                       BlockState light, BlockState accent, List<EntityType<?>> mobs) {
        this.wall = wall;
        this.floor = floor;
        this.ceiling = ceiling;
        this.pillar = pillar;
        this.light = light;
        this.accent = accent;
        this.mobs = mobs;
    }

    @Override
    public String id() {
        return "custom";
    }

    @Override
    public String displayName() {
        return "Personalizado";
    }

    @Override
    public BlockState wall() {
        return wall;
    }

    @Override
    public BlockState floor() {
        return floor;
    }

    @Override
    public BlockState ceiling() {
        return ceiling;
    }

    @Override
    public BlockState pillar() {
        return pillar;
    }

    @Override
    public BlockState light() {
        return light;
    }

    @Override
    public BlockState accent() {
        return accent;
    }

    @Override
    public List<EntityType<?>> spawnerMobs() {
        return mobs;
    }
}
