package com.fshop.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common configuration for FShop. Controls the coin economy (which items count
 * as currency and their relative value), plus market and shop limits.
 */
public final class FShopConfig {
   public static final ForgeConfigSpec SPEC;

   // Currency ---------------------------------------------------------------
   public static final ForgeConfigSpec.ConfigValue<String> BRONZE_COIN_ID;
   public static final ForgeConfigSpec.ConfigValue<String> SILVER_COIN_ID;
   public static final ForgeConfigSpec.ConfigValue<String> GOLD_COIN_ID;
   public static final ForgeConfigSpec.IntValue SILVER_VALUE;
   public static final ForgeConfigSpec.IntValue GOLD_VALUE;

   // Shops ------------------------------------------------------------------
   public static final ForgeConfigSpec.IntValue MAX_SHOPS_PER_PLAYER;
   public static final ForgeConfigSpec.IntValue MAX_OFFERS_PER_SHOP;
   public static final ForgeConfigSpec.LongValue MAX_UNIT_PRICE;
   public static final ForgeConfigSpec.BooleanValue REQUIRE_ZONE_FOR_BUY;
   public static final ForgeConfigSpec.BooleanValue REQUIRE_ZONE_FOR_SELL;
   public static final ForgeConfigSpec.BooleanValue REQUIRE_ZONE_FOR_CREATE;

   static {
      ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

      b.comment("Currency settings. FShop uses item-based coins (FantasticCoins / athens_coins by default).",
            "All prices are expressed in the smallest unit (a single bronze coin = 1).").push("currency");
      BRONZE_COIN_ID = b.comment("Item id used as the base (value 1) coin.")
            .define("bronzeCoinId", "athens_coins:bronze_coin");
      SILVER_COIN_ID = b.comment("Item id used as the mid coin.")
            .define("silverCoinId", "athens_coins:silver_coin");
      GOLD_COIN_ID = b.comment("Item id used as the high coin.")
            .define("goldCoinId", "athens_coins:gold_coin");
      SILVER_VALUE = b.comment("How many bronze coins a single silver coin is worth.")
            .defineInRange("silverValue", 100, 1, 1_000_000);
      GOLD_VALUE = b.comment("How many bronze coins a single gold coin is worth.")
            .defineInRange("goldValue", 10_000, 1, 1_000_000_000);
      b.pop();

      b.comment("Player shop / market limits.").push("shops");
      MAX_SHOPS_PER_PLAYER = b.comment("Maximum number of shops a single player may own.")
            .defineInRange("maxShopsPerPlayer", 1, 1, 50);
      MAX_OFFERS_PER_SHOP = b.comment("Maximum number of distinct offers (item entries) per shop.")
            .defineInRange("maxOffersPerShop", 54, 1, 108);
      MAX_UNIT_PRICE = b.comment("Maximum price per unit a player may set.")
            .defineInRange("maxUnitPrice", 1_000_000L, 1L, 1_000_000_000L);
      REQUIRE_ZONE_FOR_BUY = b.comment("Require the player to stand inside a market zone to use /fshop buy.")
            .define("requireZoneForBuy", true);
      REQUIRE_ZONE_FOR_SELL = b.comment("Require the player to stand inside a market zone to use /fshop sell.")
            .define("requireZoneForSell", true);
      REQUIRE_ZONE_FOR_CREATE = b.comment("Require the player to stand inside a market zone to use /fshop create.")
            .define("requireZoneForCreate", true);
      b.pop();

      SPEC = b.build();
   }

   private FShopConfig() {
   }
}
