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
        this.item = ItemStack.f_41583_;
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
        this.item = ItemStack.f_41583_;
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
        tag.m_128359_("type", this.type.name());
        tag.m_128347_("chance", this.chance);
        tag.m_128405_("min", this.minAmount);
        tag.m_128405_("max", this.maxAmount);
        tag.m_128379_("guaranteed", this.guaranteed);
        tag.m_128359_("label", this.label);
        final CompoundTag itemTag = new CompoundTag();
        if (this.item != null && !this.item.m_41619_()) {
            this.item.m_41739_(itemTag);
        }
        tag.m_128365_("item", (Tag)itemTag);
        tag.m_128359_("keyRarity", this.keyRarity);
        tag.m_128359_("command", this.command);
        tag.m_128405_("xp", this.xp);
        tag.m_128359_("effectId", this.effectId);
        tag.m_128405_("effectDuration", this.effectDuration);
        tag.m_128405_("effectAmplifier", this.effectAmplifier);
        return tag;
    }
    
    public static RewardEntry load(final CompoundTag tag) {
        final RewardEntry r = new RewardEntry();
        try {
            r.type = Type.valueOf(tag.m_128461_("type"));
        }
        catch (final IllegalArgumentException ignored) {
            r.type = Type.ITEM;
        }
        if (tag.m_128441_("chance")) {
            r.chance = tag.m_128459_("chance");
        }
        else if (tag.m_128441_("weight")) {
            r.chance = tag.m_128451_("weight");
        }
        else {
            r.chance = 10.0;
        }
        r.minAmount = (tag.m_128441_("min") ? tag.m_128451_("min") : 1);
        r.maxAmount = (tag.m_128441_("max") ? tag.m_128451_("max") : 1);
        r.guaranteed = tag.m_128471_("guaranteed");
        r.label = (tag.m_128441_("label") ? tag.m_128461_("label") : "Recompensa");
        if (tag.m_128441_("item")) {
            final CompoundTag itemTag = tag.m_128469_("item");
            r.item = (itemTag.m_128456_() ? ItemStack.f_41583_ : ItemStack.m_41712_(itemTag));
        }
        r.keyRarity = (tag.m_128441_("keyRarity") ? tag.m_128461_("keyRarity") : "COMMON");
        r.command = tag.m_128461_("command");
        r.xp = tag.m_128451_("xp");
        r.effectId = (tag.m_128441_("effectId") ? tag.m_128461_("effectId") : "minecraft:luck");
        r.effectDuration = (tag.m_128441_("effectDuration") ? tag.m_128451_("effectDuration") : 600);
        r.effectAmplifier = tag.m_128451_("effectAmplifier");
        return r;
    }
    
    public String describe() {
        return switch (this.type) {
            default -> throw new IncompatibleClassChangeError();
            case ITEM -> this.item.m_41619_() ? "(item vacio)" : this.item.m_41786_().getString();
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
