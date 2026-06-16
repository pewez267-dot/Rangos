package com.fantastickits.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * General mod configuration, backed by the Forge Config API and written to
 * {@code config/fantastickits/config.toml} (a {@code COMMON} config, so it lives in the
 * global {@code config/} directory rather than per-world).
 *
 * <p>Spec values are mirrored into plain {@code volatile} fields by {@link #bake()}
 * (called on load/reload) so the rest of the mod can read them cheaply and safely
 * from any thread without risking the "config not loaded yet" exception.</p>
 */
public final class FKConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_AUDIT_LOG;
    private static final ForgeConfigSpec.BooleanValue BROADCAST_ON_CLAIM;
    private static final ForgeConfigSpec.IntValue MAX_ITEMS_PER_KIT;
    private static final ForgeConfigSpec.BooleanValue ENABLE_COMMAND_GATING;
    private static final ForgeConfigSpec.BooleanValue OPS_BYPASS_COMMAND_GATING;
    private static final ForgeConfigSpec.BooleanValue MANAGE_LUCKPERMS_PERMISSIONS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_CLAIM_WITHOUT_GROUP;
    private static final ForgeConfigSpec.IntValue ADMIN_PERMISSION_LEVEL;

    // Baked values (sensible defaults until the config is loaded).
    private static volatile boolean auditLog = true;
    private static volatile boolean broadcastOnClaim = false;
    private static volatile int maxItemsPerKit = 54;
    private static volatile boolean commandGating = true;
    private static volatile boolean opsBypassGating = true;
    private static volatile boolean manageLuckPermsPermissions = true;
    private static volatile boolean allowClaimWithoutGroup = false;
    private static volatile int adminPermissionLevel = 4;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Ajustes generales de Fantastic Kits").push("general");
        ENABLE_AUDIT_LOG = builder
                .comment("Escribir cada evento relevante en audit.log.")
                .define("enableAuditLog", true);
        BROADCAST_ON_CLAIM = builder
                .comment("Anunciar a todo el servidor cuando un jugador reclama un kit.")
                .define("broadcastOnClaim", false);
        MAX_ITEMS_PER_KIT = builder
                .comment("Numero maximo de items que puede contener un kit.")
                .defineInRange("maxItemsPerKit", 54, 1, 256);
        builder.pop();

        builder.comment("Ajustes de seguridad y permisos").push("security");
        ENABLE_COMMAND_GATING = builder
                .comment("Activar el control de comandos por grupo (group_commands.json).",
                        "Si se desactiva, no se bloquea ningun comando.")
                .define("enableCommandGating", true);
        OPS_BYPASS_COMMAND_GATING = builder
                .comment("Si es true, los jugadores con nivel de permiso de admin no son afectados por el control de comandos.")
                .define("opsBypassCommandGating", true);
        MANAGE_LUCKPERMS_PERMISSIONS = builder
                .comment("Si es true, al asignar comandos a un kit se registran como nodos de permiso",
                        "(fantastickits.command.<comando>) en el grupo de LuckPerms del rango del kit,",
                        "de modo que SOLO ese rango (y los que lo heredan) puede usar esos comandos.",
                        "Requiere LuckPerms instalado; si no esta, se usa el control interno por grupo.")
                .define("manageLuckPermsPermissions", true);
        ALLOW_CLAIM_WITHOUT_GROUP = builder
                .comment("Si es true, un kit sin grupo asignado puede ser reclamado por cualquier jugador.",
                        "Por defecto false: un kit SIEMPRE requiere un grupo de LuckPerms.")
                .define("allowClaimWithoutGroup", false);
        ADMIN_PERMISSION_LEVEL = builder
                .comment("Nivel de permiso vanilla requerido para los comandos administrativos (create/edit/delete/get-a-otros/test).")
                .defineInRange("adminPermissionLevel", 4, 0, 4);
        builder.pop();

        SPEC = builder.build();
    }

    private FKConfig() {
    }

    /** Copies spec values into the baked fields. Safe to call on load and reload. */
    public static void bake() {
        auditLog = ENABLE_AUDIT_LOG.get();
        broadcastOnClaim = BROADCAST_ON_CLAIM.get();
        maxItemsPerKit = MAX_ITEMS_PER_KIT.get();
        commandGating = ENABLE_COMMAND_GATING.get();
        opsBypassGating = OPS_BYPASS_COMMAND_GATING.get();
        manageLuckPermsPermissions = MANAGE_LUCKPERMS_PERMISSIONS.get();
        allowClaimWithoutGroup = ALLOW_CLAIM_WITHOUT_GROUP.get();
        adminPermissionLevel = ADMIN_PERMISSION_LEVEL.get();
    }

    public static boolean auditLogEnabled() {
        return auditLog;
    }

    public static boolean broadcastOnClaim() {
        return broadcastOnClaim;
    }

    public static int maxItemsPerKit() {
        return maxItemsPerKit;
    }

    public static boolean commandGatingEnabled() {
        return commandGating;
    }

    public static boolean opsBypassCommandGating() {
        return opsBypassGating;
    }

    public static boolean manageLuckPermsPermissions() {
        return manageLuckPermsPermissions;
    }

    public static boolean allowClaimWithoutGroup() {
        return allowClaimWithoutGroup;
    }

    public static int adminPermissionLevel() {
        return adminPermissionLevel;
    }
}
