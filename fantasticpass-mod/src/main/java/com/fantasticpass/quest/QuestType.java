package com.fantasticpass.quest;

/**
 * The kinds of objectives a quest can track. Each maps to a server-side event
 * hook and to a translation key used to describe it on the client. These are
 * deliberately distinct activities (not the same objective at different scales)
 * so the quest pool feels varied. PvP was intentionally removed (the server is
 * not anarchy).
 */
public enum QuestType {
   // Gathering / mining
   BREAK_BLOCKS("break_blocks"),
   MINE_STONE("mine_stone"),
   MINE_COAL("mine_coal"),
   MINE_IRON("mine_iron"),
   MINE_GOLD("mine_gold"),
   MINE_DIAMOND("mine_diamond"),
   MINE_REDSTONE("mine_redstone"),
   MINE_LAPIS("mine_lapis"),
   MINE_EMERALD("mine_emerald"),
   MINE_ORES("mine_ores"),
   CHOP_WOOD("chop_wood"),
   HARVEST_CROPS("harvest_crops"),
   PLACE_BLOCKS("place_blocks"),

   // Combat
   KILL_MONSTERS("kill_monsters"),
   KILL_ZOMBIES("kill_zombies"),
   KILL_SKELETONS("kill_skeletons"),
   KILL_CREEPERS("kill_creepers"),
   KILL_SPIDERS("kill_spiders"),
   KILL_ENDERMEN("kill_endermen"),
   KILL_ANIMALS("kill_animals"),

   // Activities
   CATCH_FISH("catch_fish"),
   EAT_FOOD("eat_food"),
   CRAFT_ITEMS("craft_items"),
   SMELT_ITEMS("smelt_items"),
   BREED_ANIMALS("breed_animals"),
   TAME_ANIMALS("tame_animals"),
   PLAY_MINUTES("play_minutes");

   private final String id;

   QuestType(String id) {
      this.id = id;
   }

   public String getId() {
      return this.id;
   }

   public String descriptionKey() {
      return "fantasticpass.quest." + this.id;
   }

   public static QuestType byName(String name) {
      try {
         return valueOf(name);
      } catch (IllegalArgumentException e) {
         return BREAK_BLOCKS;
      }
   }
}
