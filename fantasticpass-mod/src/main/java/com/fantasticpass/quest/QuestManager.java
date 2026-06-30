package com.fantasticpass.quest;

import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Server-side quest engine. Daily quests reset every real day (free players get
 * the free pool; premium players also roll a bonus premium pool). Weekly quests
 * are worked through week by week — a week unlocks the next once all of its
 * quests (free, plus premium for premium players) are done. Completing a quest
 * auto-awards its points, which drive tier progression.
 */
public final class QuestManager {
   private QuestManager() {
   }

   public static int pointsPerTier() {
      int v = PassConfig.POINTS_PER_TIER.get();
      return v <= 0 ? 100 : v;
   }

   public static long today() {
      return System.currentTimeMillis() / 86400000L;
   }

   /** Active pass on the running server (null on the client). */
   private static PassDefinition activePass() {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return server == null ? null : PassSavedData.get(server).getActivePass();
   }

   /** Daily free count: per-pass override (if &gt;0) else the global config. */
   public static int dailyFreeCount() {
      PassDefinition pass = activePass();
      int override = pass == null ? 0 : pass.getDailyFreeCount();
      return override > 0 ? override : PassConfig.DAILY_FREE_COUNT.get();
   }

   /** Extra daily premium count: per-pass override (if &gt;0) else the global config. */
   public static int dailyPremiumCount() {
      PassDefinition pass = activePass();
      int override = pass == null ? 0 : pass.getDailyPremiumCount();
      return override > 0 ? override : PassConfig.DAILY_PREMIUM_COUNT.get();
   }

   /** Assign a fresh daily set when the day has rolled over, none exist, or stored ids are stale. */
   public static void ensureDaily(UUID uuid, PlayerPassData data) {
      long today = today();
      PassDefinition pass = activePass();
      boolean stale = data.getDailyQuestIds().isEmpty();
      if (!stale) {
         for (String id : data.getDailyQuestIds()) {
            if (resolve(pass, id) == null) {
               stale = true; // ids from an older quest pool no longer resolve
               break;
            }
         }
      }

      if (data.getDailyResetDay() != today || stale) {
         data.resetDaily(rollDaily(uuid, today, data.isPremium()), today);
      }
   }

   private static Quest resolve(PassDefinition pass, String id) {
      return pass != null ? pass.resolveQuest(id) : DefaultQuests.byId(id);
   }

   /** Force a fresh daily set right now (used by the admin test command). */
   public static void rerollDaily(UUID uuid, PlayerPassData data) {
      data.resetDaily(rollDaily(uuid, today(), data.isPremium()), today());
   }

   private static List<String> rollDaily(UUID uuid, long day, boolean premium) {
      PassDefinition pass = activePass();
      List<Quest> freePool = pass != null ? pass.dailyFreePool() : DefaultQuests.DAILY_FREE_POOL;
      List<Quest> premPool = pass != null ? pass.dailyPremiumPool() : DefaultQuests.DAILY_PREMIUM_POOL;
      List<String> ids = new ArrayList<>();
      pickInto(ids, new ArrayList<>(freePool), uuid, day, dailyFreeCount(), 1L);
      if (premium) {
         pickInto(ids, new ArrayList<>(premPool), uuid, day, dailyPremiumCount(), 31L);
      }

      return ids;
   }

   /**
    * The premium daily quests a player would roll today, regardless of premium
    * status. Used to show free players the premium track as locked previews.
    */
   public static List<Quest> previewPremiumDaily(PassDefinition pass, UUID uuid, int count) {
      List<Quest> pool = pass != null ? pass.dailyPremiumPool() : DefaultQuests.DAILY_PREMIUM_POOL;
      List<String> ids = new ArrayList<>();
      pickInto(ids, new ArrayList<>(pool), uuid, today(), count, 31L);
      List<Quest> out = new ArrayList<>();
      for (String id : ids) {
         Quest q = resolve(pass, id);
         if (q != null) {
            out.add(q);
         }
      }
      return out;
   }

