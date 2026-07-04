package com.fscrates.config;

import com.fscrates.config.Rarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class RewardEntry {
    public Type type = Type.ITEM;
    public double chance = 10.0;
    public int minAmount = 1;
    public int maxAmount = 1;
    public ItemStack item = ItemStack.EMPTY;
    public String keyRarity = "COMMON";
    public String command = "";
    public int xp = 0;
    public String effectId = "minecraft:luck";
    public int effectDuration = 600;
    public int effectAmplifier = 0;
    public boolean guaranteed = false;
    public String label = "Recompensa";
    public String rarity = "";

    public RewardEntry() {
    }

    public RewardEntry(Type type) {
        this.type = type;
    }

    public Rarity effectiveRarity(Rarity fallback) {
        if (this.rarity != null && !this.rarity.isBlank()) {
            return Rarity.byName(this.rarity);
        }
        if (this.type == Type.KEY) {
            return Rarity.byName(this.keyRarity);
        }
        return fallback == null ? Rarity.COMMON : fallback;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", this.type.name());
        tag.putDouble("chance", this.chance);
        tag.putInt("min", this.minAmount);
        tag.putInt("max", this.maxAmount);
        tag.putBoolean("guaranteed", this.guaranteed);
        tag.putString("label", this.label);
        tag.putString("itemRarity", this.rarity == null ? "" : this.rarity);
        CompoundTag itemTag = new CompoundTag();
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

    public static RewardEntry load(CompoundTag tag) {
        RewardEntry r = new RewardEntry();
        try {
            r.type = Type.valueOf(tag.getString("type"));
        }
        catch (IllegalArgumentException var3) {
            r.type = Type.ITEM;
        }
        r.chance = tag.contains("chance") ? tag.getDouble("chance") : (tag.contains("weight") ? (double)tag.getInt("weight") : 10.0);
        r.minAmount = tag.contains("min") ? tag.getInt("min") : 1;
        r.maxAmount = tag.contains("max") ? tag.getInt("max") : 1;
        r.guaranteed = tag.getBoolean("guaranteed");
        r.label = tag.contains("label") ? tag.getString("label") : "Recompensa";
        String string = r.rarity = tag.contains("itemRarity") ? tag.getString("itemRarity") : "";
        if (tag.contains("item")) {
            CompoundTag itemTag = tag.getCompound("item");
            r.item = itemTag.isEmpty() ? ItemStack.EMPTY : ItemStack.of((CompoundTag)itemTag);
        }
        r.keyRarity = tag.contains("keyRarity") ? tag.getString("keyRarity") : "COMMON";
        r.command = tag.getString("command");
        r.xp = tag.getInt("xp");
        r.effectId = tag.contains("effectId") ? tag.getString("effectId") : "minecraft:luck";
        r.effectDuration = tag.contains("effectDuration") ? tag.getInt("effectDuration") : 600;
        r.effectAmplifier = tag.getInt("effectAmplifier");
        return r;
    }

    public String describe() {
        return switch (this.type) {
            default -> throw new IncompatibleClassChangeError();
            case ITEM -> {
                if (this.item.isEmpty()) {
                    yield "(item vac\u00edo)";
                }
                yield this.item.getHoverName().getString();
            }
            case COMMAND -> "Comando: " + (this.command.isEmpty() ? "(vac\u00edo)" : this.command);
            case XP -> "XP: " + this.xp;
            case EFFECT -> "Efecto: " + this.effectId;
            case KEY -> "Fantastic Key";
        };
    }

    public RewardEntry copy() {
        return RewardEntry.load(this.save());
    }

    public static enum Type {
        ITEM,
        COMMAND,
        XP,
        EFFECT,
        KEY;

    }
}

