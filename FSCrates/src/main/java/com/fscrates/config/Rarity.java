package com.fscrates.config;

import net.minecraft.ChatFormatting;

/**
 * The five crate rarities. Each one carries its own visual identity (color,
 * friendly Spanish name and a CustomModelData id so a texture artist can later
 * supply unique models/textures per rarity without code changes).
 */
public enum Rarity {
    COMMON("Comun", ChatFormatting.WHITE, 0x9D9D97, 1),
    RARE("Rara", ChatFormatting.AQUA, 0x55FFFF, 2),
    EPIC("Epica", ChatFormatting.LIGHT_PURPLE, 0xFF55FF, 3),
    LEGENDARY("Legendaria", ChatFormatting.GOLD, 0xFFAA00, 4),
    MYTHIC("Mitica", ChatFormatting.RED, 0xFF5555, 5);

    private final String displayName;
    private final ChatFormatting color;
    private final int rgb;
    private final int modelData;

    Rarity(String displayName, ChatFormatting color, int rgb, int modelData) {
        this.displayName = displayName;
        this.color = color;
        this.rgb = rgb;
        this.modelData = modelData;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    /** Packed RGB used for name colour / particles / beams. */
    public int rgb() {
        return rgb;
    }

    /** CustomModelData value for per-rarity item models. Crate=modelData, Key=modelData+10. */
    public int crateModelData() {
        return modelData;
    }

    public int keyModelData() {
        return modelData + 10;
    }

    public Rarity next() {
        Rarity[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static Rarity byName(String name) {
        if (name == null) {
            return COMMON;
        }
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name)) {
                return r;
            }
        }
        return COMMON;
    }
}
