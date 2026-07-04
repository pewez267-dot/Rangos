package com.fscrates.config;

import net.minecraft.ChatFormatting;

public enum Rarity {
    COMMON("common", "Com\u00fan", ChatFormatting.WHITE, 13685464),
    RARE("rare", "Rara", ChatFormatting.AQUA, 5630965),
    EPIC("epic", "\u00c9pica", ChatFormatting.LIGHT_PURPLE, 14446320),
    LEGENDARY("legendary", "Legendaria", ChatFormatting.GOLD, 16757288),
    MYTHIC("mythic", "M\u00edtica", ChatFormatting.RED, 0xFF5050);

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final int rgb;

    private Rarity(String id, String displayName, ChatFormatting color, int rgb) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.rgb = rgb;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public ChatFormatting color() {
        return this.color;
    }

    public int rgb() {
        return this.rgb;
    }

    public float redF() {
        return (float)(this.rgb >> 16 & 0xFF) / 255.0f;
    }

    public float greenF() {
        return (float)(this.rgb >> 8 & 0xFF) / 255.0f;
    }

    public float blueF() {
        return (float)(this.rgb & 0xFF) / 255.0f;
    }

    public Rarity next() {
        Rarity[] v = Rarity.values();
        return v[(this.ordinal() + 1) % v.length];
    }

    public float sizeScale() {
        switch (this) {
            case LEGENDARY: {
                return 2.85f;
            }
            case MYTHIC: {
                return 2.3f;
            }
            case EPIC: {
                return 1.3f;
            }
        }
        return 1.0f;
    }

    public static Rarity byName(String name) {
        if (name == null) {
            return COMMON;
        }
        for (Rarity r : Rarity.values()) {
            if (!r.name().equalsIgnoreCase(name) && !r.id.equalsIgnoreCase(name)) continue;
            return r;
        }
        return COMMON;
    }
}

