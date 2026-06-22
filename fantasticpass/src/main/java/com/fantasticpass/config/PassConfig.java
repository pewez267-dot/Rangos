package com.fantasticpass.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Global configuration backed by Forge's TOML config system. All values are read
 * server-side for gameplay logic; {@link #LINE_SCALE} and {@link #VERTICAL_OFFSET}
 * are also read client-side by the nametag renderer.
 */
public final class PassConfig {

    public static final ForgeConfigSpec SPEC;

    // [general]
    public static final ForgeConfigSpec.IntValue MINUTES_PER_TIER;

    // [afk]
    public static final ForgeConfigSpec.IntValue AFK_THRESHOLD_SECONDS;
    public static final ForgeConfigSpec.IntValue CHECK_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue MIN_ROTATION_CHANGE_DEGREES;

    // [nametag]
    public static final ForgeConfigSpec.DoubleValue LINE_SCALE;
    public static final ForgeConfigSpec.DoubleValue VERTICAL_OFFSET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General progression settings").push("general");
        MINUTES_PER_TIER = builder
                .comment("Minutes of active (non-AFK) play required to unlock each tier.")
                .defineInRange("minutes_per_tier", 60, 1, 100000);
        builder.pop();

        builder.comment("Anti-AFK detection settings").push("afk");
        AFK_THRESHOLD_SECONDS = builder
                .comment("Seconds without meaningful activity before a player is considered AFK.")
                .defineInRange("afk_threshold_seconds", 45, 1, 86400);
        CHECK_INTERVAL_TICKS = builder
                .comment("How often (in ticks) the AFK tracker samples each player. 20 ticks = 1 second.")
                .defineInRange("check_interval_ticks", 20, 1, 1200);
        MIN_ROTATION_CHANGE_DEGREES = builder
                .comment("Minimum camera rotation change (degrees) that counts as activity.")
                .defineInRange("min_rotation_change_degrees", 2.0D, 0.0D, 180.0D);
        builder.pop();

        builder.comment("Nametag rendering settings").push("nametag");
        LINE_SCALE = builder
                .comment("Scale of the extra rank line relative to the vanilla name (0.75 = 75%).")
                .defineInRange("line_scale", 0.75D, 0.1D, 2.0D);
        VERTICAL_OFFSET = builder
                .comment("Vertical offset (blocks) of the extra line below the vanilla name.")
                .defineInRange("vertical_offset", -0.25D, -2.0D, 2.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private PassConfig() {
    }
}
