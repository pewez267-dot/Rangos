package com.fscrates.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * A single possible crate reward. Supports several reward types so a crate can
 * give items (with full NBT), run commands, grant XP, apply effects or hand out
 * keys. Selection uses {@link #weight} (relative chance).
 */
public class RewardEntry {

    public enum Type {
        ITEM,       // give an ItemStack (full NBT preserved)
        COMMAND,    // run a server command ({player} placeholder supported)
        XP,         // grant experience points
        EFFECT,     // apply a mob effect
        KEY         // give another crate key (admin reward)
    }

    public Type type = Type.ITEM;

    /** weight relative to all rewards in the crate; higher = more likely. */
    public int weight = 10;
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

    /** Optional flag: guaranteed rewards are always given regardless of weight. */
    public boolean guaranteed = false;

    /** Display label shown in the GUI rarity bars. */
    public String label = "Recompensa";

    public RewardEntry() {}

    public RewardEntry(Type type) {
        this.type = type;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putInt("weight", weight);
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
        r.weight = tag.contains("weight") ? tag.getInt("weight") : 10;
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
            case ITEM -> item.isEmpty() ? "(item vac\u00edo)" : item.getHoverName().getString();
            case COMMAND -> "Comando: " + (command.isEmpty() ? "(vac\u00edo)" : command);
            case XP -> "XP: " + xp;
            case EFFECT -> "Efecto: " + effectId;
            case KEY -> "Llave " + keyRarity;
        };
    }

    public RewardEntry copy() {
        return load(this.save());
    }
}
