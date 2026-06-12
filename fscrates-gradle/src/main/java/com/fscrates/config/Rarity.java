// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.config;

import net.minecraft.ChatFormatting;

public enum Rarity
{
    COMMON("common", "Comun", ChatFormatting.WHITE, 13685464), 
    RARE("rare", "Rara", ChatFormatting.AQUA, 5630965), 
    EPIC("epic", "Epica", ChatFormatting.LIGHT_PURPLE, 14446320), 
    LEGENDARY("legendary", "Legendaria", ChatFormatting.GOLD, 16757288), 
    MYTHIC("mythic", "Mitica", ChatFormatting.RED, 16732240);
    
    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final int rgb;
    
    private Rarity(final String id, final String displayName, final ChatFormatting color, final int rgb) {
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
        return (this.rgb >> 16 & 0xFF) / 255.0f;
    }
    
    public float greenF() {
        return (this.rgb >> 8 & 0xFF) / 255.0f;
    }
    
    public float blueF() {
        return (this.rgb & 0xFF) / 255.0f;
    }
    
    public Rarity next() {
        final Rarity[] v = values();
        return v[(this.ordinal() + 1) % v.length];
    }
    
    public static Rarity byName(final String name) {
        if (name == null) {
            return Rarity.COMMON;
        }
        for (final Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name) || r.id.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return Rarity.COMMON;
    }
}
