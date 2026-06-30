package com.fantasticpass.quest;

import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PlayerPassData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side quest engine. Daily quests reset every real day; weekly quests are
 * worked through week by week (a week unlocks once all its quests are done).
 * Completing a quest auto-awards its points, which drive tier progression.
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

   /** Assign a fresh daily set when the day has rolled over. */
   public static void ensureDaily(UUID uuid, PlayerPassData data) {
      long today = today();
      if (data.getDailyResetDay() != today || data.getDailyQuestIds().isEmpty()) {
         data.resetDaily(rollDaily(uuid, today), today);
      }
   }

   private static List<String> rollDaily(UUID uuid, long day) {
      List<Quest> pool = new ArrayList<>(DefaultQuests.DAILY_POOL);
      Random rng = new Random(day * 1099511628211L ^ uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
      Collections.shuffle(pool, rng);
      List<String> ids = new ArrayList<>();
      for (int i = 0; i < Math.min(DefaultQuests.DAILY_PER_DAY, pool.size()); i++) {
         ids.add(pool.get(i).getId());
      }

      return ids;
   }

   public static List<Quest> activeDaily(PlayerPassData data) {
      List<Quest> list = new ArrayList<>();
      for (String id : data.getDailyQuestIds()) {
         Quest q = DefaultQuests.byId(id);
         if (q != null) {
            list.add(q);
         }
      }

      return list;
   }

   /** Recompute the tier from points; returns true if it advanced. */
   public static boolean recomputeTier(PlayerPassData data) {
      int newTier = Math.max(0, Math.min(100, data.getPoints() / pointsPerTier()));
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
      if (amount <= 0) {
         return false;
      }

      ensureDaily(player.getUUID(), data);
      boolean tierChanged = false;

      for (Quest q : activeDaily(data)) {
         tierChanged |= progress(player, data, q, type, amount);
      }

      List<Quest> week = DefaultQuests.weekQuests(data.getCurrentWeek());
      for (Quest q : week) {
         tierChanged |= progress(player, data, q, type, amount);
      }

      boolean allDone = true;
      for (Quest q : week) {
         if (!data.isQuestClaimed(q.getId())) {
            allDone = false;
            break;
         }
      }

      if (allDone && data.getCurrentWeek() < DefaultQuests.weekCount()) {
         data.setCurrentWeek(data.getCurrentWeek() + 1);
         player.sendSystemMessage(Component.translatable("fantasticpass.msg.week_unlocked", data.getCurrentWeek()).withStyle(ChatFormatting.AQUA));
      }

      return tierChanged;
   }

   private static boolean progress(ServerPlayer player, PlayerPassData data, Quest quest, QuestType type, int amount) {
      if (quest.getType() != type || data.isQuestClaimed(quest.getId())) {
         return false;
      }

      int p = data.addQuestProgress(quest.getId(), amount);
      if (p >= quest.getTarget()) {
         data.markQuestClaimed(quest.getId());
         data.addPoints(quest.getPoints());
         player.sendSystemMessage(
            Component.translatable("fantasticpass.msg.quest_complete", quest.getDescription(), quest.getPoints()).withStyle(ChatFormatting.GREEN)
         );
         return recomputeTier(data);
      }

      return false;
   }
}
