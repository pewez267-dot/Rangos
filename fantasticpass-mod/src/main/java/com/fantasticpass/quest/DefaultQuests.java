package com.fantasticpass.quest;

import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in quest content. Everything here is a DISTINCT objective (never the
 * same task at a bigger number): there are separate free and premium daily
 * pools, and eight themed weeks whose quests differ from each other and do not
 * scale exponentially. Point values are tuned against {@code POINTS_PER_TIER}
 * (default 100) so that several quests are needed to advance one tier.
 */
public final class DefaultQuests {
   /** Distinct daily quests every player can roll. */
   public static final List<Quest> DAILY_FREE_POOL;
   /** Extra distinct daily quests only premium players roll (bonus track). */
   public static final List<Quest> DAILY_PREMIUM_POOL;

   private static final List<List<Quest>> WEEKS_FREE = new ArrayList<>();
   private static final List<List<Quest>> WEEKS_PREMIUM = new ArrayList<>();

   static {
      // ---- FREE daily pool (accessible, low point values) ----
      List<Quest> free = new ArrayList<>();
      free.add(q("df_break", QuestType.BREAK_BLOCKS, 64, 10));
      free.add(q("df_stone", QuestType.MINE_STONE, 96, 9));
      free.add(q("df_coal", QuestType.MINE_COAL, 16, 11));
      free.add(q("df_iron", QuestType.MINE_IRON, 12, 13));
      free.add(q("df_wood", QuestType.CHOP_WOOD, 32, 10));
      free.add(q("df_crops", QuestType.HARVEST_CROPS, 24, 12));
      free.add(q("df_place", QuestType.PLACE_BLOCKS, 64, 8));
      free.add(q("df_monsters", QuestType.KILL_MONSTERS, 20, 12));
      free.add(q("df_zombies", QuestType.KILL_ZOMBIES, 12, 11));
      free.add(q("df_skeletons", QuestType.KILL_SKELETONS, 10, 11));
      free.add(q("df_spiders", QuestType.KILL_SPIDERS, 8, 10));
      free.add(q("df_animals", QuestType.KILL_ANIMALS, 10, 10));
      free.add(q("df_fish", QuestType.CATCH_FISH, 8, 12));
      free.add(q("df_eat", QuestType.EAT_FOOD, 10, 8));
      free.add(q("df_craft", QuestType.CRAFT_ITEMS, 16, 8));
      free.add(q("df_smelt", QuestType.SMELT_ITEMS, 16, 10));
      free.add(q("df_breed", QuestType.BREED_ANIMALS, 4, 12));
      free.add(q("df_ores", QuestType.MINE_ORES, 24, 12));
      free.add(q("df_copper", QuestType.MINE_COPPER, 24, 10));
      free.add(q("df_drowned", QuestType.KILL_DROWNED, 8, 12));
      free.add(q("df_phantoms", QuestType.KILL_PHANTOMS, 5, 13));
      free.add(q("df_slimes", QuestType.KILL_SLIMES, 10, 11));
      free.add(q("df_pillagers", QuestType.KILL_PILLAGERS, 6, 13));
      free.add(q("df_place_more", QuestType.PLACE_BLOCKS, 128, 11));
      free.add(q("df_break_more", QuestType.BREAK_BLOCKS, 128, 12));
      free.add(q("df_iron_more", QuestType.MINE_IRON, 24, 14));
      DAILY_FREE_POOL = Collections.unmodifiableList(free);

      // ---- PREMIUM daily pool (richer / rarer objectives) ----
      List<Quest> prem = new ArrayList<>();
      prem.add(q("dp_diamond", QuestType.MINE_DIAMOND, 5, 22));
      prem.add(q("dp_gold", QuestType.MINE_GOLD, 8, 16));
      prem.add(q("dp_redstone", QuestType.MINE_REDSTONE, 16, 14));
      prem.add(q("dp_lapis", QuestType.MINE_LAPIS, 8, 14));
      prem.add(q("dp_emerald", QuestType.MINE_EMERALD, 3, 22));
      prem.add(q("dp_creepers", QuestType.KILL_CREEPERS, 6, 16));
      prem.add(q("dp_endermen", QuestType.KILL_ENDERMEN, 5, 20));
      prem.add(q("dp_monsters", QuestType.KILL_MONSTERS, 40, 18));
      prem.add(q("dp_fish", QuestType.CATCH_FISH, 16, 16));
      prem.add(q("dp_wood", QuestType.CHOP_WOOD, 64, 14));
      prem.add(q("dp_crops", QuestType.HARVEST_CROPS, 48, 16));
      prem.add(q("dp_tame", QuestType.TAME_ANIMALS, 2, 18));
      prem.add(q("dp_breed", QuestType.BREED_ANIMALS, 8, 16));
      prem.add(q("dp_smelt", QuestType.SMELT_ITEMS, 32, 16));
      prem.add(q("dp_craft", QuestType.CRAFT_ITEMS, 32, 14));
      prem.add(q("dp_blaze", QuestType.KILL_BLAZE, 12, 22));
      prem.add(q("dp_wither_skel", QuestType.KILL_WITHER_SKELETONS, 8, 26));
      prem.add(q("dp_piglins", QuestType.KILL_PIGLINS, 14, 20));
      prem.add(q("dp_guardians", QuestType.KILL_GUARDIANS, 8, 24));
      prem.add(q("dp_witches", QuestType.KILL_WITCHES, 8, 20));
      prem.add(q("dp_pillagers", QuestType.KILL_PILLAGERS, 16, 18));
      prem.add(q("dp_ghasts", QuestType.KILL_GHASTS, 5, 26));
      prem.add(q("dp_hoglins", QuestType.KILL_HOGLINS, 8, 22));
      prem.add(q("dp_vindicators", QuestType.KILL_VINDICATORS, 10, 22));
      prem.add(q("dp_magma", QuestType.KILL_MAGMA_CUBES, 16, 18));
      prem.add(q("dp_netherite", QuestType.MINE_NETHERITE, 2, 30));
      prem.add(q("dp_quartz", QuestType.MINE_QUARTZ, 64, 16));
      DAILY_PREMIUM_POOL = Collections.unmodifiableList(prem);

      // ---- 8 themed weeks. Distinct objectives, NON-exponential points. ----
      // Week 1 — Mining
      addWeek(
         List.of(
            q("wf1_stone", QuestType.MINE_STONE, 128, 28),
            q("wf1_coal", QuestType.MINE_COAL, 48, 30),
            q("wf1_iron", QuestType.MINE_IRON, 24, 32),
            q("wf1_break", QuestType.BREAK_BLOCKS, 256, 26),
            q("wf1_wood", QuestType.CHOP_WOOD, 64, 28)
         ),
         List.of(
            q("wp1_diamond", QuestType.MINE_DIAMOND, 12, 40),
            q("wp1_ores", QuestType.MINE_ORES, 96, 36)
         )
      );
      // Week 2 — Combat
      addWeek(
         List.of(
            q("wf2_monsters", QuestType.KILL_MONSTERS, 60, 30),
            q("wf2_zombies", QuestType.KILL_ZOMBIES, 30, 28),
            q("wf2_skeletons", QuestType.KILL_SKELETONS, 20, 30),
            q("wf2_spiders", QuestType.KILL_SPIDERS, 18, 28),
            q("wf2_animals", QuestType.KILL_ANIMALS, 30, 26)
         ),
         List.of(
            q("wp2_creepers", QuestType.KILL_CREEPERS, 15, 38),
            q("wp2_endermen", QuestType.KILL_ENDERMEN, 10, 42),
            q("wp2_blaze", QuestType.KILL_BLAZE, 16, 40)
         )
      );
      // Week 3 — Farming & ranching
      addWeek(
         List.of(
            q("wf3_crops", QuestType.HARVEST_CROPS, 128, 30),
            q("wf3_breed", QuestType.BREED_ANIMALS, 16, 30),
            q("wf3_eat", QuestType.EAT_FOOD, 32, 26),
            q("wf3_animals", QuestType.KILL_ANIMALS, 24, 26),
            q("wf3_place", QuestType.PLACE_BLOCKS, 128, 26)
         ),
         List.of(
            q("wp3_tame", QuestType.TAME_ANIMALS, 3, 40),
            q("wp3_breed", QuestType.BREED_ANIMALS, 32, 38)
         )
      );
      // Week 4 — Treasures
      addWeek(
         List.of(
            q("wf4_gold", QuestType.MINE_GOLD, 24, 30),
            q("wf4_redstone", QuestType.MINE_REDSTONE, 48, 28),
            q("wf4_lapis", QuestType.MINE_LAPIS, 32, 28),
            q("wf4_ores", QuestType.MINE_ORES, 64, 30),
            q("wf4_coal", QuestType.MINE_COAL, 96, 26)
         ),
         List.of(
            q("wp4_diamond", QuestType.MINE_DIAMOND, 16, 42),
            q("wp4_emerald", QuestType.MINE_EMERALD, 8, 42),
            q("wp4_netherite", QuestType.MINE_NETHERITE, 4, 48)
         )
      );
      // Week 5 — Fishing & cooking
      addWeek(
         List.of(
            q("wf5_fish", QuestType.CATCH_FISH, 48, 30),
            q("wf5_smelt", QuestType.SMELT_ITEMS, 64, 28),
            q("wf5_craft", QuestType.CRAFT_ITEMS, 64, 28),
            q("wf5_eat", QuestType.EAT_FOOD, 48, 28),
            q("wf5_crops", QuestType.HARVEST_CROPS, 96, 28)
         ),
         List.of(
            q("wp5_fish", QuestType.CATCH_FISH, 96, 40),
            q("wp5_smelt", QuestType.SMELT_ITEMS, 128, 38)
         )
      );
      // Week 6 — Hunter
      addWeek(
         List.of(
            q("wf6_skeletons", QuestType.KILL_SKELETONS, 30, 30),
            q("wf6_spiders", QuestType.KILL_SPIDERS, 24, 28),
            q("wf6_zombies", QuestType.KILL_ZOMBIES, 40, 28),
            q("wf6_monsters", QuestType.KILL_MONSTERS, 50, 30),
            q("wf6_animals", QuestType.KILL_ANIMALS, 40, 26)
         ),
         List.of(
            q("wp6_witches", QuestType.KILL_WITCHES, 14, 40),
            q("wp6_pillagers", QuestType.KILL_PILLAGERS, 24, 38),
            q("wp6_ghasts", QuestType.KILL_GHASTS, 8, 44)
         )
      );
      // Week 7 — Builder
      addWeek(
         List.of(
            q("wf7_place", QuestType.PLACE_BLOCKS, 512, 30),
            q("wf7_wood", QuestType.CHOP_WOOD, 128, 28),
            q("wf7_stone", QuestType.MINE_STONE, 256, 28),
            q("wf7_craft", QuestType.CRAFT_ITEMS, 96, 28),
            q("wf7_smelt", QuestType.SMELT_ITEMS, 96, 28)
         ),
         List.of(
            q("wp7_place", QuestType.PLACE_BLOCKS, 1024, 42),
            q("wp7_ores", QuestType.MINE_ORES, 128, 38)
         )
      );
      // Week 8 — Mastery
      addWeek(
         List.of(
            q("wf8_diamond", QuestType.MINE_DIAMOND, 24, 34),
            q("wf8_monsters", QuestType.KILL_MONSTERS, 80, 32),
            q("wf8_crops", QuestType.HARVEST_CROPS, 160, 30),
            q("wf8_fish", QuestType.CATCH_FISH, 64, 30),
            q("wf8_breed", QuestType.BREED_ANIMALS, 24, 30)
         ),
         List.of(
            q("wp8_emerald", QuestType.MINE_EMERALD, 16, 45),
            q("wp8_endermen", QuestType.KILL_ENDERMEN, 20, 45),
            q("wp8_wither_skel", QuestType.KILL_WITHER_SKELETONS, 16, 50)
         )
      );
   }

