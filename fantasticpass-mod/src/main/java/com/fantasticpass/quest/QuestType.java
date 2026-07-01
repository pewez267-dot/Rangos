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
   MINE_NETHERITE("mine_netherite"),
   MINE_QUARTZ("mine_quartz"),
   MINE_COPPER("mine_copper"),
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
   KILL_BLAZE("kill_blaze"),
   KILL_WITHER_SKELETONS("kill_wither_skeletons"),
   KILL_PIGLINS("kill_piglins"),
   KILL_SLIMES("kill_slimes"),
   KILL_MAGMA_CUBES("kill_magma_cubes"),
   KILL_GUARDIANS("kill_guardians"),
   KILL_PHANTOMS("kill_phantoms"),
   KILL_DROWNED("kill_drowned"),
   KILL_WITCHES("kill_witches"),
   KILL_PILLAGERS("kill_pillagers"),
   KILL_GHASTS("kill_ghasts"),
   KILL_HOGLINS("kill_hoglins"),
   KILL_VINDICATORS("kill_vindicators"),
   KILL_RAVAGERS("kill_ravagers"),
   KILL_EVOKERS("kill_evokers"),
   KILL_VEXES("kill_vexes"),
   KILL_ILLUSIONERS("kill_illusioners"),
   KILL_SHULKERS("kill_shulkers"),
   KILL_WARDENS("kill_wardens"),
   KILL_SILVERFISH("kill_silverfish"),
   KILL_ENDERMITES("kill_endermites"),
   KILL_STRAYS("kill_strays"),
   KILL_HUSKS("kill_husks"),
   KILL_ZOMBIE_VILLAGERS("kill_zombie_villagers"),
   KILL_CAVE_SPIDERS("kill_cave_spiders"),
   KILL_ZOMBIFIED_PIGLINS("kill_zombified_piglins"),
   KILL_WITHER("kill_wither"),
   KILL_ENDER_DRAGON("kill_ender_dragon"),
   KILL_BOSSES("kill_bosses"),

   // Activities
   CATCH_FISH("catch_fish"),
   EAT_FOOD("eat_food"),
   CRAFT_ITEMS("craft_items"),
   SMELT_ITEMS("smelt_items"),
   BREED_ANIMALS("breed_animals"),
   TAME_ANIMALS("tame_animals"),
   DEAL_DAMAGE("deal_damage"),
   GAIN_XP("gain_xp"),
   TRAVEL_BLOCKS("travel_blocks"),

   // Parameterized objectives (full mod compatibility: target any registered
   // entity / block / item, vanilla OR modded).
   KILL_ENTITY("kill_entity", ParamKind.ENTITY),
   MINE_BLOCK("mine_block", ParamKind.BLOCK),
   PLACE_BLOCK("place_block", ParamKind.BLOCK),
   CRAFT_ITEM("craft_item", ParamKind.ITEM),
   SMELT_ITEM("smelt_item", ParamKind.ITEM),
   EAT_ITEM("eat_item", ParamKind.ITEM),
   PICKUP_ITEM("pickup_item", ParamKind.ITEM),
   USE_ITEM("use_item", ParamKind.ITEM),
   TAME_ENTITY("tame_entity", ParamKind.ENTITY),
   BREED_ENTITY("breed_entity", ParamKind.ENTITY),

   PLAY_MINUTES("play_minutes");

   /** What kind of registry target (if any) a quest of this type carries. */
   public enum ParamKind {
      NONE,
      ENTITY,
      BLOCK,
      ITEM
   }

   private final String id;
   private final ParamKind paramKind;

   QuestType(String id) {
      this(id, ParamKind.NONE);
   }

   QuestType(String id, ParamKind paramKind) {
      this.id = id;
      this.paramKind = paramKind;
   }

   public ParamKind getParamKind() {
      return this.paramKind;
   }

   public boolean isParameterized() {
      return this.paramKind != ParamKind.NONE;
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
