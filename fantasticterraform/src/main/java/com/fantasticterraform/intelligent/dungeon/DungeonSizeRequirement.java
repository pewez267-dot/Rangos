package com.fantasticterraform.intelligent.dungeon;

import com.fantasticterraform.config.TerraformConfig;

/**
 * Requisitos minimos de tamano para generar una dungeon de un tier dado. Se leen de
 * {@code config.toml} (seccion intelligent_generation.dungeon.tiers).
 */
public final class DungeonSizeRequirement {

    public final DungeonTier tier;
    public final long minVolume;
    public final int minWidth;
    public final int minHeight;
    public final int minLength;
    public final int minRooms;
    public final int maxRooms;

    private DungeonSizeRequirement(DungeonTier tier, long minVolume, int minWidth, int minHeight, int minLength,
                                   int minRooms, int maxRooms) {
        this.tier = tier;
        this.minVolume = minVolume;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.minLength = minLength;
        this.minRooms = minRooms;
        this.maxRooms = maxRooms;
    }

    public static DungeonSizeRequirement forTier(DungeonTier tier) {
        int i = tier.configIndex();
        TerraformConfig.General c = TerraformConfig.GENERAL;
        return new DungeonSizeRequirement(tier,
                c.tierMinVolume[i].get(),
                c.tierMinWidth[i].get(),
                c.tierMinHeight[i].get(),
                c.tierMinLength[i].get(),
                c.tierMinRooms[i].get(),
                c.tierMaxRooms[i].get());
    }
}
