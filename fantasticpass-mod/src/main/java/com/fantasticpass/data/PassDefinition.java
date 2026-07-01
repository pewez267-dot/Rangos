package com.fantasticpass.data;

import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.Quest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A fully editable pass: tier rewards (unlimited tiers), per-pass quest config
 * and, optionally, fully custom daily / weekly quest pools defined by the admin.
 * When a custom pool is empty the built-in {@link DefaultQuests} content is used
 * as a fallback so a pass is playable out of the box.
 */
public final class PassDefinition {
   public static final int MAX_TIERS = 999;
   public static final int MAX_WEEKS = 52;

   private String id;
   private String name;
   private final List<TierDefinition> tiers = new ArrayList<>();
   private int minutesPerTierOverride;
   private int tierCount = 100;
   /** Per-pass quest config. 0 = inherit the global config value. */
   private int dailyFreeCount;
   private int dailyPremiumCount;
   private int weekCountOverride;

   /** Custom quest pools (empty = use the built-in defaults). */
   private final List<Quest> customDailyFree = new ArrayList<>();
   private final List<Quest> customDailyPremium = new ArrayList<>();
   private final List<List<Quest>> customWeeksFree = new ArrayList<>();
   private final List<List<Quest>> customWeeksPremium = new ArrayList<>();

   /** Ordered playlist of http(s) audio links streamed while the pass UI is open. */
   private final List<String> musicUrls = new ArrayList<>();

   public PassDefinition(String id, String name) {
      this.id = id == null ? "" : id;
      this.name = name == null ? "" : name;
      this.minutesPerTierOverride = 0;
      this.ensureTiers(100);
   }

   private void ensureTiers(int count) {
      while (this.tiers.size() < count) {
         this.tiers.add(new TierDefinition(this.tiers.size() + 1));
      }
   }

   /** Number of tiers actually used/shown by this pass (1-999). */
   public int getTierCount() {
      return this.tierCount < 1 ? 100 : Math.min(MAX_TIERS, this.tierCount);
   }

   public void setTierCount(int tierCount) {
      this.tierCount = Math.max(1, Math.min(MAX_TIERS, tierCount));
      this.ensureTiers(this.tierCount);
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id == null ? "" : id;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name == null ? "" : name;
   }

   public int getMinutesPerTierOverride() {
      return this.minutesPerTierOverride;
   }

   public void setMinutesPerTierOverride(int minutesPerTierOverride) {
      this.minutesPerTierOverride = minutesPerTierOverride;
   }

   /** Daily FREE quests per day for this pass (0 = use global config). */
   public int getDailyFreeCount() {
      return this.dailyFreeCount;
   }

   public void setDailyFreeCount(int count) {
      this.dailyFreeCount = Math.max(0, Math.min(12, count));
   }

   /** Extra daily PREMIUM quests per day for this pass (0 = use global config). */
   public int getDailyPremiumCount() {
      return this.dailyPremiumCount;
   }

   public void setDailyPremiumCount(int count) {
      this.dailyPremiumCount = Math.max(0, Math.min(12, count));
   }

   /** Number of weekly sets for this pass (0 = use global config, max 52). */
   public int getWeekCountOverride() {
      return this.weekCountOverride;
   }

   public void setWeekCountOverride(int count) {
      this.weekCountOverride = Math.max(0, Math.min(MAX_WEEKS, count));
   }

   public List<TierDefinition> getTiers() {
      return this.tiers;
   }

   public TierDefinition getTier(int tierNumber) {
      if (tierNumber < 1 || tierNumber > MAX_TIERS) {
         return null;
      }
      this.ensureTiers(tierNumber);
      return this.tiers.get(tierNumber - 1);
   }

   public void setTier(int tierNumber, TierDefinition definition) {
      if (tierNumber >= 1 && tierNumber <= MAX_TIERS && definition != null) {
         this.ensureTiers(tierNumber);
         this.tiers.set(tierNumber - 1, definition);
      }
   }

   // ---- Custom quest pools -------------------------------------------------

   public List<Quest> getCustomDailyFree() {
      return this.customDailyFree;
   }

   /** The pass music playlist (ordered http/https audio links). Streamed via etched. */
   public List<String> getMusicUrls() {
      return this.musicUrls;
   }

   public List<Quest> getCustomDailyPremium() {
      return this.customDailyPremium;
   }

   /** Mutable custom free quest list for a 1-based week (created on demand). */
   public List<Quest> getCustomWeekFree(int week) {
      while (this.customWeeksFree.size() < week) {
         this.customWeeksFree.add(new ArrayList<>());
      }
      return this.customWeeksFree.get(week - 1);
   }

   public List<Quest> getCustomWeekPremium(int week) {
      while (this.customWeeksPremium.size() < week) {
         this.customWeeksPremium.add(new ArrayList<>());
      }
      return this.customWeeksPremium.get(week - 1);
   }

