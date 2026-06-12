// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.crate;

import net.minecraft.nbt.Tag;
import java.util.Iterator;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.saveddata.SavedData;

public class CooldownData extends SavedData
{
    public static final String NAME = "fscrates_cooldowns";
    private final Map<String, Long> expiry;
    
    public CooldownData() {
        this.expiry = new HashMap<String, Long>();
    }
    
    public static CooldownData get(final ServerLevel anyLevel) {
        final ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CooldownData::new, CooldownData::load), "fscrates_cooldowns");
    }
    
    private static String key(final UUID player, final String crateId) {
        return player.toString() + "|" + crateId;
    }
    
    public long remainingSeconds(final UUID player, final String crateId) {
        final Long until = this.expiry.get(key(player, crateId));
        if (until == null) {
            return 0L;
        }
        final long remainingMs = until - System.currentTimeMillis();
        return (remainingMs <= 0L) ? 0L : ((remainingMs + 999L) / 1000L);
    }
    
    public boolean isReady(final UUID player, final String crateId) {
        return this.remainingSeconds(player, crateId) <= 0L;
    }
    
    public void startCooldown(final UUID player, final String crateId, final int seconds) {
        if (seconds <= 0) {
            return;
        }
        this.expiry.put(key(player, crateId), System.currentTimeMillis() + seconds * 1000L);
        this.setDirty();
    }
    
    public static CooldownData load(final CompoundTag tag) {
        final CooldownData data = new CooldownData();
        final CompoundTag stored = tag.getCompound("cooldowns");
        for (final String k : stored.getAllKeys()) {
            data.expiry.put(k, stored.getLong(k));
        }
        return data;
    }
    
    public CompoundTag save(final CompoundTag tag) {
        final CompoundTag stored = new CompoundTag();
        final long now = System.currentTimeMillis();
        for (final Map.Entry<String, Long> e : this.expiry.entrySet()) {
            if (e.getValue() > now) {
                stored.putLong((String)e.getKey(), (long)e.getValue());
            }
        }
        tag.put("cooldowns", (Tag)stored);
        return tag;
    }
}