   private DefaultQuests() {
   }

   private static Quest q(String id, QuestType type, int target, int points) {
      return new Quest(id, type, target, points);
   }

   private static void addWeek(List<Quest> free, List<Quest> premium) {
      WEEKS_FREE.add(free);
      WEEKS_PREMIUM.add(premium);
   }

   /** Total number of distinct themed weeks available (capped by config). */
   public static int weekCount() {
      int cfg = PassConfig.WEEK_COUNT.get();
      return Math.max(1, Math.min(WEEKS_FREE.size(), cfg));
   }

   /** Week count honouring a per-pass override (0 = use global config). */
   public static int effectiveWeekCount(PassDefinition pass) {
      int override = pass == null ? 0 : pass.getWeekCountOverride();
      int cfg = override > 0 ? override : PassConfig.WEEK_COUNT.get();
      return Math.max(1, Math.min(WEEKS_FREE.size(), cfg));
   }

   /** Highest distinct themed week the content ships with (8). */
   public static int maxWeeks() {
      return WEEKS_FREE.size();
   }

   /** Free weekly quests for the given 1-based week (clamped). */
   public static List<Quest> weekQuests(int week) {
      int idx = Math.max(1, Math.min(WEEKS_FREE.size(), week)) - 1;
      return WEEKS_FREE.get(idx);
   }

