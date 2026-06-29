package com.fantasticpass.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class PlayerPassData {
   private int minutesActive;
   private int partialSeconds;
   private int currentTier;
   private final Set<Integer> claimedTiers = new LinkedHashSet<>();
   private boolean isPremium;
   private String activePassId = "";
   private final Map<String, PassRankReward> earnedRanks = new LinkedHashMap<>();
   private String displayedRankId;

   public int getMinutesActive() {
      return this.minutesActive;
   }

   public void setMinutesActive(int minutesActive) {
      this.minutesActive = Math.max(0, minutesActive);
   }

   public int getPartialSeconds() {
      return this.partialSeconds;
   }

   public int addActiveSeconds(int seconds) {
      if (seconds <= 0) {
         return 0;
      } else {
         this.partialSeconds += seconds;
         int gained = this.partialSeconds / 60;
         this.partialSeconds %= 60;
         if (gained > 0) {
            this.minutesActive += gained;
         }

         return gained;
      }
   }

   public int getCurrentTier() {
      return this.currentTier;
   }

   public void setCurrentTier(int currentTier) {
      this.currentTier = Math.max(0, Math.min(100, currentTier));
   }

   public Set<Integer> getClaimedTiers() {
      return this.claimedTiers;
   }

   public boolean isTierClaimed(int tier) {
      return this.claimedTiers.contains(tier);
   }

   public void markClaimed(int tier) {
      this.claimedTiers.add(tier);
   }

   public boolean isPremium() {
      return this.isPremium;
   }

   public void setPremium(boolean premium) {
      this.isPremium = premium;
   }

   public String getActivePassId() {
      return this.activePassId;
   }

   public void setActivePassId(String activePassId) {
      this.activePassId = activePassId == null ? "" : activePassId;
   }

   public List<String> getEarnedRankIds() {
      return new ArrayList<>(this.earnedRanks.keySet());
   }

   public Map<String, PassRankReward> getEarnedRanks() {
      return this.earnedRanks;
   }

   public boolean hasEarnedRank(String rankId) {
      return rankId != null && this.earnedRanks.containsKey(rankId);
   }

   @Nullable
   public PassRankReward getEarnedRank(String rankId) {
      return rankId == null ? null : this.earnedRanks.get(rankId);
   }

   public void addEarnedRank(PassRankReward reward) {
      if (reward != null && reward.isValid()) {
         this.earnedRanks.put(reward.getRankId(), reward.copy());
      }
   }

   public String getDisplayedRankId() {
      return this.displayedRankId;
   }

   public void setDisplayedRankId(String displayedRankId) {
      this.displayedRankId = displayedRankId != null && !displayedRankId.isEmpty() ? displayedRankId : null;
   }

   public void resetForNewSeason(String newActivePassId) {
      this.minutesActive = 0;
      this.partialSeconds = 0;
      this.currentTier = 0;
      this.claimedTiers.clear();
      this.isPremium = false;
      this.activePassId = newActivePassId == null ? "" : newActivePassId;
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

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("minutesActive", this.minutesActive);
      tag.putInt("partialSeconds", this.partialSeconds);
      tag.putInt("currentTier", this.currentTier);
      tag.putBoolean("isPremium", this.isPremium);
      tag.putString("activePassId", this.activePassId);
      int[] claimed = new int[this.claimedTiers.size()];
      int i = 0;

      for (int tier : this.claimedTiers) {
         claimed[i++] = tier;
      }

      tag.putIntArray("claimedTiers", claimed);
      ListTag ranks = new ListTag();

      for (PassRankReward reward : this.earnedRanks.values()) {
         ranks.add(reward.toNbt());
      }

      tag.put("earnedRanks", ranks);
      if (this.displayedRankId != null) {
         tag.putString("displayedRankId", this.displayedRankId);
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
      ListTag ranks = tag.getList("earnedRanks", 10);

      for (int i = 0; i < ranks.size(); i++) {
         PassRankReward reward = PassRankReward.fromNbt(ranks.getCompound(i));
         if (reward.isValid()) {
            this.earnedRanks.put(reward.getRankId(), reward);
         }
      }

      this.displayedRankId = tag.contains("displayedRankId", 8) ? tag.getString("displayedRankId") : null;
   }
}
