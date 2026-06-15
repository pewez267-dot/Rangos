package com.pewez.fantasticshortcuts.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Mod configuration, written to {@code config/fantasticshortcuts/config.toml}.
 *
 * This file holds behaviour toggles only. The shortcuts themselves live in
 * {@code config/fantasticshortcuts/shortcuts.json} and are managed by the storage layer.
 */
public final class ModConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_REPLACE_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> SHORTCUT_PRIORITY;
    public static final ForgeConfigSpec.BooleanValue AUDIT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue WARN_ON_CONFLICT;
    public static final ForgeConfigSpec.BooleanValue LUCKPERMS_INTEGRATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Fantastic Shortcuts configuration",
                "Shortcuts are defined in config/fantasticshortcuts/shortcuts.json").push("general");

        ENABLE_REPLACE_MODE = builder
                .comment("Global default for replace mode. When a shortcut has replaceOriginal enabled,",
                        "the original command is hidden from the client command tree (tab/suggestions).",
                        "This master switch must be true for any per-shortcut replace to take effect.")
                .define("enableReplaceMode", true);

        SHORTCUT_PRIORITY = builder
                .comment("Conflict priority when an alias collides with an existing command.",
                        "Allowed values: SHORTCUT (keep the shortcut, warn) or ORIGINAL (keep the existing command, skip the shortcut).")
                .define("shortcutPriority", "ORIGINAL");

        AUDIT_ENABLED = builder
                .comment("Enable the audit log in config/fantasticshortcuts/audit/.")
                .define("auditEnabled", true);

        WARN_ON_CONFLICT = builder
                .comment("Log a warning to the server console when an alias conflicts with another command.")
                .define("warnOnConflict", true);

        LUCKPERMS_INTEGRATION = builder
                .comment("Use the LuckPerms API (read-only) when LuckPerms is installed, for audit context.",
                        "Fantastic Shortcuts never modifies or grants permissions.")
                .define("luckPermsIntegration", true);

        builder.pop();
        SPEC = builder.build();
    }

    private ModConfig() {
    }

    public static boolean keepShortcutOnConflict() {
        return "SHORTCUT".equalsIgnoreCase(SHORTCUT_PRIORITY.get());
    }
}
