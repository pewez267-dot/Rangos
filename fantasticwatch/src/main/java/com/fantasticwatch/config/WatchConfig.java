package com.fantasticwatch.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.time.DayOfWeek;
import java.util.Locale;

/**
 * Forge {@link ForgeConfigSpec} backed configuration for Fantastic Watch.
 *
 * <p>Registered as {@code config/fantasticwatch/config.toml}. Values are read live at use time.</p>
 */
public final class WatchConfig {

    public static final ForgeConfigSpec SPEC;

    // [general]
    public static final ForgeConfigSpec.IntValue TRACKING_RETENTION_DAYS;
    public static final ForgeConfigSpec.ConfigValue<String> CLEANUP_DAY;
    public static final ForgeConfigSpec.BooleanValue SCAN_INVENTORY_ON_LOGIN;
    public static final ForgeConfigSpec.BooleanValue LOG_NON_OP_INTERACTIONS;

    // [nbt]
    public static final ForgeConfigSpec.ConfigValue<String> TAG_NAMESPACE;
    public static final ForgeConfigSpec.BooleanValue TAG_VISIBLE;

    // [performance]
    public static final ForgeConfigSpec.BooleanValue ASYNC_WRITE;
    public static final ForgeConfigSpec.IntValue BUFFER_SIZE;
    public static final ForgeConfigSpec.IntValue FLUSH_INTERVAL_SECONDS;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Fantastic Watch - general settings").push("general");

        TRACKING_RETENTION_DAYS = builder
                .comment("Reference retention window in days. The authoritative purge boundary is the most",
                        "recent 'cleanup_day' at 00:00 UTC (Monday-to-Monday by default).")
                .defineInRange("tracking_retention_days", 7, 1, 3650);

        CLEANUP_DAY = builder
                .comment("Day of week whose most recent 00:00 UTC instant is the purge boundary.",
                        "One of: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY.")
                .define("cleanup_day", "MONDAY");

        SCAN_INVENTORY_ON_LOGIN = builder
                .comment("Scan every connecting player's inventory for tracked items.")
                .define("scan_inventory_on_login", true);

        LOG_NON_OP_INTERACTIONS = builder
                .comment("Log interactions performed by non-operators on tracked items (recommended for forensics).")
                .define("log_non_op_interactions", true);

        builder.pop();

        builder.comment("Fantastic Watch - NBT marking").push("nbt");

        TAG_NAMESPACE = builder
                .comment("Root compound key used for the tracking mark on each item's NBT.")
                .define("tag_namespace", "fantasticwatch");

        TAG_VISIBLE = builder
                .comment("Whether the mark may appear in the item tooltip. Custom NBT compounds are not rendered",
                        "by vanilla tooltips, so the mark is invisible regardless; this flag is reserved and",
                        "kept false to guarantee the mark never disturbs the player experience.")
                .define("tag_visible", false);

        builder.pop();

        builder.comment("Fantastic Watch - performance").push("performance");

        ASYNC_WRITE = builder
                .comment("When true all disk writes happen on a dedicated background writer thread.")
                .define("async_write", true);

        BUFFER_SIZE = builder
                .comment("Number of buffered log lines (across all files) before a forced flush.")
                .defineInRange("buffer_size", 256, 1, 1_000_000);

        FLUSH_INTERVAL_SECONDS = builder
                .comment("Maximum seconds buffered lines may sit in memory before being flushed.")
                .defineInRange("flush_interval_seconds", 3, 1, 3600);

        builder.pop();

        SPEC = builder.build();
    }

    private WatchConfig() {
    }

    /**
     * Parses {@link #CLEANUP_DAY} into a {@link DayOfWeek}, defaulting to {@link DayOfWeek#MONDAY}
     * when the configured value is not a recognised day name.
     */
    public static DayOfWeek cleanupDay() {
        String raw = CLEANUP_DAY.get();
        if (raw == null) {
            return DayOfWeek.MONDAY;
        }
        try {
            return DayOfWeek.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DayOfWeek.MONDAY;
        }
    }
}
