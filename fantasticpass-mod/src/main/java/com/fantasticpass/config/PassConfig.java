package com.fantasticpass.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public final class PassConfig {
   public static final ForgeConfigSpec SPEC;
   public static final IntValue MINUTES_PER_TIER;
   public static final IntValue DEFAULT_TIER_COUNT;
   public static final IntValue POINTS_PER_TIER;
   public static final IntValue POINTS_PER_MINUTE;
   public static final IntValue WEEK_COUNT;
   public static final IntValue DAILY_FREE_COUNT;
   public static final IntValue DAILY_PREMIUM_COUNT;
   public static final IntValue AFK_THRESHOLD_SECONDS;
   public static final IntValue CHECK_INTERVAL_TICKS;
   public static final DoubleValue MIN_ROTATION_CHANGE_DEGREES;
   public static final DoubleValue LINE_SCALE;
   public static final DoubleValue VERTICAL_OFFSET;

   private PassConfig() {
   }

   static {
      Builder builder = new Builder();
      builder.comment("General progression settings").push("general");
      MINUTES_PER_TIER = builder.comment("Minutes of active (non-AFK) play required to unlock each tier.").defineInRange("minutes_per_tier", 60, 1, 100000);
      DEFAULT_TIER_COUNT = builder.comment("Number of reward tiers a freshly seeded default pass has (1-100).").defineInRange("default_tier_count", 100, 1, 100);
      POINTS_PER_TIER = builder.comment("Battle pass points required to advance one tier (quests + playtime award points).")
         .defineInRange("points_per_tier", 100, 1, 100000);
      POINTS_PER_MINUTE = builder.comment("Passive points per minute of active (non-AFK) play. Default 0 so ALL progress comes from quests; raise it if you want playtime to give points.").defineInRange("points_per_minute", 0, 0, 100000);
      builder.pop();
      builder.comment("Quest settings").push("quests");
      WEEK_COUNT = builder.comment("How many weekly quest sets are available (1-8 themed weeks).").defineInRange("week_count", 8, 1, 8);
      DAILY_FREE_COUNT = builder.comment("How many free daily quests each player receives per day.").defineInRange("daily_free_count", 4, 1, 12);
      DAILY_PREMIUM_COUNT = builder.comment("How many EXTRA daily quests premium players receive per day.").defineInRange("daily_premium_count", 3, 0, 12);
      builder.pop();
      builder.comment("Anti-AFK detection settings").push("afk");
      AFK_THRESHOLD_SECONDS = builder.comment("Seconds without meaningful activity before a player is considered AFK.")
         .defineInRange("afk_threshold_seconds", 45, 1, 86400);
      CHECK_INTERVAL_TICKS = builder.comment("How often (in ticks) the AFK tracker samples each player. 20 ticks = 1 second.")
         .defineInRange("check_interval_ticks", 20, 1, 1200);
      MIN_ROTATION_CHANGE_DEGREES = builder.comment("Minimum camera rotation change (degrees) that counts as activity.")
         .defineInRange("min_rotation_change_degrees", 2.0, 0.0, 180.0);
      builder.pop();
      builder.comment("Nametag rendering settings").push("nametag");
      LINE_SCALE = builder.comment("Scale of the extra rank line relative to the vanilla name (0.75 = 75%).").defineInRange("line_scale", 0.75, 0.1, 2.0);
      VERTICAL_OFFSET = builder.comment("Vertical offset (blocks) of the extra line below the vanilla name.")
         .defineInRange("vertical_offset", -0.25, -2.0, 2.0);
      builder.pop();
      SPEC = builder.build();
   }
}
