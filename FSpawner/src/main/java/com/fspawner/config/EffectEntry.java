package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;

/**
 * A status effect to apply on the spawned entity. When {@link #permanent} is
 * true the duration is treated as infinite.
 */
public class EffectEntry {

    public String id;          // effect registry id, e.g. "minecraft:strength"
    public int amplifier;      // 0 = level I
    public int duration;       // ticks (ignored when permanent)
    public boolean permanent;
    public boolean ambient;
    public boolean particles;

    public EffectEntry(String id, int amplifier, int duration, boolean permanent, boolean ambient, boolean particles) {
        this.id = id;
        this.amplifier = amplifier;
        this.duration = duration;
        this.permanent = permanent;
        this.ambient = ambient;
        this.particles = particles;
    }

    public EffectEntry(String id) {
        this(id, 0, 600, false, false, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FSKeys.FX_ID, id == null ? "minecraft:strength" : id);
        tag.putInt(FSKeys.FX_AMPLIFIER, amplifier);
        tag.putInt(FSKeys.FX_DURATION, duration);
        tag.putBoolean(FSKeys.FX_PERMANENT, permanent);
        tag.putBoolean(FSKeys.FX_AMBIENT, ambient);
        tag.putBoolean(FSKeys.FX_PARTICLES, particles);
        return tag;
    }

    public static EffectEntry load(CompoundTag tag) {
        return new EffectEntry(
                tag.getString(FSKeys.FX_ID),
                tag.getInt(FSKeys.FX_AMPLIFIER),
                tag.contains(FSKeys.FX_DURATION) ? tag.getInt(FSKeys.FX_DURATION) : 600,
                tag.getBoolean(FSKeys.FX_PERMANENT),
                tag.getBoolean(FSKeys.FX_AMBIENT),
                !tag.contains(FSKeys.FX_PARTICLES) || tag.getBoolean(FSKeys.FX_PARTICLES));
    }

    public EffectEntry copy() {
        return new EffectEntry(id, amplifier, duration, permanent, ambient, particles);
    }
}
