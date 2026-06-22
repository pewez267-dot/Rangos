package com.fantasticranks.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Global configuration backed by Forge's TOML config system. Read server-side for
 * gameplay logic; {@link #LINE_SCALE} and {@link #VERTICAL_OFFSET} are also read
 * client-side by the nametag renderer.
 */
public final class RanksConfig {

    public static final ForgeConfigSpec SPEC;

    // [general]
    public static final ForgeConfigSpec.ConfigValue<String> RANK_UP_MESSAGE;

    // [afk]
    public static final ForgeConfigSpec.IntValue AFK_THRESHOLD_SECONDS;
    public static final ForgeConfigSpec.IntValue CHECK_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue MIN_ROTATION_CHANGE_DEGREES;

    // [nametag]
    public static final ForgeConfigSpec.DoubleValue LINE_SCALE;
    public static final ForgeConfigSpec.DoubleValue VERTICAL_OFFSET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        RANK_UP_MESSAGE = builder
                .comment("Message sent on rank up. {rank} is replaced with the rank name. Supports legacy color codes.")
                .define("rank_up_message", "\u00A76You ranked up to {rank}!");
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

    private RanksConfig() {
    }
}
