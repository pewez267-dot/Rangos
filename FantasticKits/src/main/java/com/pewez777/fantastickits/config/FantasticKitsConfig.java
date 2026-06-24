/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side configuration for Fantastic Kits.
 *
 * <p>The configuration file is registered at
 * {@code config/fantastickits/config.toml} and contains the core gating rules,
 * the audit-log subsystem settings and the security-events subsystem settings.
 * Every option that controls security or claiming logic lives here so that the
 * server owner has full, file-based control that survives restarts.</p>
 */
public final class FantasticKitsConfig {

    private FantasticKitsConfig() {
    }

    public static final ForgeConfigSpec SPEC;

    // ---- Core gating rules -------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue STRICT_GROUP_MATCHING;
    public static final ForgeConfigSpec.BooleanValue SINGLE_PERMANENT_CLAIM;
    public static final ForgeConfigSpec.BooleanValue ENFORCE_COMMAND_BARRIER;
    public static final ForgeConfigSpec.BooleanValue PUBLISH_LUCKPERMS_NODES;

    // ---- Anti-exploit ------------------------------------------------------
    public static final ForgeConfigSpec.IntValue CLAIM_COOLDOWN_MILLIS;
    public static final ForgeConfigSpec.IntValue MAX_REQUESTS_PER_SECOND;

    // ---- Audit log ---------------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue AUDIT_LOG_ENABLED;
    public static final ForgeConfigSpec.BooleanValue AUDIT_LOG_CONSOLE;
    public static final ForgeConfigSpec.BooleanValue AUDIT_LOG_FILE;
    public static final ForgeConfigSpec.IntValue AUDIT_LOG_MAX_ENTRIES;
    public static final ForgeConfigSpec.IntValue AUDIT_MAX_FILE_SIZE_MB;
    public static final ForgeConfigSpec.BooleanValue AUDIT_VIEWER_ENABLED;

    // ---- Security events ---------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue SECURITY_EVENTS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SECURITY_EVENTS_CONSOLE;
    public static final ForgeConfigSpec.BooleanValue SECURITY_EVENTS_FILE;
    public static final ForgeConfigSpec.IntValue SECURITY_MAX_FILE_SIZE_MB;
    public static final ForgeConfigSpec.BooleanValue LOG_PLAYER_IP;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Fantastic Kits configuration",
                "Copyright (c) 2026 Pewez777. All Rights Reserved.",
                "All security and claiming validation is performed server-side only.");

        builder.push("core");
        STRICT_GROUP_MATCHING = builder
                .comment("If true, a kit can ONLY be claimed/used by players whose LuckPerms",
                        "PRIMARY GROUP matches the kit's owner group EXACTLY.",
                        "Inheritance, weights and permission hierarchies are NEVER considered.")
                .define("strictGroupMatching", true);
        SINGLE_PERMANENT_CLAIM = builder
                .comment("If true, each kit may be claimed only once per player, permanently.")
                .define("singlePermanentClaim", true);
        ENFORCE_COMMAND_BARRIER = builder
                .comment("If true, commands owned by a kit can only be executed by players",
                        "whose primary group exactly matches that kit's owner group.")
                .define("enforceCommandBarrier", true);
        PUBLISH_LUCKPERMS_NODES = builder
                .comment("If true, the group -> kit -> commands relationship is published to",
                        "LuckPerms automatically as meta/permission nodes when available.")
                .define("publishLuckPermsNodes", true);
        builder.pop();

        builder.push("antiExploit");
        CLAIM_COOLDOWN_MILLIS = builder
                .comment("Minimum milliseconds between two claim attempts from the same player.",
                        "Mitigates double-click, macro and packet-spam duplication exploits.")
                .defineInRange("claimCooldownMillis", 1000, 0, 60000);
        MAX_REQUESTS_PER_SECOND = builder
                .comment("Maximum mod packets accepted per player per second before throttling.")
                .defineInRange("maxRequestsPerSecond", 5, 1, 100);
        builder.pop();

        builder.push("audit");
        AUDIT_LOG_ENABLED = builder
                .comment("Master switch for the audit-log subsystem.")
                .define("auditLogEnabled", true);
        AUDIT_LOG_CONSOLE = builder
                .comment("Mirror audit entries to the server console.")
                .define("auditLogConsole", true);
        AUDIT_LOG_FILE = builder
                .comment("Write audit entries to config/fantastickits/audit/audit.log (append-only).")
                .define("auditLogFile", true);
        AUDIT_LOG_MAX_ENTRIES = builder
                .comment("Soft cap of retained entries across rotated files.")
                .defineInRange("auditLogMaxEntries", 50000, 100, 10_000_000);
        AUDIT_MAX_FILE_SIZE_MB = builder
                .comment("Rotate audit.log once it reaches this size (MB): audit-1.log, audit-2.log...")
                .defineInRange("auditMaxFileSizeMB", 25, 1, 1024);
        AUDIT_VIEWER_ENABLED = builder
                .comment("Allow administrators to read (never modify) audit logs in-game.")
                .define("auditViewerEnabled", true);
        builder.pop();

        builder.push("security");
        SECURITY_EVENTS_ENABLED = builder
                .comment("Master switch for the security-events subsystem.")
                .define("securityEventsEnabled", true);
        SECURITY_EVENTS_CONSOLE = builder
                .comment("Mirror security events to the server console.")
                .define("securityEventsConsole", true);
        SECURITY_EVENTS_FILE = builder
                .comment("Write security events to config/fantastickits/audit/security.log (append-only).")
                .define("securityEventsFile", true);
        SECURITY_MAX_FILE_SIZE_MB = builder
                .comment("Rotate security.log once it reaches this size (MB).")
                .defineInRange("securityMaxFileSizeMB", 25, 1, 1024);
        LOG_PLAYER_IP = builder
                .comment("If true and available, record the player's IP in audit/security entries.")
                .define("logPlayerIp", true);
        builder.pop();

        SPEC = builder.build();
    }
}
