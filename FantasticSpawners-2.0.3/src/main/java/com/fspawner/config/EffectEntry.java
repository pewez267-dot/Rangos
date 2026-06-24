// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;

public class EffectEntry
{
    public String id;
    public int amplifier;
    public int duration;
    public boolean permanent;
    public boolean ambient;
    public boolean particles;
    
    public EffectEntry(final String id, final int amplifier, final int duration, final boolean permanent, final boolean ambient, final boolean particles) {
        this.id = id;
        this.amplifier = amplifier;
        this.duration = duration;
        this.permanent = permanent;
        this.ambient = ambient;
        this.particles = particles;
    }
    
    public EffectEntry(final String id) {
        this(id, 0, 600, false, false, true);
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", (this.id == null) ? "minecraft:strength" : this.id);
        tag.putInt("amplifier", this.amplifier);
        tag.putInt("duration", this.duration);
        tag.putBoolean("permanent", this.permanent);
        tag.putBoolean("ambient", this.ambient);
        tag.putBoolean("particles", this.particles);
        return tag;
    }
    
    public static EffectEntry load(final CompoundTag tag) {
        return new EffectEntry(tag.getString("id"), tag.getInt("amplifier"), tag.contains("duration") ? tag.getInt("duration") : 600, tag.getBoolean("permanent"), tag.getBoolean("ambient"), !tag.contains("particles") || tag.getBoolean("particles"));
    }
    
    public EffectEntry copy() {
        return new EffectEntry(this.id, this.amplifier, this.duration, this.permanent, this.ambient, this.particles);
    }
}
