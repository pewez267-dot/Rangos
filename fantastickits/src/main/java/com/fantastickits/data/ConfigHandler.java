package com.fantastickits.data;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge Config API handler for general mod settings.
 * Loaded from config/fantastickits/config.toml
 */
public class ConfigHandler {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_AUDIT_LOG;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMMAND_RESTRICTIONS;
    public static final ForgeConfigSpec.IntValue MAX_ITEMS_PER_KIT;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_OP_FOR_ADMIN;
    public static final ForgeConfigSpec.IntValue OP_LEVEL_REQUIRED;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_CLAIM;
    public static final ForgeConfigSpec.BooleanValue LOG_COMMAND_USAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("FantasticKits Configuration").push("general");

        ENABLE_AUDIT_LOG = builder
                .comment("Enable audit logging to audit.log")
                .define("enableAuditLog", true);

        ENABLE_COMMAND_RESTRICTIONS = builder
                .comment("Enable command restriction system (group-based command blocking)")
                .define("enableCommandRestrictions", true);

        MAX_ITEMS_PER_KIT = builder
                .comment("Maximum number of items a kit can contain")
                .defineInRange("maxItemsPerKit", 54, 1, 54);

        REQUIRE_OP_FOR_ADMIN = builder
                .comment("Require OP status for admin commands (create, edit, delete, get)")
                .define("requireOpForAdmin", true);

        OP_LEVEL_REQUIRED = builder
                .comment("Minimum OP level required for admin commands (1-4)")
                .defineInRange("opLevelRequired", 2, 1, 4);

        NOTIFY_ON_CLAIM = builder
                .comment("Send a chat message to the player when they claim a kit")
                .define("notifyOnClaim", true);

        LOG_COMMAND_USAGE = builder
                .comment("Log command usage (allowed and blocked) to audit.log")
                .define("logCommandUsage", true);

        builder.pop();

        SPEC = builder.build();
    }
}