   /** Premium-only weekly quests for the given 1-based week (clamped). */
   public static List<Quest> premiumWeekQuests(int week) {
      int idx = Math.max(1, Math.min(WEEKS_PREMIUM.size(), week)) - 1;
      return WEEKS_PREMIUM.get(idx);
   }

   /** All quests active for a player on a given week, optionally including premium. */
   public static List<Quest> allWeekQuests(int week, boolean premium) {
      List<Quest> list = new ArrayList<>(weekQuests(week));
      if (premium) {
         list.addAll(premiumWeekQuests(week));
      }
      return list;
   }

   public static Quest byId(String id) {
      if (id == null) {
         return null;
      }

      for (Quest qq : DAILY_FREE_POOL) {
         if (qq.getId().equals(id)) {
            return qq;
         }
      }
      for (Quest qq : DAILY_PREMIUM_POOL) {
         if (qq.getId().equals(id)) {
            return qq;
         }
      }
      for (List<Quest> week : WEEKS_FREE) {
         for (Quest qq : week) {
            if (qq.getId().equals(id)) {
               return qq;
            }
         }
      }
      for (List<Quest> week : WEEKS_PREMIUM) {
         for (Quest qq : week) {
            if (qq.getId().equals(id)) {
               return qq;
            }
         }
      }

      return null;
   }
}
