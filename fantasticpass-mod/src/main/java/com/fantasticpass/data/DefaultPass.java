package com.fantasticpass.data;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Builds a fully populated default battle pass so rewards are visible the
 * moment the mod is installed, without an admin having to configure 100 tiers
 * by hand. Faithful to the Castle pack philosophy of shipping with content:
 * every tier has free + premium item rewards, and milestone tiers grant ranks.
 */
public final class DefaultPass {
   public static final String DEFAULT_ID = "castle";
   public static final String DEFAULT_NAME = "Castle";

   private DefaultPass() {
   }

   // Free reward pools per progression band (early -> late game).
   private static final Item[] FREE_EARLY = {Items.BREAD, Items.COOKED_BEEF, Items.COAL, Items.OAK_LOG, Items.ARROW, Items.IRON_NUGGET, Items.APPLE, Items.TORCH};
   private static final Item[] FREE_MID = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.EXPERIENCE_BOTTLE, Items.ENDER_PEARL, Items.LAPIS_LAZULI, Items.REDSTONE, Items.IRON_BLOCK, Items.COOKED_PORKCHOP};
   private static final Item[] FREE_LATE = {Items.DIAMOND, Items.EMERALD, Items.GOLDEN_APPLE, Items.GOLD_BLOCK, Items.OBSIDIAN, Items.NETHERITE_SCRAP, Items.QUARTZ_BLOCK, Items.AMETHYST_SHARD};
   private static final Item[] FREE_END = {Items.DIAMOND_BLOCK, Items.NETHERITE_INGOT, Items.ENCHANTED_GOLDEN_APPLE, Items.EMERALD_BLOCK, Items.EXPERIENCE_BOTTLE, Items.ENDER_EYE, Items.DIAMOND, Items.GOLDEN_CARROT};

   // Premium reward pools per band (richer than free).
   private static final Item[] PREM_EARLY = {Items.IRON_INGOT, Items.GOLDEN_APPLE, Items.EXPERIENCE_BOTTLE, Items.LAPIS_BLOCK, Items.IRON_BLOCK, Items.COOKED_BEEF, Items.ENDER_PEARL, Items.GOLD_INGOT};
   private static final Item[] PREM_MID = {Items.DIAMOND, Items.EMERALD, Items.GOLD_BLOCK, Items.ENCHANTED_BOOK, Items.OBSIDIAN, Items.BLAZE_ROD, Items.DIAMOND, Items.GOLDEN_CARROT};
   private static final Item[] PREM_LATE = {Items.DIAMOND_BLOCK, Items.NETHERITE_SCRAP, Items.EMERALD_BLOCK, Items.ENCHANTED_GOLDEN_APPLE, Items.TOTEM_OF_UNDYING, Items.SHULKER_BOX, Items.DIAMOND_BLOCK, Items.NETHER_STAR};
   private static final Item[] PREM_END = {Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, Items.ELYTRA, Items.BEACON, Items.NETHERITE_INGOT, Items.ENCHANTED_GOLDEN_APPLE, Items.DRAGON_HEAD, Items.NETHER_STAR};

   /**
    * Build the populated 100-tier default pass.
    */
   public static PassDefinition build() {
      PassDefinition pass = new PassDefinition(DEFAULT_ID, DEFAULT_NAME);

      for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
         TierDefinition def = new TierDefinition(tier);
         populateFree(def, tier);
         populatePremium(def, tier);
         applyMilestone(def, tier);
         pass.setTier(tier, def);
      }

      return pass;
   }

   private static void populateFree(TierDefinition def, int tier) {
      Item[] pool = poolFor(tier, FREE_EARLY, FREE_MID, FREE_LATE, FREE_END);
      Item primary = pool[tier % pool.length];
      def.getFreeRewards().add(new ItemStack(primary, freeCount(tier)));

      // Every 5th free tier also grants a small bonus stack for variety.
      if (tier % 5 == 0) {
         Item bonus = pool[(tier + 3) % pool.length];
         def.getFreeRewards().add(new ItemStack(bonus, 2));
      }
   }

   private static void populatePremium(TierDefinition def, int tier) {
      Item[] pool = poolFor(tier, PREM_EARLY, PREM_MID, PREM_LATE, PREM_END);
      Item primary = pool[tier % pool.length];
      def.getPremiumRewards().add(new ItemStack(primary, premiumCount(tier)));

      Item secondary = pool[(tier + 2) % pool.length];
      def.getPremiumRewards().add(new ItemStack(secondary, 1));

      // High tiers get an extra premium drop.
      if (tier % 4 == 0) {
         Item extra = pool[(tier + 5) % pool.length];
         def.getPremiumRewards().add(new ItemStack(extra, Math.max(1, tier / 25)));
      }
   }

   private static void applyMilestone(TierDefinition def, int tier) {
      switch (tier) {
         case 10 -> def.setRankReward(new PassRankReward("novato", "\u2726 Novato",
            new NametagStyle(0xFFD700, false, false, false, false, false, 0, 0)));
         case 25 -> def.setRankReward(new PassRankReward("caballero", "\u2694 Caballero",
            new NametagStyle(0x55FFFF, true, false, false, false, false, 0, 0)));
         case 50 -> def.setRankReward(new PassRankReward("baron", "\u265C Bar\u00f3n",
            new NametagStyle(0xFF55FF, true, false, false, false, false, 0, 0)));
         case 75 -> def.setRankReward(new PassRankReward("duque", "\u265B Duque",
            new NametagStyle(0xFFFFFF, true, false, false, false, true, 0x55FFFF, 0xFF55FF)));
         case 100 -> def.setRankReward(new PassRankReward("rey", "\u2654 Rey",
            new NametagStyle(0xFFFFFF, true, false, false, false, true, 0xFFD700, 0xFF5555)));
         default -> {
         }
      }
   }

   private static Item[] poolFor(int tier, Item[] early, Item[] mid, Item[] late, Item[] end) {
      if (tier <= 25) {
         return early;
      } else if (tier <= 50) {
         return mid;
      } else if (tier <= 75) {
         return late;
      } else {
         return end;
      }
   }

   private static int freeCount(int tier) {
      int band = (tier - 1) / 25;
      int base = 4 + band * 2;
      int scaled = base + tier / 12;
      return Math.max(1, Math.min(64, scaled));
   }

   private static int premiumCount(int tier) {
      int band = (tier - 1) / 25;
      int base = 2 + band;
      int scaled = base + tier / 20;
      return Math.max(1, Math.min(32, scaled));
   }
}
