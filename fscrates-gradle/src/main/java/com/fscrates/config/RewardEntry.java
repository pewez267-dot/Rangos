// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.config;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class RewardEntry
{
    public Type type;
    public double chance;
    public int minAmount;
    public int maxAmount;
    public ItemStack item;
    public String keyRarity;
    public String command;
    public int xp;
    public String effectId;
    public int effectDuration;
    public int effectAmplifier;
    public boolean guaranteed;
    public String label;
    
    public RewardEntry() {
        this.type = Type.ITEM;
        this.chance = 10.0;
        this.minAmount = 1;
        this.maxAmount = 1;
        this.item = ItemStack.EMPTY;
        this.keyRarity = "COMMON";
        this.command = "";
        this.xp = 0;
        this.effectId = "minecraft:luck";
        this.effectDuration = 600;
        this.effectAmplifier = 0;
        this.guaranteed = false;
        this.label = "Recompensa";
    }
    
    public RewardEntry(final Type type) {
        this.type = Type.ITEM;
        this.chance = 10.0;
        this.minAmount = 1;
        this.maxAmount = 1;
        this.item = ItemStack.EMPTY;
        this.keyRarity = "COMMON";
        this.command = "";
        this.xp = 0;
        this.effectId = "minecraft:luck";
        this.effectDuration = 600;
        this.effectAmplifier = 0;
        this.guaranteed = false;
        this.label = "Recompensa";
        this.type = type;
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("type", this.type.name());
        tag.putDouble("chance", this.chance);
        tag.putInt("min", this.minAmount);
        tag.putInt("max", this.maxAmount);
        tag.putBoolean("guaranteed", this.guaranteed);
        tag.putString("label", this.label);
        final CompoundTag itemTag = new CompoundTag();
        if (this.item != null && !this.item.isEmpty()) {
            this.item.save(itemTag);
        }
        tag.put("item", (Tag)itemTag);
        tag.putString("keyRarity", this.keyRarity);
        tag.putString("command", this.command);
        tag.putInt("xp", this.xp);
        tag.putString("effectId", this.effectId);
        tag.putInt("effectDuration", this.effectDuration);
        tag.putInt("effectAmplifier", this.effectAmplifier);
        return tag;
    }
    
    public static RewardEntry load(final CompoundTag tag) {
        final RewardEntry r = new RewardEntry();
        try {
            r.type = Type.valueOf(tag.getString("type"));
        }
        catch (final IllegalArgumentException ignored) {
            r.type = Type.ITEM;
        }
        if (tag.contains("chance")) {
            r.chance = tag.getDouble("chance");
        }
        else if (tag.contains("weight")) {
            r.chance = tag.getInt("weight");
        }
        else {
            r.chance = 10.0;
        }
        r.minAmount = (tag.contains("min") ? tag.getInt("min") : 1);
        r.maxAmount = (tag.contains("max") ? tag.getInt("max") : 1);
        r.guaranteed = tag.getBoolean("guaranteed");
        r.label = (tag.contains("label") ? tag.getString("label") : "Recompensa");
        if (tag.contains("item")) {
            final CompoundTag itemTag = tag.getCompound("item");
            r.item = (itemTag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(itemTag));
        }
        r.keyRarity = (tag.contains("keyRarity") ? tag.getString("keyRarity") : "COMMON");
        r.command = tag.getString("command");
        r.xp = tag.getInt("xp");
        r.effectId = (tag.contains("effectId") ? tag.getString("effectId") : "minecraft:luck");
        r.effectDuration = (tag.contains("effectDuration") ? tag.getInt("effectDuration") : 600);
        r.effectAmplifier = tag.getInt("effectAmplifier");
        return r;
    }
    
    public String describe() {
        return switch (this.type) {
            default -> throw new IncompatibleClassChangeError();
            case ITEM -> this.item.isEmpty() ? "(item vacio)" : this.item.getHoverName().getString();
            case COMMAND -> "Comando: " + (this.command.isEmpty() ? "(vacio)" : this.command);
            case XP -> "XP: " + this.xp;
            case EFFECT -> "Efecto: " + this.effectId;
            case KEY -> "Llave " + this.keyRarity;
        };
    }
    
    public RewardEntry copy() {
        return load(this.save());
    }
    
    public enum Type
    {
        ITEM, 
        COMMAND, 
        XP, 
        EFFECT, 
        KEY;
    }
}
