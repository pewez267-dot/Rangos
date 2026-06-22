package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-player Battle Pass progress. Persisted via a Forge capability (NBT). A single
 * record exists per player regardless of how many passes have been active over time.
 */
public final class PlayerPassData {

    private int minutesActive;
    /** Active seconds accumulated towards the next whole minute (0..59). */
    private int partialSeconds;
    private int currentTier;
    private final Set<Integer> claimedTiers = new LinkedHashSet<>();
    private boolean isPremium;
    private String activePassId = "";
    /**
     * Full earned pass-rank rewards keyed by rank id. The complete style/text is kept
     * with the player so the nametag still renders correctly even after the granting
     * pass is deleted or replaced. Insertion order is preserved.
     */
    private final Map<String, PassRankReward> earnedRanks = new LinkedHashMap<>();
    /** Rank id currently displayed under the player's name, or {@code null} for none. */
    private String displayedRankId;

    public int getMinutesActive() {
        return minutesActive;
    }

    public void setMinutesActive(int minutesActive) {
        this.minutesActive = Math.max(0, minutesActive);
    }

    public int getPartialSeconds() {
        return partialSeconds;
    }

    /**
     * Adds active seconds and rolls whole minutes into {@link #minutesActive}.
     *
     * @return the number of whole minutes that were added
     */
    public int addActiveSeconds(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        partialSeconds += seconds;
        int gained = partialSeconds / 60;
        partialSeconds %= 60;
        if (gained > 0) {
            minutesActive += gained;
        }
        return gained;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(int currentTier) {
        this.currentTier = Math.max(0, Math.min(PassDefinition.TIER_COUNT, currentTier));
    }

    public Set<Integer> getClaimedTiers() {
        return claimedTiers;
    }

    public boolean isTierClaimed(int tier) {
        return claimedTiers.contains(tier);
    }

    public void markClaimed(int tier) {
        claimedTiers.add(tier);
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        this.isPremium = premium;
    }

    public String getActivePassId() {
        return activePassId;
    }

    public void setActivePassId(String activePassId) {
        this.activePassId = activePassId == null ? "" : activePassId;
    }

    public List<String> getEarnedRankIds() {
        return new ArrayList<>(earnedRanks.keySet());
    }

    public Map<String, PassRankReward> getEarnedRanks() {
        return earnedRanks;
    }

    public boolean hasEarnedRank(String rankId) {
        return rankId != null && earnedRanks.containsKey(rankId);
    }

    @Nullable
    public PassRankReward getEarnedRank(String rankId) {
        return rankId == null ? null : earnedRanks.get(rankId);
    }

    public void addEarnedRank(PassRankReward reward) {
        if (reward != null && reward.isValid()) {
            // Store a copy so later edits to the pass template do not mutate player history.
            earnedRanks.put(reward.getRankId(), reward.copy());
        }
    }

    public String getDisplayedRankId() {
        return displayedRankId;
    }

    public void setDisplayedRankId(String displayedRankId) {
        this.displayedRankId = (displayedRankId == null || displayedRankId.isEmpty()) ? null : displayedRankId;
    }

    /**
     * Resets seasonal progress when a new pass is activated, preserving the rank
     * history ({@link #earnedRankIds}) and the currently displayed rank.
     */
    public void resetForNewSeason(String newActivePassId) {
        this.minutesActive = 0;
        this.partialSeconds = 0;
        this.currentTier = 0;
        this.claimedTiers.clear();
        this.isPremium = false;
        this.activePassId = newActivePassId == null ? "" : newActivePassId;
        // earnedRankIds and displayedRankId intentionally preserved.
    }

    public void copyFrom(PlayerPassData other) {
        this.minutesActive = other.minutesActive;
        this.partialSeconds = other.partialSeconds;
        this.currentTier = other.currentTier;
        this.claimedTiers.clear();
        this.claimedTiers.addAll(other.claimedTiers);
        this.isPremium = other.isPremium;
        this.activePassId = other.activePassId;
        this.earnedRanks.clear();
        this.earnedRanks.putAll(other.earnedRanks);
        this.displayedRankId = other.displayedRankId;
    }

    // ---- NBT ----

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("minutesActive", minutesActive);
        tag.putInt("partialSeconds", partialSeconds);
        tag.putInt("currentTier", currentTier);
        tag.putBoolean("isPremium", isPremium);
        tag.putString("activePassId", activePassId);

        int[] claimed = new int[claimedTiers.size()];
        int i = 0;
        for (int tier : claimedTiers) {
            claimed[i++] = tier;
        }
        tag.putIntArray("claimedTiers", claimed);

        ListTag ranks = new ListTag();
        for (PassRankReward reward : earnedRanks.values()) {
            ranks.add(reward.toNbt());
        }
        tag.put("earnedRanks", ranks);

        if (displayedRankId != null) {
            tag.putString("displayedRankId", displayedRankId);
        }
        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        this.minutesActive = tag.getInt("minutesActive");
        this.partialSeconds = tag.getInt("partialSeconds");
        this.currentTier = tag.getInt("currentTier");
        this.isPremium = tag.getBoolean("isPremium");
        this.activePassId = tag.getString("activePassId");

        this.claimedTiers.clear();
        for (int tier : tag.getIntArray("claimedTiers")) {
            this.claimedTiers.add(tier);
        }

        this.earnedRanks.clear();
        ListTag ranks = tag.getList("earnedRanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < ranks.size(); i++) {
            PassRankReward reward = PassRankReward.fromNbt(ranks.getCompound(i));
            if (reward.isValid()) {
                this.earnedRanks.put(reward.getRankId(), reward);
            }
        }

        this.displayedRankId = tag.contains("displayedRankId", Tag.TAG_STRING)
                ? tag.getString("displayedRankId") : null;
    }
}
