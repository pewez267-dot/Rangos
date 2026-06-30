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
   private int points;
   private long dailyResetDay;
   private int currentWeek = 1;
   private final List<String> dailyQuestIds = new ArrayList<>();
   private final Map<String, Integer> questProgress = new LinkedHashMap<>();
   private final Set<String> claimedQuests = new LinkedHashSet<>();

   public int getPoints() {
      return this.points;
   }

   public void setPoints(int points) {
      this.points = Math.max(0, points);
   }

   public void addPoints(int amount) {
      if (amount > 0) {
         this.points += amount;
      }
   }

   public long getDailyResetDay() {
      return this.dailyResetDay;
   }

   public void setDailyResetDay(long day) {
      this.dailyResetDay = day;
   }

   public int getCurrentWeek() {
      return this.currentWeek;
   }

   public void setCurrentWeek(int week) {
      this.currentWeek = Math.max(1, week);
   }

   public List<String> getDailyQuestIds() {
      return this.dailyQuestIds;
   }

   public void setDailyQuestIds(List<String> ids) {
      this.dailyQuestIds.clear();
      if (ids != null) {
         this.dailyQuestIds.addAll(ids);
      }
   }

   public int getQuestProgress(String questId) {
      return this.questProgress.getOrDefault(questId, 0);
   }

   public Map<String, Integer> getAllQuestProgress() {
      return this.questProgress;
   }

   public void setQuestProgress(String questId, int value) {
      this.questProgress.put(questId, Math.max(0, value));
   }

   public int addQuestProgress(String questId, int amount) {
      int v = this.getQuestProgress(questId) + Math.max(0, amount);
      this.questProgress.put(questId, v);
      return v;
   }

   public boolean isQuestClaimed(String questId) {
      return this.claimedQuests.contains(questId);
   }

   public void markQuestClaimed(String questId) {
      this.claimedQuests.add(questId);
   }

   public Set<String> getClaimedQuests() {
      return this.claimedQuests;
   }

   /** Clears progress/claims for the current daily set so a fresh day starts clean. */
   public void resetDaily(List<String> newDailyIds, long day) {
      for (String id : this.dailyQuestIds) {
         this.questProgress.remove(id);
         this.claimedQuests.remove(id);
      }

      this.setDailyQuestIds(newDailyIds);
      this.dailyResetDay = day;
   }

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
      this.points = 0;
      this.currentWeek = 1;
      this.dailyResetDay = 0L;
      this.dailyQuestIds.clear();
      this.questProgress.clear();
      this.claimedQuests.clear();
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
      this.points = other.points;
      this.dailyResetDay = other.dailyResetDay;
      this.currentWeek = other.currentWeek;
      this.dailyQuestIds.clear();
      this.dailyQuestIds.addAll(other.dailyQuestIds);
      this.questProgress.clear();
      this.questProgress.putAll(other.questProgress);
      this.claimedQuests.clear();
      this.claimedQuests.addAll(other.claimedQuests);
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

      tag.putInt("points", this.points);
      tag.putLong("dailyResetDay", this.dailyResetDay);
      tag.putInt("currentWeek", this.currentWeek);
      ListTag daily = new ListTag();
      for (String id : this.dailyQuestIds) {
         daily.add(net.minecraft.nbt.StringTag.valueOf(id));
      }
      tag.put("dailyQuestIds", daily);
      CompoundTag progress = new CompoundTag();
      for (Map.Entry<String, Integer> e : this.questProgress.entrySet()) {
         progress.putInt(e.getKey(), e.getValue());
      }
      tag.put("questProgress", progress);
      ListTag claimedQ = new ListTag();
      for (String id : this.claimedQuests) {
         claimedQ.add(net.minecraft.nbt.StringTag.valueOf(id));
      }
      tag.put("claimedQuests", claimedQ);
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
      this.points = tag.getInt("points");
      this.dailyResetDay = tag.getLong("dailyResetDay");
      this.currentWeek = Math.max(1, tag.getInt("currentWeek"));
      this.dailyQuestIds.clear();
      ListTag daily = tag.getList("dailyQuestIds", 8);
      for (int i = 0; i < daily.size(); i++) {
         this.dailyQuestIds.add(daily.getString(i));
      }

      this.questProgress.clear();
      CompoundTag progress = tag.getCompound("questProgress");
      for (String key : progress.getAllKeys()) {
         this.questProgress.put(key, progress.getInt(key));
      }

      this.claimedQuests.clear();
      ListTag claimedQ = tag.getList("claimedQuests", 8);
      for (int i = 0; i < claimedQ.size(); i++) {
         this.claimedQuests.add(claimedQ.getString(i));
      }
   }
}