   private static void pickInto(List<String> out, List<Quest> pool, UUID uuid, long day, int count, long salt) {
      Random rng = new Random(day * 1099511628211L ^ uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ salt * 2654435761L);
      Collections.shuffle(pool, rng);
      for (int i = 0; i < Math.min(Math.max(0, count), pool.size()); i++) {
         out.add(pool.get(i).getId());
      }
   }

   public static List<Quest> activeDaily(PlayerPassData data) {
      return activeDaily(activePass(), data);
   }

   /** Resolve the player's daily quest ids against the given pass (custom + defaults). */
   public static List<Quest> activeDaily(PassDefinition pass, PlayerPassData data) {
      List<Quest> list = new ArrayList<>();
      for (String id : data.getDailyQuestIds()) {
         Quest q = resolve(pass, id);
         if (q != null) {
            list.add(q);
         }
      }

      return list;
   }

   /** Recompute the tier from points; returns true if it advanced. */
   public static boolean recomputeTier(PlayerPassData data) {
      PassDefinition pass = activePass();
      int maxTier = pass == null ? 100 : pass.getTierCount();
      int newTier = Math.max(0, Math.min(maxTier, data.getPoints() / pointsPerTier()));
      if (newTier > data.getCurrentTier()) {
         data.setCurrentTier(newTier);
         return true;
      }

      return false;
   }

   /**
    * Apply an objective event to the player's daily and current-week quests.
    * Returns true if the tier changed (caller should resync the nametag).
    */
   public static boolean track(ServerPlayer player, PlayerPassData data, QuestType type, int amount) {
      return track(player, data, type, "", amount);
   }

   /**
    * Track progress for an event of {@code type}. For parameterized quests the
    * {@code param} (entity/block/item id) must match the quest's target.
    */
   public static boolean track(ServerPlayer player, PlayerPassData data, QuestType type, String param, int amount) {
      if (amount <= 0) {
         return false;
      }

      ensureDaily(player.getUUID(), data);
      PassDefinition pass = activePass();
      boolean tierChanged = false;

      for (Quest q : activeDaily(pass, data)) {
         tierChanged |= progress(player, data, q, type, param, amount);
      }

      int w = data.getCurrentWeek();
      List<Quest> week = new ArrayList<>(pass != null ? pass.weekFreeQuests(w) : DefaultQuests.weekQuestsCyclic(w));
      if (data.isPremium()) {
         week.addAll(pass != null ? pass.weekPremiumQuests(w) : DefaultQuests.premiumWeekQuestsCyclic(w));
      }
      for (Quest q : week) {
         tierChanged |= progress(player, data, q, type, param, amount);
      }

      boolean allDone = true;
      for (Quest q : week) {
         if (!data.isQuestClaimed(q.getId())) {
            allDone = false;
            break;
         }
      }

      if (allDone && data.getCurrentWeek() < DefaultQuests.effectiveWeekCount(pass)) {
         data.setCurrentWeek(data.getCurrentWeek() + 1);
         player.sendSystemMessage(Component.translatable("fantasticpass.msg.week_unlocked", data.getCurrentWeek()).withStyle(ChatFormatting.AQUA));
      }

      return tierChanged;
   }

   private static boolean progress(ServerPlayer player, PlayerPassData data, Quest quest, QuestType type, String param, int amount) {
      if (quest.getType() != type || data.isQuestClaimed(quest.getId())) {
         return false;
      }
      // Parameterized quests only advance on a matching target id.
      if (!quest.getParam().isEmpty() && !quest.getParam().equalsIgnoreCase(param)) {
         return false;
      }

      int p = data.addQuestProgress(quest.getId(), amount);
      if (p >= quest.getTarget()) {
         data.markQuestClaimed(quest.getId());
         data.addPoints(quest.getPoints());
         // Clean compact toast instead of a green chat line.
         boolean premiumQuest = quest.getId().startsWith("dp_") || quest.getId().startsWith("wp");
         com.fantasticpass.network.PacketHandler.sendToPlayer(
            player, new com.fantasticpass.network.QuestCompletePacket(quest.getType(), quest.getTarget(), quest.getPoints(), premiumQuest));
         return recomputeTier(data);
      }

      return false;
   }
}