   /** Effective free daily pool: custom if defined, else the built-in pool. */
   public List<Quest> dailyFreePool() {
      return this.customDailyFree.isEmpty() ? DefaultQuests.DAILY_FREE_POOL : this.customDailyFree;
   }

   public List<Quest> dailyPremiumPool() {
      return this.customDailyPremium.isEmpty() ? DefaultQuests.DAILY_PREMIUM_POOL : this.customDailyPremium;
   }

   /** Effective free quests for a 1-based week: custom if defined, else default (cyclic). */
   public List<Quest> weekFreeQuests(int week) {
      if (week >= 1 && week <= this.customWeeksFree.size() && !this.customWeeksFree.get(week - 1).isEmpty()) {
         return this.customWeeksFree.get(week - 1);
      }
      return DefaultQuests.weekQuestsCyclic(week);
   }

   public List<Quest> weekPremiumQuests(int week) {
      if (week >= 1 && week <= this.customWeeksPremium.size() && !this.customWeeksPremium.get(week - 1).isEmpty()) {
         return this.customWeeksPremium.get(week - 1);
      }
      return DefaultQuests.premiumWeekQuestsCyclic(week);
   }

   /** Resolve a quest id against the custom pools first, then the defaults. */
   public Quest resolveQuest(String questId) {
      if (questId == null) {
         return null;
      }
      Quest q = find(this.customDailyFree, questId);
      if (q == null) {
         q = find(this.customDailyPremium, questId);
      }
      for (List<Quest> w : this.customWeeksFree) {
         if (q != null) {
            break;
         }
         q = find(w, questId);
      }
      for (List<Quest> w : this.customWeeksPremium) {
         if (q != null) {
            break;
         }
         q = find(w, questId);
      }
      return q != null ? q : DefaultQuests.byId(questId);
   }

   private static Quest find(List<Quest> list, String id) {
      for (Quest q : list) {
         if (q.getId().equals(id)) {
            return q;
         }
      }
      return null;
   }

   // ---- Persistence --------------------------------------------------------

   public PassDefinition copy() {
      PassDefinition copy = new PassDefinition(this.id, this.name);
      copy.minutesPerTierOverride = this.minutesPerTierOverride;
      copy.tierCount = this.tierCount;
      copy.dailyFreeCount = this.dailyFreeCount;
      copy.dailyPremiumCount = this.dailyPremiumCount;
      copy.weekCountOverride = this.weekCountOverride;
      copy.tiers.clear();
      for (TierDefinition tier : this.tiers) {
         copy.tiers.add(tier.copy());
      }
      copyQuests(this.customDailyFree, copy.customDailyFree);
      copyQuests(this.customDailyPremium, copy.customDailyPremium);
      copyWeeks(this.customWeeksFree, copy.customWeeksFree);
      copyWeeks(this.customWeeksPremium, copy.customWeeksPremium);
      copy.musicUrls.clear();
      copy.musicUrls.addAll(this.musicUrls);
      return copy;
   }

   private static void copyQuests(List<Quest> src, List<Quest> dst) {
      dst.clear();
      dst.addAll(src);
   }

   private static void copyWeeks(List<List<Quest>> src, List<List<Quest>> dst) {
      dst.clear();
      for (List<Quest> w : src) {
         dst.add(new ArrayList<>(w));
      }
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putString("id", this.id);
      tag.putString("name", this.name);
      tag.putInt("minutesPerTierOverride", this.minutesPerTierOverride);
      tag.putInt("tierCount", this.tierCount);
      tag.putInt("dailyFreeCount", this.dailyFreeCount);
      tag.putInt("dailyPremiumCount", this.dailyPremiumCount);
      tag.putInt("weekCountOverride", this.weekCountOverride);

      ListTag list = new ListTag();
      for (TierDefinition tier : this.tiers) {
         list.add(tier.toNbt());
      }
      tag.put("tiers", list);

      tag.put("cDailyFree", questsToNbt(this.customDailyFree));
      tag.put("cDailyPrem", questsToNbt(this.customDailyPremium));
      tag.put("cWeeksFree", weeksToNbt(this.customWeeksFree));
      tag.put("cWeeksPrem", weeksToNbt(this.customWeeksPremium));

      ListTag music = new ListTag();
      for (String url : this.musicUrls) {
         music.add(net.minecraft.nbt.StringTag.valueOf(url));
      }
      tag.put("musicUrls", music);
      return tag;
   }

