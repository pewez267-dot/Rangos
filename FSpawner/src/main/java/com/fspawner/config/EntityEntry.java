package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;

/** A single entity option with a spawn weight (used by the random pool mode). */
public class EntityEntry {

    public String id;
    public int weight;

    public EntityEntry(String id, int weight) {
        this.id = id;
        this.weight = Math.max(1, weight);
    }

    public EntityEntry(String id) {
        this(id, 1);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FSKeys.ENTITY_ID, id == null ? "minecraft:pig" : id);
        tag.putInt(FSKeys.ENTITY_WEIGHT, weight);
        return tag;
    }

    public static EntityEntry load(CompoundTag tag) {
        return new EntityEntry(
                tag.getString(FSKeys.ENTITY_ID),
                tag.contains(FSKeys.ENTITY_WEIGHT) ? tag.getInt(FSKeys.ENTITY_WEIGHT) : 1);
    }

    public EntityEntry copy() {
        return new EntityEntry(id, weight);
    }
}
