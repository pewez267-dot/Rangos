package com.fantasticpass.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in quest content. Daily quests are drawn from {@link #DAILY_POOL} (a
 * fresh set each real day) and weekly quests are fixed per week in {@link #WEEKS}.
 * Points earned from quests feed the tier progression alongside active playtime.
 */
public final class DefaultQuests {
   public static final int DAILY_PER_DAY = 4;

   public static final List<Quest> DAILY_POOL = List.of(
      new Quest("d_break_64", QuestType.BREAK_BLOCKS, 64, 60),
      new Quest("d_break_128", QuestType.BREAK_BLOCKS, 128, 90),
      new Quest("d_ore_16", QuestType.MINE_ORES, 16, 80),
      new Quest("d_ore_32", QuestType.MINE_ORES, 32, 120),
      new Quest("d_mob_20", QuestType.KILL_MONSTERS, 20, 80),
      new Quest("d_mob_40", QuestType.KILL_MONSTERS, 40, 130),
      new Quest("d_animal_10", QuestType.KILL_ANIMALS, 10, 60),
      new Quest("d_fish_8", QuestType.CATCH_FISH, 8, 70),
      new Quest("d_play_20", QuestType.PLAY_MINUTES, 20, 60),
      new Quest("d_play_45", QuestType.PLAY_MINUTES, 45, 110)
   );

   private static final List<List<Quest>> WEEKS = new ArrayList<>();

   static {
      // 8 weeks of escalating weekly quests.
      for (int w = 1; w <= 8; w++) {
         int f = w; // difficulty factor
         List<Quest> week = List.of(
            new Quest("w" + w + "_break", QuestType.BREAK_BLOCKS, 300 + f * 100, 200 + f * 20),
            new Quest("w" + w + "_ore", QuestType.MINE_ORES, 64 + f * 24, 220 + f * 25),
            new Quest("w" + w + "_monsters", QuestType.KILL_MONSTERS, 100 + f * 40, 220 + f * 25),
            new Quest("w" + w + "_animals", QuestType.KILL_ANIMALS, 40 + f * 15, 180 + f * 20),
            new Quest("w" + w + "_players", QuestType.KILL_PLAYERS, 5 + f, 300 + f * 30),
            new Quest("w" + w + "_fish", QuestType.CATCH_FISH, 30 + f * 10, 180 + f * 20),
            new Quest("w" + w + "_play", QuestType.PLAY_MINUTES, 180 + f * 30, 260 + f * 25)
         );
         WEEKS.add(week);
      }
   }

   private DefaultQuests() {
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
