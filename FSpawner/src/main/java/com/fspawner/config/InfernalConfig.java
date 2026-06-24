package com.fspawner.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Infernal Mobs integration configuration. */
public class InfernalConfig {

    public enum Mode {
        DISABLED,   // no infernal at all
        ALWAYS,     // always infernal, using the explicit {@link #mods} list
        RANDOM,     // pick min..max random modifiers from {@link #pool}
        CUSTOM      // exactly the modifiers in {@link #mods}
    }

    public Mode mode = Mode.DISABLED;
    /** Explicit modifier list (internal names) for ALWAYS / CUSTOM. */
    public List<String> mods = new ArrayList<>();
    /** Allowed pool (internal names) used by RANDOM. */
    public List<String> pool = new ArrayList<>();
    public int min = 2;
    public int max = 5;

    public InfernalConfig() {}

    public boolean isEnabled() {
        return mode != Mode.DISABLED;
    }

    /** Resolves the final space-separated modifier string for a single spawn. */
    public String resolveModifierString(RandomSource random) {
        switch (mode) {
            case ALWAYS:
            case CUSTOM:
                return String.join(" ", mods);
            case RANDOM: {
                if (pool.isEmpty()) {
                    return "";
                }
                int lo = Math.max(0, Math.min(min, max));
                int hi = Math.max(min, max);
                int count = lo + (hi > lo ? random.nextInt(hi - lo + 1) : 0);
                count = Math.min(count, pool.size());
                List<String> shuffled = new ArrayList<>(pool);
                Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
                return String.join(" ", shuffled.subList(0, count));
            }
            default:
                return "";
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FSKeys.INF_MODE, mode.name());
        tag.putInt(FSKeys.INF_MIN, min);
        tag.putInt(FSKeys.INF_MAX, max);
        tag.put(FSKeys.INF_MODS, toList(mods));
        tag.put(FSKeys.INF_POOL, toList(pool));
        return tag;
    }

    public static InfernalConfig load(CompoundTag tag) {
        InfernalConfig cfg = new InfernalConfig();
        try {
            cfg.mode = Mode.valueOf(tag.getString(FSKeys.INF_MODE));
        } catch (IllegalArgumentException ignored) {
            cfg.mode = Mode.DISABLED;
        }
        cfg.min = tag.contains(FSKeys.INF_MIN) ? tag.getInt(FSKeys.INF_MIN) : 2;
        cfg.max = tag.contains(FSKeys.INF_MAX) ? tag.getInt(FSKeys.INF_MAX) : 5;
        cfg.mods = fromList(tag.getList(FSKeys.INF_MODS, Tag.TAG_STRING));
        cfg.pool = fromList(tag.getList(FSKeys.INF_POOL, Tag.TAG_STRING));
        return cfg;
    }

    private static ListTag toList(List<String> values) {
        ListTag list = new ListTag();
        for (String v : values) {
            list.add(StringTag.valueOf(v));
        }
        return list;
    }

    private static List<String> fromList(ListTag list) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    public InfernalConfig copy() {
        InfernalConfig c = new InfernalConfig();
        c.mode = mode;
        c.min = min;
        c.max = max;
        c.mods = new ArrayList<>(mods);
        c.pool = new ArrayList<>(pool);
        return c;
    }
}
