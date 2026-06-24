// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.config;

import java.util.Iterator;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import java.util.Collections;
import java.util.Random;
import java.util.Collection;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.List;

public class InfernalConfig
{
    public Mode mode;
    public List<String> mods;
    public List<String> pool;
    public int min;
    public int max;
    
    public InfernalConfig() {
        this.mode = Mode.DISABLED;
        this.mods = new ArrayList<String>();
        this.pool = new ArrayList<String>();
        this.min = 2;
        this.max = 5;
    }
    
    public boolean isEnabled() {
        return this.mode != Mode.DISABLED;
    }
    
    public String resolveModifierString(final RandomSource random) {
        switch (this.mode) {
            case ALWAYS:
            case CUSTOM: {
                return String.join(" ", this.mods);
            }
            case RANDOM: {
                if (this.pool.isEmpty()) {
                    return "";
                }
                final int lo = Math.max(0, Math.min(this.min, this.max));
                final int hi = Math.max(this.min, this.max);
                int count = lo + ((hi > lo) ? random.nextInt(hi - lo + 1) : 0);
                count = Math.min(count, this.pool.size());
                final List<String> shuffled = new ArrayList<String>(this.pool);
                Collections.shuffle(shuffled, new Random(random.nextLong()));
                return String.join(" ", shuffled.subList(0, count));
            }
            default: {
                return "";
            }
        }
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("mode", this.mode.name());
        tag.putInt("min", this.min);
        tag.putInt("max", this.max);
        tag.put("mods", (Tag)toList(this.mods));
        tag.put("pool", (Tag)toList(this.pool));
        return tag;
    }
    
    public static InfernalConfig load(final CompoundTag tag) {
        final InfernalConfig cfg = new InfernalConfig();
        try {
            cfg.mode = Mode.valueOf(tag.getString("mode"));
        }
        catch (final IllegalArgumentException ignored) {
            cfg.mode = Mode.DISABLED;
        }
        cfg.min = (tag.contains("min") ? tag.getInt("min") : 2);
        cfg.max = (tag.contains("max") ? tag.getInt("max") : 5);
        cfg.mods = fromList(tag.getList("mods", 8));
        cfg.pool = fromList(tag.getList("pool", 8));
        return cfg;
    }
    
    private static ListTag toList(final List<String> values) {
        final ListTag list = new ListTag();
        for (final String v : values) {
            list.add(StringTag.valueOf(v));
        }
        return list;
    }
    
    private static List<String> fromList(final ListTag list) {
        final List<String> out = new ArrayList<String>();
        for (int i = 0; i < list.size(); ++i) {
            out.add(list.getString(i));
        }
        return out;
    }
    
    public InfernalConfig copy() {
        final InfernalConfig c = new InfernalConfig();
        c.mode = this.mode;
        c.min = this.min;
        c.max = this.max;
        c.mods = new ArrayList<String>(this.mods);
        c.pool = new ArrayList<String>(this.pool);
        return c;
    }
    
    public enum Mode
    {
        DISABLED, 
        ALWAYS, 
        RANDOM, 
        CUSTOM;
    }
}
