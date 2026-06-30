package com.fantasticpass.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public final class PassConfig {
   public static final ForgeConfigSpec SPEC;
   public static final IntValue MINUTES_PER_TIER;
   public static final IntValue POINTS_PER_TIER;
   public static final IntValue POINTS_PER_MINUTE;
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
      POINTS_PER_TIER = builder.comment("Battle pass points required to advance one tier (quests + playtime award points).")
         .defineInRange("points_per_tier", 100, 1, 100000);
      POINTS_PER_MINUTE = builder.comment("Points awarded per minute of active play (kept low so quests drive most progress).").defineInRange("points_per_minute", 3, 0, 100000);
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
