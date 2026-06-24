package com.fantastic.kits.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player permanent record. Tracks every kit the player has already claimed
 * along with the timestamp, plus a small history of recent denied attempts so
 * the audit log can correlate suspicious activity.
 *
 * <p>Once a kit id is added to {@link #claimed} it cannot be reclaimed. There
 * is no expiration, no cooldown reset and no admin GUI to wipe it: by design,
 * permanent means permanent.
 */
public final class PlayerData {

    private final UUID playerId;
    private String lastKnownName;
    private final Map<String, Long> claimed = new ConcurrentHashMap<>();
    private final Map<String, Long> deniedAttempts = new LinkedHashMap<>();

    public PlayerData(UUID playerId, String lastKnownName) {
        this.playerId = playerId;
        this.lastKnownName = lastKnownName;
    }

    public UUID playerId() { return playerId; }
    public String lastKnownName() { return lastKnownName; }
    public void lastKnownName(String name) {
        if (name != null && !name.isBlank()) this.lastKnownName = name;
    }

    public boolean hasClaimed(String kitId) {
        return kitId != null && claimed.containsKey(kitId.toLowerCase());
    }

    public Set<String> claimedKitIds() {
        return Collections.unmodifiableSet(claimed.keySet());
    }

    public long claimedAt(String kitId) {
        Long ts = claimed.get(kitId == null ? "" : kitId.toLowerCase());
        return ts == null ? 0L : ts;
    }

    /**
     * Records a successful claim. Returns false if the kit had already been
     * claimed - the caller is expected to treat that as an exploit attempt.
     */
    public boolean recordClaim(String kitId, long timestamp) {
        if (kitId == null) return false;
        String key = kitId.toLowerCase();
        if (claimed.containsKey(key)) return false;
        claimed.put(key, timestamp);
        return true;
    }

    public void recordDenied(String kitId, long timestamp) {
        if (kitId == null) return;
        synchronized (deniedAttempts) {
            deniedAttempts.put(kitId.toLowerCase() + "@" + timestamp, timestamp);
            if (deniedAttempts.size() > 64) {
                // Keep the structure small.
                java.util.Iterator<Map.Entry<String, Long>> it = deniedAttempts.entrySet().iterator();
                while (deniedAttempts.size() > 64 && it.hasNext()) { it.next(); it.remove(); }
            }
        }
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("uuid", playerId);
        if (lastKnownName != null) t.putString("name", lastKnownName);

        ListTag claimedList = new ListTag();
        for (Map.Entry<String, Long> e : claimed.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", e.getKey());
            entry.putLong("at", e.getValue());
            claimedList.add(entry);
        }
        t.put("claimed", claimedList);

        ListTag denied = new ListTag();
        synchronized (deniedAttempts) {
            for (Map.Entry<String, Long> e : deniedAttempts.entrySet()) {
                denied.add(StringTag.valueOf(e.getKey()));
            }
        }
        t.put("denied", denied);
        return t;
    }

    public static PlayerData load(CompoundTag t) {
        UUID uuid = t.hasUUID("uuid") ? t.getUUID("uuid") : UUID.randomUUID();
        String name = t.contains("name") ? t.getString("name") : "";
        PlayerData pd = new PlayerData(uuid, name);
        if (t.contains("claimed")) {
            ListTag list = t.getList("claimed", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.getString("id");
                long at = entry.getLong("at");
                if (!id.isBlank()) pd.claimed.put(id, at);
            }
        }
        if (t.contains("denied")) {
            ListTag denied = t.getList("denied", Tag.TAG_STRING);
            synchronized (pd.deniedAttempts) {
                for (int i = 0; i < denied.size(); i++) {
                    String s = denied.getString(i);
                    pd.deniedAttempts.put(s, 0L);
                }
            }
        }
        return pd;
    }
}
