package com.fscrates.crate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-crate cooldown storage. Persisted in the world save so
 * cooldowns survive restarts. Key format: {@code <playerUUID>|<crateId>} -> the
 * epoch millisecond at which the cooldown expires.
 */
public class CooldownData extends SavedData {

    public static final String NAME = "fscrates_cooldowns";

    private final Map<String, Long> expiry = new HashMap<>();

    public CooldownData() {}

    public static CooldownData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(CooldownData::load, CooldownData::new, NAME);
    }

    private static String key(UUID player, String crateId) {
        return player.toString() + "|" + crateId;
    }

    /** Remaining cooldown in seconds, or 0 if ready. */
    public long remainingSeconds(UUID player, String crateId) {
        Long until = expiry.get(key(player, crateId));
        if (until == null) {
            return 0;
        }
        long remainingMs = until - System.currentTimeMillis();
        return remainingMs <= 0 ? 0 : (remainingMs + 999) / 1000;
    }

    public boolean isReady(UUID player, String crateId) {
        return remainingSeconds(player, crateId) <= 0;
    }

    public void startCooldown(UUID player, String crateId, int seconds) {
        if (seconds <= 0) {
            return;
        }
        expiry.put(key(player, crateId), System.currentTimeMillis() + seconds * 1000L);
        setDirty();
    }

    public static CooldownData load(CompoundTag tag) {
        CooldownData data = new CooldownData();
        CompoundTag stored = tag.getCompound("cooldowns");
        for (String k : stored.getAllKeys()) {
            data.expiry.put(k, stored.getLong(k));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag stored = new CompoundTag();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : expiry.entrySet()) {
            // prune expired entries when saving to keep the file small
            if (e.getValue() > now) {
                stored.putLong(e.getKey(), e.getValue());
            }
        }
        tag.put("cooldowns", stored);
        return tag;
    }
}
