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
      // Mining / gathering (demanding on purpose).
      free.add(q("df_break", QuestType.BREAK_BLOCKS, 300, 10));
      free.add(q("df_break2", QuestType.BREAK_BLOCKS, 500, 14));
      free.add(q("df_break3", QuestType.BREAK_BLOCKS, 900, 18));
      free.add(q("df_stone", QuestType.MINE_STONE, 400, 10));
      free.add(q("df_stone2", QuestType.MINE_STONE, 800, 15));
      free.add(q("df_coal", QuestType.MINE_COAL, 64, 12));
      free.add(q("df_coal2", QuestType.MINE_COAL, 128, 15));
      free.add(q("df_iron", QuestType.MINE_IRON, 48, 14));
      free.add(q("df_iron2", QuestType.MINE_IRON, 96, 18));
      free.add(q("df_copper", QuestType.MINE_COPPER, 96, 11));
      free.add(q("df_gold", QuestType.MINE_GOLD, 32, 13));
      free.add(q("df_redstone", QuestType.MINE_REDSTONE, 64, 12));
      free.add(q("df_lapis", QuestType.MINE_LAPIS, 32, 12));
      free.add(q("df_ores", QuestType.MINE_ORES, 96, 13));
      free.add(q("df_ores2", QuestType.MINE_ORES, 180, 18));
      free.add(q("df_wood", QuestType.CHOP_WOOD, 128, 11));
      free.add(q("df_wood2", QuestType.CHOP_WOOD, 256, 15));
      free.add(q("df_crops", QuestType.HARVEST_CROPS, 120, 12));
      free.add(q("df_crops2", QuestType.HARVEST_CROPS, 240, 15));
      free.add(q("df_place", QuestType.PLACE_BLOCKS, 300, 9));
      free.add(q("df_place2", QuestType.PLACE_BLOCKS, 600, 13));
      // Combat.
      free.add(q("df_monsters", QuestType.KILL_MONSTERS, 70, 12));
      free.add(q("df_monsters2", QuestType.KILL_MONSTERS, 140, 16));
      free.add(q("df_monsters3", QuestType.KILL_MONSTERS, 240, 22));
      free.add(q("df_zombies", QuestType.KILL_ZOMBIES, 50, 11));
      free.add(q("df_skeletons", QuestType.KILL_SKELETONS, 40, 11));
      free.add(q("df_spiders", QuestType.KILL_SPIDERS, 32, 10));
      free.add(q("df_creepers", QuestType.KILL_CREEPERS, 20, 14));
      free.add(q("df_animals", QuestType.KILL_ANIMALS, 40, 10));
      free.add(q("df_drowned", QuestType.KILL_DROWNED, 30, 12));
      free.add(q("df_phantoms", QuestType.KILL_PHANTOMS, 14, 13));
      free.add(q("df_slimes", QuestType.KILL_SLIMES, 32, 11));
      free.add(q("df_pillagers", QuestType.KILL_PILLAGERS, 20, 13));
      free.add(q("df_husks", QuestType.KILL_HUSKS, 24, 12));
      free.add(q("df_strays", QuestType.KILL_STRAYS, 24, 12));
      free.add(q("df_silverfish", QuestType.KILL_SILVERFISH, 40, 11));
      free.add(q("df_zombie_villagers", QuestType.KILL_ZOMBIE_VILLAGERS, 16, 13));
      free.add(q("df_cave_spiders", QuestType.KILL_CAVE_SPIDERS, 32, 12));
      free.add(q("df_endermen", QuestType.KILL_ENDERMEN, 12, 15));
      free.add(q("df_witches", QuestType.KILL_WITCHES, 10, 15));
      // Activities.
      free.add(q("df_fish", QuestType.CATCH_FISH, 30, 12));
      free.add(q("df_fish2", QuestType.CATCH_FISH, 60, 15));
      free.add(q("df_eat", QuestType.EAT_FOOD, 40, 8));
      free.add(q("df_craft", QuestType.CRAFT_ITEMS, 64, 9));
      free.add(q("df_craft2", QuestType.CRAFT_ITEMS, 128, 13));
      free.add(q("df_smelt", QuestType.SMELT_ITEMS, 64, 10));
      free.add(q("df_smelt2", QuestType.SMELT_ITEMS, 128, 13));
      free.add(q("df_breed", QuestType.BREED_ANIMALS, 12, 12));
      free.add(q("df_tame", QuestType.TAME_ANIMALS, 3, 13));
      free.add(q("df_travel", QuestType.TRAVEL_BLOCKS, 2500, 10));
      free.add(q("df_travel2", QuestType.TRAVEL_BLOCKS, 5000, 15));
      free.add(q("df_sprint", QuestType.SPRINT_BLOCKS, 1500, 11));
      free.add(q("df_sprint2", QuestType.SPRINT_BLOCKS, 3000, 15));
      free.add(q("df_swim", QuestType.SWIM_BLOCKS, 400, 12));
      free.add(q("df_swim2", QuestType.SWIM_BLOCKS, 900, 15));
      free.add(q("df_damage", QuestType.DEAL_DAMAGE, 700, 10));
      free.add(q("df_damage2", QuestType.DEAL_DAMAGE, 1400, 15));
      free.add(q("df_take", QuestType.TAKE_DAMAGE, 200, 9));
      free.add(q("df_xp", QuestType.GAIN_XP, 250, 10));
      free.add(q("df_xp2", QuestType.GAIN_XP, 500, 15));
      free.add(q("df_jump", QuestType.JUMP, 500, 8));
      free.add(q("df_jump2", QuestType.JUMP, 1200, 12));
      DAILY_FREE_POOL = Collections.unmodifiableList(free);

      // ---- PREMIUM daily pool (richer / rarer objectives) ----
      List<Quest> prem = new ArrayList<>();
      // Valuable mining.
      prem.add(q("dp_diamond", QuestType.MINE_DIAMOND, 16, 24));
      prem.add(q("dp_diamond2", QuestType.MINE_DIAMOND, 32, 30));
      prem.add(q("dp_emerald", QuestType.MINE_EMERALD, 10, 24));
      prem.add(q("dp_gold", QuestType.MINE_GOLD, 48, 16));
      prem.add(q("dp_redstone", QuestType.MINE_REDSTONE, 96, 14));
      prem.add(q("dp_lapis", QuestType.MINE_LAPIS, 48, 14));
      prem.add(q("dp_netherite", QuestType.MINE_NETHERITE, 4, 34));
      prem.add(q("dp_netherite2", QuestType.MINE_NETHERITE, 8, 46));
      prem.add(q("dp_quartz", QuestType.MINE_QUARTZ, 256, 16));
      prem.add(q("dp_ores", QuestType.MINE_ORES, 256, 18));
      prem.add(q("dp_ores2", QuestType.MINE_ORES, 400, 24));
      // Combat (harder).
      prem.add(q("dp_creepers", QuestType.KILL_CREEPERS, 25, 16));
      prem.add(q("dp_endermen", QuestType.KILL_ENDERMEN, 20, 20));
      prem.add(q("dp_monsters", QuestType.KILL_MONSTERS, 150, 18));
      prem.add(q("dp_monsters2", QuestType.KILL_MONSTERS, 280, 26));
      prem.add(q("dp_blaze", QuestType.KILL_BLAZE, 40, 22));
      prem.add(q("dp_wither_skel", QuestType.KILL_WITHER_SKELETONS, 28, 26));
      prem.add(q("dp_piglins", QuestType.KILL_PIGLINS, 50, 20));
      prem.add(q("dp_guardians", QuestType.KILL_GUARDIANS, 28, 24));
      prem.add(q("dp_witches", QuestType.KILL_WITCHES, 28, 20));
      prem.add(q("dp_pillagers", QuestType.KILL_PILLAGERS, 50, 18));
      prem.add(q("dp_ghasts", QuestType.KILL_GHASTS, 16, 26));
      prem.add(q("dp_hoglins", QuestType.KILL_HOGLINS, 28, 22));
      prem.add(q("dp_vindicators", QuestType.KILL_VINDICATORS, 32, 22));
      prem.add(q("dp_magma", QuestType.KILL_MAGMA_CUBES, 50, 18));
      prem.add(q("dp_ravagers", QuestType.KILL_RAVAGERS, 10, 30));
      prem.add(q("dp_evokers", QuestType.KILL_EVOKERS, 10, 32));
      prem.add(q("dp_shulkers", QuestType.KILL_SHULKERS, 20, 26));
      prem.add(q("dp_zpiglins", QuestType.KILL_ZOMBIFIED_PIGLINS, 60, 18));
      prem.add(q("dp_cave_spiders", QuestType.KILL_CAVE_SPIDERS, 50, 18));
      prem.add(q("dp_phantoms", QuestType.KILL_PHANTOMS, 24, 22));
      prem.add(q("dp_slimes", QuestType.KILL_SLIMES, 60, 16));
      prem.add(q("dp_vexes", QuestType.KILL_VEXES, 16, 26));
      prem.add(q("dp_illusioners", QuestType.KILL_ILLUSIONERS, 8, 30));
      prem.add(q("dp_endermites", QuestType.KILL_ENDERMITES, 24, 20));
      // Activities.
      prem.add(q("dp_fish", QuestType.CATCH_FISH, 60, 16));
      prem.add(q("dp_wood", QuestType.CHOP_WOOD, 300, 14));
      prem.add(q("dp_crops", QuestType.HARVEST_CROPS, 256, 16));
      prem.add(q("dp_tame", QuestType.TAME_ANIMALS, 6, 20));
      prem.add(q("dp_breed", QuestType.BREED_ANIMALS, 30, 16));
      prem.add(q("dp_smelt", QuestType.SMELT_ITEMS, 160, 16));
      prem.add(q("dp_craft", QuestType.CRAFT_ITEMS, 160, 14));
      prem.add(q("dp_travel", QuestType.TRAVEL_BLOCKS, 6000, 18));
      prem.add(q("dp_travel2", QuestType.TRAVEL_BLOCKS, 12000, 24));
      prem.add(q("dp_sprint", QuestType.SPRINT_BLOCKS, 4000, 18));
      prem.add(q("dp_swim", QuestType.SWIM_BLOCKS, 1500, 18));
      prem.add(q("dp_damage", QuestType.DEAL_DAMAGE, 2500, 18));
      prem.add(q("dp_damage2", QuestType.DEAL_DAMAGE, 4500, 24));
      prem.add(q("dp_take", QuestType.TAKE_DAMAGE, 600, 16));
      prem.add(q("dp_xp", QuestType.GAIN_XP, 1000, 16));
      prem.add(q("dp_xp2", QuestType.GAIN_XP, 2000, 22));
      prem.add(q("dp_jump", QuestType.JUMP, 2500, 14));
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
            q("wp1_ores", QuestType.MINE_ORES, 96, 36),
            q("wp1_iron", QuestType.MINE_IRON, 48, 34),
            q("wp1_gold", QuestType.MINE_GOLD, 24, 34),
            q("wp1_netherite", QuestType.MINE_NETHERITE, 3, 46)
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
            q("wp2_blaze", QuestType.KILL_BLAZE, 16, 40),
            q("wp2_witches", QuestType.KILL_WITCHES, 12, 40),
            q("wp2_pillagers", QuestType.KILL_PILLAGERS, 20, 38)
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
            q("wp3_breed", QuestType.BREED_ANIMALS, 32, 38),
            q("wp3_eat", QuestType.EAT_FOOD, 48, 34),
            q("wp3_crops", QuestType.HARVEST_CROPS, 160, 36),
            q("wp3_fish", QuestType.CATCH_FISH, 48, 36)
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
            q("wp4_netherite", QuestType.MINE_NETHERITE, 4, 48),
            q("wp4_gold", QuestType.MINE_GOLD, 48, 36),
            q("wp4_quartz", QuestType.MINE_QUARTZ, 96, 34)
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
            q("wp5_smelt", QuestType.SMELT_ITEMS, 128, 38),
            q("wp5_craft", QuestType.CRAFT_ITEMS, 128, 36),
            q("wp5_eat", QuestType.EAT_FOOD, 64, 34),
            q("wp5_crops", QuestType.HARVEST_CROPS, 128, 36)
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
            q("wp6_ghasts", QuestType.KILL_GHASTS, 8, 44),
            q("wp6_skeletons", QuestType.KILL_SKELETONS, 60, 34),
            q("wp6_creepers", QuestType.KILL_CREEPERS, 24, 38)
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
            q("wp7_ores", QuestType.MINE_ORES, 128, 38),
            q("wp7_wood", QuestType.CHOP_WOOD, 256, 34),
            q("wp7_stone", QuestType.MINE_STONE, 512, 34),
            q("wp7_craft", QuestType.CRAFT_ITEMS, 128, 36)
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
            q("wp8_wither_skel", QuestType.KILL_WITHER_SKELETONS, 16, 50),
            q("wp8_diamond", QuestType.MINE_DIAMOND, 32, 44),
            q("wp8_monsters", QuestType.KILL_MONSTERS, 120, 40)
         )
      );
      // Week 9 — Nether
      addWeek(
         List.of(
            q("wf9_quartz", QuestType.MINE_QUARTZ, 96, 30),
            q("wf9_piglins", QuestType.KILL_PIGLINS, 20, 30),
            q("wf9_blaze", QuestType.KILL_BLAZE, 16, 32),
            q("wf9_magma", QuestType.KILL_MAGMA_CUBES, 24, 28),
            q("wf9_gold", QuestType.MINE_GOLD, 32, 28)
         ),
         List.of(
            q("wp9_wither_skel", QuestType.KILL_WITHER_SKELETONS, 12, 42),
            q("wp9_ghasts", QuestType.KILL_GHASTS, 10, 44),
            q("wp9_hoglins", QuestType.KILL_HOGLINS, 16, 40),
            q("wp9_netherite", QuestType.MINE_NETHERITE, 4, 48),
            q("wp9_zpiglins", QuestType.KILL_ZOMBIFIED_PIGLINS, 40, 36)
         )
      );
      // Week 10 — Explorer
      addWeek(
         List.of(
            q("wf10_travel", QuestType.TRAVEL_BLOCKS, 4000, 30),
            q("wf10_fish", QuestType.CATCH_FISH, 48, 28),
            q("wf10_drowned", QuestType.KILL_DROWNED, 24, 28),
            q("wf10_phantoms", QuestType.KILL_PHANTOMS, 12, 30),
            q("wf10_break", QuestType.BREAK_BLOCKS, 512, 26)
         ),
         List.of(
            q("wp10_travel", QuestType.TRAVEL_BLOCKS, 10000, 42),
            q("wp10_guardians", QuestType.KILL_GUARDIANS, 12, 42),
            q("wp10_pillagers", QuestType.KILL_PILLAGERS, 24, 38),
            q("wp10_endermen", QuestType.KILL_ENDERMEN, 16, 42),
            q("wp10_shulkers", QuestType.KILL_SHULKERS, 8, 46)
         )
      );
      // Week 11 — Battlefield
      addWeek(
         List.of(
            q("wf11_zombies", QuestType.KILL_ZOMBIES, 60, 28),
            q("wf11_skeletons", QuestType.KILL_SKELETONS, 40, 28),
            q("wf11_spiders", QuestType.KILL_SPIDERS, 30, 28),
            q("wf11_creepers", QuestType.KILL_CREEPERS, 20, 30),
            q("wf11_damage", QuestType.DEAL_DAMAGE, 500, 28)
         ),
         List.of(
            q("wp11_ravagers", QuestType.KILL_RAVAGERS, 8, 44),
            q("wp11_evokers", QuestType.KILL_EVOKERS, 8, 46),
            q("wp11_vindicators", QuestType.KILL_VINDICATORS, 24, 40),
            q("wp11_witches", QuestType.KILL_WITCHES, 20, 40),
            q("wp11_warden", QuestType.KILL_WARDENS, 1, 60)
         )
      );
      // Week 12 — Grind
      addWeek(
         List.of(
            q("wf12_xp", QuestType.GAIN_XP, 500, 30),
            q("wf12_craft", QuestType.CRAFT_ITEMS, 128, 28),
            q("wf12_smelt", QuestType.SMELT_ITEMS, 128, 28),
            q("wf12_place", QuestType.PLACE_BLOCKS, 512, 28),
            q("wf12_stone", QuestType.MINE_STONE, 512, 28)
         ),
         List.of(
            q("wp12_xp", QuestType.GAIN_XP, 1500, 44),
            q("wp12_diamond", QuestType.MINE_DIAMOND, 32, 44),
            q("wp12_emerald", QuestType.MINE_EMERALD, 16, 44),
            q("wp12_monsters", QuestType.KILL_MONSTERS, 120, 40),
            q("wp12_travel", QuestType.TRAVEL_BLOCKS, 8000, 42)
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

   /** Week count honouring a per-pass override (0 = use global config). Unlimited up to 52. */
   public static int effectiveWeekCount(PassDefinition pass) {
      int override = pass == null ? 0 : pass.getWeekCountOverride();
      int cfg = override > 0 ? override : PassConfig.WEEK_COUNT.get();
      return Math.max(1, Math.min(PassDefinition.MAX_WEEKS, cfg));
   }

   /** Highest distinct themed week the content ships with (8). */
   public static int maxWeeks() {
      return WEEKS_FREE.size();
   }

   /** Free weekly quests, cycling the 8 themed sets for weeks beyond 8. */
   public static List<Quest> weekQuestsCyclic(int week) {
      int idx = (Math.max(1, week) - 1) % WEEKS_FREE.size();
      return WEEKS_FREE.get(idx);
   }

   /** Premium weekly quests, cycling the 8 themed sets for weeks beyond 8. */
   public static List<Quest> premiumWeekQuestsCyclic(int week) {
      int idx = (Math.max(1, week) - 1) % WEEKS_PREMIUM.size();
      return WEEKS_PREMIUM.get(idx);
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
