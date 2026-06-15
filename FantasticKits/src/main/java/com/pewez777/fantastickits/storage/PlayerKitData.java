/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.storage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Per-player persistent record of kit claims and history.
 *
 * <p>The set of claimed kit ids enforces the "single permanent claim" rule:
 * once a kit id is present, it can never be claimed again by this player. The
 * history list is an append-only audit of every claim/denial for the player.</p>
 */
public final class PlayerKitData {

    /** A single immutable history entry. */
    public static final class HistoryEntry {
        public final String kitId;
        public final String kitName;
        public final String group;
        public final long timestamp;
        public final String action;
        public final String result;

        public HistoryEntry(String kitId, String kitName, String group,
                            long timestamp, String action, String result) {
            this.kitId = kitId == null ? "" : kitId;
            this.kitName = kitName == null ? "" : kitName;
            this.group = group == null ? "" : group;
            this.timestamp = timestamp;
            this.action = action == null ? "" : action;
            this.result = result == null ? "" : result;
        }
    }

    private final UUID playerId;
    private String lastKnownName;
    private final Set<String> claimedKitIds = new LinkedHashSet<>();
    private final List<HistoryEntry> history = new ArrayList<>();

    public PlayerKitData(UUID playerId) {
        this.playerId = playerId;
        this.lastKnownName = "";
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String name) {
        if (name != null && !name.isEmpty()) {
            this.lastKnownName = name;
        }
    }

    public boolean hasClaimed(String kitId) {
        return kitId != null && claimedKitIds.contains(kitId);
    }

    public void markClaimed(String kitId) {
        if (kitId != null && !kitId.isEmpty()) {
            claimedKitIds.add(kitId);
        }
    }

    /** Removes a claim record (used when a kit is permanently deleted). */
    public boolean removeClaim(String kitId) {
        return claimedKitIds.remove(kitId);
    }

    public Set<String> getClaimedKitIds() {
        return claimedKitIds;
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public void addHistory(HistoryEntry entry) {
        if (entry != null) {
            history.add(entry);
        }
    }

    // ---- Serialization -----------------------------------------------------

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerId", playerId.toString());
        tag.putString("LastKnownName", lastKnownName);

        ListTag claims = new ListTag();
        for (String id : claimedKitIds) {
            claims.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        tag.put("ClaimedKits", claims);

        ListTag historyTag = new ListTag();
        for (HistoryEntry entry : history) {
            CompoundTag e = new CompoundTag();
            e.putString("KitId", entry.kitId);
            e.putString("KitName", entry.kitName);
            e.putString("Group", entry.group);
            e.putLong("Time", entry.timestamp);
            e.putString("Action", entry.action);
            e.putString("Result", entry.result);
            historyTag.add(e);
        }
        tag.put("History", historyTag);
        return tag;
    }

    public static PlayerKitData fromNbt(UUID fallbackId, CompoundTag tag) {
        UUID id = fallbackId;
        if (tag != null && tag.contains("PlayerId")) {
            try {
                id = UUID.fromString(tag.getString("PlayerId"));
            } catch (IllegalArgumentException ignored) {
                id = fallbackId;
            }
        }
        PlayerKitData data = new PlayerKitData(id);
        if (tag == null) {
            return data;
        }
        data.setLastKnownName(tag.getString("LastKnownName"));

        ListTag claims = tag.getList("ClaimedKits", Tag.TAG_STRING);
        for (int i = 0; i < claims.size(); i++) {
            data.markClaimed(claims.getString(i));
        }

        ListTag historyTag = tag.getList("History", Tag.TAG_COMPOUND);
        for (int i = 0; i < historyTag.size(); i++) {
            CompoundTag e = historyTag.getCompound(i);
            data.addHistory(new HistoryEntry(
                    e.getString("KitId"),
                    e.getString("KitName"),
                    e.getString("Group"),
                    e.getLong("Time"),
                    e.getString("Action"),
                    e.getString("Result")));
        }
        return data;
    }
}
