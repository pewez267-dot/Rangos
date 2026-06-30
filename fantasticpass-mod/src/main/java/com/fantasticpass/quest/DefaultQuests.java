package com.fantasticpass.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in quest content. The daily pool is intentionally huge so the four
 * quests drawn each day feel fresh for a long time; weekly quests are fixed per
 * week and escalate in difficulty. Point values are tuned so that several
 * quests are needed to advance a tier (a tier costs {@code POINTS_PER_TIER}
 * points, default 100), which keeps the progress bar meaningful.
 */
public final class DefaultQuests {
   public static final int DAILY_PER_DAY = 4;

   public static final List<Quest> DAILY_POOL;
   private static final List<List<Quest>> WEEKS = new ArrayList<>();

   static {
      List<Quest> daily = new ArrayList<>();
      // Each entry: target -> points. Points are modest so ~3-6 quests = one tier.
      addSet(daily, QuestType.BREAK_BLOCKS,
         new int[]{32, 64, 96, 128, 160, 200, 256, 320, 400, 512},
         new int[]{10, 12, 15, 18, 20, 24, 28, 32, 38, 45});
      addSet(daily, QuestType.MINE_ORES,
         new int[]{8, 16, 24, 32, 48, 64, 80, 96, 128},
         new int[]{12, 16, 20, 24, 30, 36, 42, 48, 56});
      addSet(daily, QuestType.KILL_MONSTERS,
         new int[]{10, 20, 30, 40, 60, 80, 100, 140},
         new int[]{12, 16, 20, 24, 30, 36, 42, 52});
      addSet(daily, QuestType.KILL_ANIMALS,
         new int[]{5, 10, 15, 20, 30, 40, 60},
         new int[]{10, 12, 15, 18, 24, 30, 38});
      addSet(daily, QuestType.KILL_PLAYERS,
         new int[]{1, 2, 3, 5, 8},
         new int[]{20, 30, 40, 55, 75});
      addSet(daily, QuestType.CATCH_FISH,
         new int[]{4, 8, 12, 16, 24, 32, 48},
         new int[]{10, 14, 18, 22, 28, 36, 46});
      addSet(daily, QuestType.PLAY_MINUTES,
         new int[]{10, 15, 20, 30, 45, 60, 90},
         new int[]{8, 12, 15, 20, 28, 36, 50});
      DAILY_POOL = Collections.unmodifiableList(daily);

      // 8 weeks of escalating weekly quests (points tuned so a week ~ a few tiers).
      for (int w = 1; w <= 8; w++) {
         int f = w; // difficulty factor
         List<Quest> week = List.of(
            new Quest("w" + w + "_break", QuestType.BREAK_BLOCKS, 400 + f * 120, 50 + f * 8),
            new Quest("w" + w + "_ore", QuestType.MINE_ORES, 80 + f * 30, 55 + f * 9),
            new Quest("w" + w + "_monsters", QuestType.KILL_MONSTERS, 120 + f * 50, 55 + f * 9),
            new Quest("w" + w + "_animals", QuestType.KILL_ANIMALS, 50 + f * 20, 45 + f * 8),
            new Quest("w" + w + "_players", QuestType.KILL_PLAYERS, 5 + f * 2, 70 + f * 10),
            new Quest("w" + w + "_fish", QuestType.CATCH_FISH, 35 + f * 12, 45 + f * 8),
            new Quest("w" + w + "_play", QuestType.PLAY_MINUTES, 180 + f * 30, 60 + f * 10)
         );
         WEEKS.add(week);
      }
   }

   private DefaultQuests() {
   }

   private static void addSet(List<Quest> out, QuestType type, int[] targets, int[] points) {
      for (int i = 0; i < targets.length; i++) {
         out.add(new Quest("d_" + type.getId() + "_" + targets[i], type, targets[i], points[i]));
      }
   }

   public static int weekCount() {
      return WEEKS.size();
   }

   /** Weekly quests for the given 1-based week (clamped to the available range). */
   public static List<Quest> weekQuests(int week) {
      int idx = Math.max(1, Math.min(WEEKS.size(), week)) - 1;
      return WEEKS.get(idx);
   }

   public static Quest byId(String id) {
      for (Quest q : DAILY_POOL) {
         if (q.getId().equals(id)) {
            return q;
         }
      }

      for (List<Quest> week : WEEKS) {
         for (Quest q : week) {
            if (q.getId().equals(id)) {
               return q;
            }
         }
      }

      return null;
   }
}
