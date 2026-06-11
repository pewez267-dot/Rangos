package com.fscrates.config;

import net.minecraft.ChatFormatting;

/**
 * The five crate tiers. Each tier owns a colour, a friendly Spanish name and a
 * registry/texture id ({@code common}, {@code rare}, ...). Keys are now bound to
 * the TIER (not to an individual crate): a crate of a given tier is opened by
 * any key of the same tier. There is exactly one key item per tier.
 */
public enum Rarity {
    COMMON("common", "Comun", ChatFormatting.WHITE, 0xD0D2D8),
    RARE("rare", "Rara", ChatFormatting.AQUA, 0x55EBF5),
    EPIC("epic", "Epica", ChatFormatting.LIGHT_PURPLE, 0xDC6EF0),
    LEGENDARY("legendary", "Legendaria", ChatFormatting.GOLD, 0xFFB228),
    MYTHIC("mythic", "Mitica", ChatFormatting.RED, 0xFF5050);

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final int rgb;

    Rarity(String id, String displayName, ChatFormatting color, int rgb) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.rgb = rgb;
    }

    /** Lowercase id used for item ids and texture file names (e.g. "legendary"). */
    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    /** Packed RGB used for name colour, particles, beams and chest tint. */
    public int rgb() {
        return rgb;
    }

    public float redF() {
        return ((rgb >> 16) & 0xFF) / 255f;
    }

    public float greenF() {
        return ((rgb >> 8) & 0xFF) / 255f;
    }

    public float blueF() {
        return (rgb & 0xFF) / 255f;
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
            if (r.name().equalsIgnoreCase(name) || r.id.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return COMMON;
    }
}
