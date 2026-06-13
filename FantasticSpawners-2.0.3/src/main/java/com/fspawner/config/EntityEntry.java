// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;

public class EntityEntry
{
    public String id;
    public int weight;
    
    public EntityEntry(final String id, final int weight) {
        this.id = id;
        this.weight = Math.max(1, weight);
    }
    
    public EntityEntry(final String id) {
        this(id, 1);
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", (this.id == null) ? "minecraft:pig" : this.id);
        tag.putInt("weight", this.weight);
        return tag;
    }
    
    public static EntityEntry load(final CompoundTag tag) {
        return new EntityEntry(tag.getString("id"), tag.contains("weight") ? tag.getInt("weight") : 1);
    }
    
    public EntityEntry copy() {
        return new EntityEntry(this.id, this.weight);
    }
}