   public static PassDefinition fromNbt(CompoundTag tag) {
      PassDefinition pass = new PassDefinition(tag.getString("id"), tag.getString("name"));
      pass.minutesPerTierOverride = tag.getInt("minutesPerTierOverride");
      pass.tierCount = tag.contains("tierCount") ? tag.getInt("tierCount") : 100;
      pass.dailyFreeCount = tag.getInt("dailyFreeCount");
      pass.dailyPremiumCount = tag.getInt("dailyPremiumCount");
      pass.weekCountOverride = tag.getInt("weekCountOverride");

      ListTag list = tag.getList("tiers", 10);
      for (int i = 0; i < list.size(); i++) {
         TierDefinition tier = TierDefinition.fromNbt(list.getCompound(i));
         int idx = tier.getTierNumber() - 1;
         if (idx >= 0 && idx < MAX_TIERS) {
            pass.ensureTiers(idx + 1);
            pass.tiers.set(idx, tier);
         }
      }
      pass.ensureTiers(pass.getTierCount());

      questsFromNbt(tag.getList("cDailyFree", 10), pass.customDailyFree);
      questsFromNbt(tag.getList("cDailyPrem", 10), pass.customDailyPremium);
      weeksFromNbt(tag.getList("cWeeksFree", 9), pass.customWeeksFree);
      weeksFromNbt(tag.getList("cWeeksPrem", 9), pass.customWeeksPremium);

      ListTag music = tag.getList("musicUrls", 8);
      pass.musicUrls.clear();
      for (int i = 0; i < music.size(); i++) {
         pass.musicUrls.add(music.getString(i));
      }
      return pass;
   }

   private static ListTag questsToNbt(List<Quest> quests) {
      ListTag list = new ListTag();
      for (Quest q : quests) {
         list.add(q.toNbt());
      }
      return list;
   }

   private static void questsFromNbt(ListTag list, List<Quest> target) {
      target.clear();
      for (int i = 0; i < list.size(); i++) {
         target.add(Quest.fromNbt(list.getCompound(i)));
      }
   }

   private static ListTag weeksToNbt(List<List<Quest>> weeks) {
      ListTag list = new ListTag();
      for (List<Quest> week : weeks) {
         list.add(questsToNbt(week));
      }
      return list;
   }

   private static void weeksFromNbt(ListTag list, List<List<Quest>> target) {
      target.clear();
      for (int i = 0; i < list.size(); i++) {
         Tag t = list.get(i);
         List<Quest> week = new ArrayList<>();
         if (t instanceof ListTag inner) {
            questsFromNbt(inner, week);
         }
         target.add(week);
      }
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUtf(this.id);
      buf.writeUtf(this.name);
      buf.writeVarInt(this.minutesPerTierOverride);
      buf.writeVarInt(this.tierCount);
      buf.writeVarInt(this.dailyFreeCount);
      buf.writeVarInt(this.dailyPremiumCount);
      buf.writeVarInt(this.weekCountOverride);

      buf.writeVarInt(this.tiers.size());
      for (TierDefinition tier : this.tiers) {
         tier.toBuf(buf);
      }

      questsToBuf(buf, this.customDailyFree);
      questsToBuf(buf, this.customDailyPremium);
      weeksToBuf(buf, this.customWeeksFree);
      weeksToBuf(buf, this.customWeeksPremium);

      buf.writeVarInt(this.musicUrls.size());
      for (String url : this.musicUrls) {
         buf.writeUtf(url);
      }
   }

   public static PassDefinition fromBuf(FriendlyByteBuf buf) {
      PassDefinition pass = new PassDefinition(buf.readUtf(), buf.readUtf());
      pass.minutesPerTierOverride = buf.readVarInt();
      pass.tierCount = buf.readVarInt();
      pass.dailyFreeCount = buf.readVarInt();
      pass.dailyPremiumCount = buf.readVarInt();
      pass.weekCountOverride = buf.readVarInt();

      int tierN = buf.readVarInt();
      pass.tiers.clear();
      for (int i = 0; i < tierN; i++) {
         TierDefinition tier = TierDefinition.fromBuf(buf);
         pass.tiers.add(tier);
      }
      pass.ensureTiers(pass.getTierCount());

      questsFromBuf(buf, pass.customDailyFree);
      questsFromBuf(buf, pass.customDailyPremium);
      weeksFromBuf(buf, pass.customWeeksFree);
      weeksFromBuf(buf, pass.customWeeksPremium);

      int musicN = buf.readVarInt();
      pass.musicUrls.clear();
      for (int i = 0; i < musicN; i++) {
         pass.musicUrls.add(buf.readUtf());
      }
      return pass;
   }

   private static void questsToBuf(FriendlyByteBuf buf, List<Quest> quests) {
      buf.writeVarInt(quests.size());
      for (Quest q : quests) {
         q.toBuf(buf);
      }
   }

   private static void questsFromBuf(FriendlyByteBuf buf, List<Quest> target) {
      target.clear();
      int n = buf.readVarInt();
      for (int i = 0; i < n; i++) {
         target.add(Quest.fromBuf(buf));
      }
   }

   private static void weeksToBuf(FriendlyByteBuf buf, List<List<Quest>> weeks) {
      buf.writeVarInt(weeks.size());
      for (List<Quest> week : weeks) {
         questsToBuf(buf, week);
      }
   }

   private static void weeksFromBuf(FriendlyByteBuf buf, List<List<Quest>> target) {
      target.clear();
      int n = buf.readVarInt();
      for (int i = 0; i < n; i++) {
         List<Quest> week = new ArrayList<>();
         questsFromBuf(buf, week);
         target.add(week);
      }
   }
}
