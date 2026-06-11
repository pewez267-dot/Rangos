package com.fscrates.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * A single possible crate reward. Supports several reward types so a crate can
 * give items (with full NBT), run commands, grant XP, apply effects or hand out
 * tier keys.
 *
 * <p>Selection is driven by {@link #chance}, a <b>percentage</b> (0-100) exactly
 * like a vanilla spawner / loot weight expressed as a percent. The GUI lets the
 * admin type the percentage directly; the loot engine normalises all chances so
 * they always add up to 100% across one roll.
 */
public class RewardEntry {

    public enum Type {
        ITEM,       // give an ItemStack (full NBT preserved)
        COMMAND,    // run a server command ({player} placeholder supported)
        XP,         // grant experience points
        EFFECT,     // apply a mob effect
        KEY         // give a tier key
    }

    public Type type = Type.ITEM;

    /** Probability in percent (0-100). Normalised against the other rewards. */
    public double chance = 10.0;
    public int minAmount = 1;
    public int maxAmount = 1;

    // ITEM / KEY
    public ItemStack item = ItemStack.EMPTY;
    public String keyRarity = "COMMON"; // for KEY type

    // COMMAND
    public String command = "";

    // XP
    public int xp = 0;

    // EFFECT
    public String effectId = "minecraft:luck";
    public int effectDuration = 600; // ticks
    public int effectAmplifier = 0;

    /** Guaranteed rewards are always given regardless of chance. */
    public boolean guaranteed = false;

    /** Display label shown in the GUI probability bars. */
    public String label = "Recompensa";

    public RewardEntry() {}

    public RewardEntry(Type type) {
        this.type = type;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putDouble("chance", chance);
        tag.putInt("min", minAmount);
        tag.putInt("max", maxAmount);
        tag.putBoolean("guaranteed", guaranteed);
        tag.putString("label", label);

        CompoundTag itemTag = new CompoundTag();
        if (item != null && !item.isEmpty()) {
            item.save(itemTag);
        }
        tag.put("item", itemTag);
        tag.putString("keyRarity", keyRarity);
        tag.putString("command", command);
        tag.putInt("xp", xp);
        tag.putString("effectId", effectId);
        tag.putInt("effectDuration", effectDuration);
        tag.putInt("effectAmplifier", effectAmplifier);
        return tag;
    }

    public static RewardEntry load(CompoundTag tag) {
        RewardEntry r = new RewardEntry();
        try {
            r.type = Type.valueOf(tag.getString("type"));
        } catch (IllegalArgumentException ignored) {
            r.type = Type.ITEM;
        }
        // back-compat: old configs stored an int "weight"
        if (tag.contains("chance")) {
            r.chance = tag.getDouble("chance");
        } else if (tag.contains("weight")) {
            r.chance = tag.getInt("weight");
        } else {
            r.chance = 10.0;
        }
        r.minAmount = tag.contains("min") ? tag.getInt("min") : 1;
        r.maxAmount = tag.contains("max") ? tag.getInt("max") : 1;
        r.guaranteed = tag.getBoolean("guaranteed");
        r.label = tag.contains("label") ? tag.getString("label") : "Recompensa";

        if (tag.contains("item")) {
            CompoundTag itemTag = tag.getCompound("item");
            r.item = itemTag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(itemTag);
        }
        r.keyRarity = tag.contains("keyRarity") ? tag.getString("keyRarity") : "COMMON";
        r.command = tag.getString("command");
        r.xp = tag.getInt("xp");
        r.effectId = tag.contains("effectId") ? tag.getString("effectId") : "minecraft:luck";
        r.effectDuration = tag.contains("effectDuration") ? tag.getInt("effectDuration") : 600;
        r.effectAmplifier = tag.getInt("effectAmplifier");
        return r;
    }

    /** A short human label for the GUI/probability bars. */
    public String describe() {
        return switch (type) {
            case ITEM -> item.isEmpty() ? "(item vacio)" : item.getHoverName().getString();
            case COMMAND -> "Comando: " + (command.isEmpty() ? "(vacio)" : command);
            case XP -> "XP: " + xp;
            case EFFECT -> "Efecto: " + effectId;
            case KEY -> "Llave " + keyRarity;
        };
    }

    public RewardEntry copy() {
        return load(this.save());
    }
}
