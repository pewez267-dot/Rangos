package com.fantasticaudit.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge {@link ForgeConfigSpec} backed configuration for Fantastic Audit.
 *
 * <p>The file is registered as {@code config/fantasticaudit/config.toml} (see
 * {@code FantasticAudit} constructor). All values are read directly from the spec at
 * event time; there is no caching layer, so a {@code /reload} style config change is
 * picked up automatically by Forge.</p>
 */
public final class AuditConfig {

    public static final ForgeConfigSpec SPEC;

    // [general]
    public static final ForgeConfigSpec.IntValue LOG_RETENTION_DAYS;
    public static final ForgeConfigSpec.ConfigValue<String> SERVER_RESOURCE_PACK_HASH;
    public static final ForgeConfigSpec.BooleanValue LOG_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue LOG_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue BLOCK_SUMMARY;
    public static final ForgeConfigSpec.BooleanValue CAPTURE_ARCHITECTURY_BREAKS;
    public static final ForgeConfigSpec.BooleanValue LOG_ITEMS;
    public static final ForgeConfigSpec.BooleanValue LOG_SESSIONS;
    public static final ForgeConfigSpec.BooleanValue LOG_RESOURCE_PACKS;
    public static final ForgeConfigSpec.BooleanValue LOG_CONTAINERS;

    // [performance]
    public static final ForgeConfigSpec.BooleanValue ASYNC_WRITE;
    public static final ForgeConfigSpec.IntValue BUFFER_SIZE;
    public static final ForgeConfigSpec.IntValue FLUSH_INTERVAL_SECONDS;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Fantastic Audit - general settings").push("general");

        LOG_RETENTION_DAYS = builder
                .comment("Number of days of log lines to keep. On server start, lines older than this are pruned.")
                .defineInRange("log_retention_days", 90, 1, 36500);

        SERVER_RESOURCE_PACK_HASH = builder
                .comment("SHA-1 hash of the official server resource pack. Used for Xray-suspicion detection.",
                        "Leave empty if the server does not enforce a resource pack.")
                .define("server_resource_pack_hash", "");

        LOG_COMMANDS = builder
                .comment("Log every command executed by a player.")
                .define("log_commands", true);

        LOG_BLOCKS = builder
                .comment("Log block break / place / interact events.")
                .define("log_blocks", true);

        BLOCK_SUMMARY = builder
                .comment("Maintain a separate, cumulative human-readable summary of mined blocks per player",
                        "at config/fantasticaudit/summaries/blocks/{UUID}.txt (block id, total mined, tool used).",
                        "Survives restarts and aggregates across sessions.")
                .define("block_summary", true);

        CAPTURE_ARCHITECTURY_BREAKS = builder
                .comment("Also capture block breaks routed through Architectury's BlockEvent.BREAK rather than",
                        "Forge's BlockEvent. Area tools such as JustHammers break the extra blocks through",
                        "Architectury, so without this only the directly-hit block is logged. Requires the",
                        "Architectury API to be installed; harmless (ignored) if it is not. The directly-hit",
                        "block is de-duplicated so it is never logged twice.")
                .define("capture_architectury_breaks", true);

        LOG_ITEMS = builder
                .comment("Log item pickup / drop / use / craft events.")
                .define("log_items", true);

        LOG_SESSIONS = builder
                .comment("Log session start / end events.")
                .define("log_sessions", true);

        LOG_RESOURCE_PACKS = builder
                .comment("Log resource pack acceptance / decline / load / failure events.")
                .define("log_resource_packs", true);

        LOG_CONTAINERS = builder
                .comment("Log container put / take events (net changes per open/close session).")
                .define("log_containers", true);

        builder.pop();

        builder.comment("Fantastic Audit - performance settings").push("performance");

        ASYNC_WRITE = builder
                .comment("When true all disk writes happen on a dedicated background writer thread.",
                        "When false writes are performed synchronously under a global lock (still thread-safe).")
                .define("async_write", true);

        BUFFER_SIZE = builder
                .comment("Number of buffered log lines (across all files) before a forced flush is triggered.")
                .defineInRange("buffer_size", 512, 1, 1_000_000);

        FLUSH_INTERVAL_SECONDS = builder
                .comment("Maximum number of seconds buffered lines may sit in memory before being flushed to disk.")
                .defineInRange("flush_interval_seconds", 5, 1, 3600);

        builder.pop();

        SPEC = builder.build();
    }

    private AuditConfig() {
        // Static configuration holder; not instantiable.
    }
}
