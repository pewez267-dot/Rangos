package com.fantasticshortcuts.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * General configuration, backed by the Forge Config API and written to
 * {@code config/fantasticshortcuts/config.toml}. Spec values are mirrored into baked
 * fields by {@link #bake()} for cheap, thread-safe reads from any context.
 */
public final class ShortcutsConfig {

    public static final ForgeConfigSpec SPEC;

    public static final String EXTRA_IGNORE = "ignore";
    public static final String EXTRA_DENY = "deny";

    private static final ForgeConfigSpec.ConfigValue<String> EXTRA_ARGS_BEHAVIOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_CONFLICT_WARNINGS;
    private static final ForgeConfigSpec.BooleanValue AUDIT_ENABLED;
    private static final ForgeConfigSpec.IntValue COMMAND_CACHE_SIZE;
    private static final ForgeConfigSpec.BooleanValue CACHE_REFRESH_ON_RELOAD;

    private static volatile String extraArgsBehavior = EXTRA_IGNORE;
    private static volatile boolean showConflictWarnings = true;
    private static volatile boolean auditEnabled = true;
    private static volatile int commandCacheSize = 1000;
    private static volatile boolean cacheRefreshOnReload = true;

    static {
        final ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Ajustes generales de Fantastic Shortcuts").push("general");
        EXTRA_ARGS_BEHAVIOR = b
                .comment("Que hacer con argumentos extra cuando el shortcut NO usa {args}: 'ignore' o 'deny'.")
                .defineInList("extra_args_behavior", EXTRA_IGNORE, List.of(EXTRA_IGNORE, EXTRA_DENY));
        SHOW_CONFLICT_WARNINGS = b
                .comment("Mostrar advertencias de conflicto en la GUI al crear/editar shortcuts.")
                .define("show_conflict_warnings", true);
        AUDIT_ENABLED = b
                .comment("Escribir eventos en config/fantasticshortcuts/audit/audit.log.")
                .define("audit_enabled", true);
        b.pop();

        b.comment("Ajustes de rendimiento").push("performance");
        COMMAND_CACHE_SIZE = b
                .comment("Numero maximo de shortcuts cacheados en memoria.")
                .defineInRange("command_cache_size", 1000, 1, 1_000_000);
        CACHE_REFRESH_ON_RELOAD = b
                .comment("Reconstruir el cache de shortcuts al recargar.")
                .define("cache_refresh_on_reload", true);
        b.pop();

        SPEC = b.build();
    }

    private ShortcutsConfig() {
    }

    public static void bake() {
        extraArgsBehavior = EXTRA_ARGS_BEHAVIOR.get();
        showConflictWarnings = SHOW_CONFLICT_WARNINGS.get();
        auditEnabled = AUDIT_ENABLED.get();
        commandCacheSize = COMMAND_CACHE_SIZE.get();
        cacheRefreshOnReload = CACHE_REFRESH_ON_RELOAD.get();
    }

    public static boolean denyExtraArgs() {
        return EXTRA_DENY.equalsIgnoreCase(extraArgsBehavior);
    }

    public static boolean showConflictWarnings() {
        return showConflictWarnings;
    }

    public static boolean auditEnabled() {
        return auditEnabled;
    }

    public static int commandCacheSize() {
        return commandCacheSize;
    }

    public static boolean cacheRefreshOnReload() {
        return cacheRefreshOnReload;
    }
}
