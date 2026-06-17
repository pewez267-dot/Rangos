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
    public static final ForgeConfigSpec.BooleanValue LOG_STACKABLE_SPAWNS;

    // [nbt]
    public static final ForgeConfigSpec.ConfigValue<String> TAG_NAMESPACE;
    public static final ForgeConfigSpec.BooleanValue TAG_VISIBLE;
    public static final ForgeConfigSpec.ConfigValue<String> MARK_MODE;

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

        LOG_STACKABLE_SPAWNS = builder
                .comment("Log when an operator materialises STACKABLE items from the creative inventory, WITHOUT",
                        "applying any NBT mark (so those items keep stacking normally). This records what was",
                        "pulled (item id, quantity, who, when, where) as ITEM_STACK_SPAWNED, but such items are",
                        "not individually followed afterwards (that would require the stacking-breaking mark).",
                        "Detected by diffing the operator's inventory totals once per second while in creative.")
                .define("log_stackable_spawns", true);

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

        MARK_MODE = builder
                .comment("Which items receive the per-item NBT tracking mark. The mark carries a unique id, so",
                        "vanilla will NOT stack two marked items (or a marked item with an unmarked one).",
                        "  - unstackable_only (DEFAULT): only mark items whose max stack size is 1 (tools, armor,",
                        "      etc.). Stackable items (blocks, resources) are never marked, so stacking is never",
                        "      broken. Recommended: high-value gear is still fully tracked.",
                        "  - none: never mark any item. Stacking is fully vanilla; per-item NBT tracking is off.",
                        "  - all: mark every item (original behaviour). WARNING: this breaks stacking of marked items.",
                        "Items already marked but that should not be under the current mode are auto-unmarked when",
                        "encountered (login, container open, operator inventory scan), restoring their stacking.")
                .define("mark_mode", "unstackable_only");

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
    public static DayOfWeek cleanupDay() {        String raw = CLEANUP_DAY.get();
        if (raw == null) {
            return DayOfWeek.MONDAY;
        }
        try {
            return DayOfWeek.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DayOfWeek.MONDAY;
        }
    }

    /** Normalised marking mode: one of {@code none}, {@code unstackable_only}, {@code all}. */
    public static String markMode() {
        String raw = MARK_MODE.get();
        if (raw == null) {
            return "unstackable_only";
        }
        String mode = raw.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "none", "all", "unstackable_only" -> mode;
            default -> "unstackable_only";
        };
    }
}
