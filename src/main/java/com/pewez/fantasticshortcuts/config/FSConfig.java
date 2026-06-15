package com.pewez.fantasticshortcuts.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuración del mod escrita en {@code config/fantasticshortcuts/config.toml}.
 *
 * <p>Opciones expuestas (exigidas por la especificación):
 * <ul>
 *     <li>{@code enableReplaceMode}: interruptor global del modo "Reemplazar". Si está en
 *     {@code false}, ningún atajo ocultará su comando original del TAB aunque tenga la opción
 *     activada individualmente.</li>
 *     <li>{@code shortcutPriority}: prioridad entre el atajo y el comando original cuando ambos
 *     podrían resolver el mismo literal ({@code SHORTCUT_FIRST} / {@code ORIGINAL_FIRST}).</li>
 *     <li>{@code auditEnabled}: activa/desactiva el sistema de auditoría.</li>
 * </ul>
 */
public final class FSConfig {

    public enum Priority { SHORTCUT_FIRST, ORIGINAL_FIRST }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_REPLACE_MODE;
    public static final ForgeConfigSpec.EnumValue<Priority> SHORTCUT_PRIORITY;
    public static final ForgeConfigSpec.BooleanValue AUDIT_ENABLED;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Fantastic Shortcuts - configuracion general.",
                "Este mod NUNCA otorga permisos: solo traduce atajos a comandos reales y respeta",
                "el sistema de permisos vanilla, de mods y de LuckPerms."
        ).push("general");

        ENABLE_REPLACE_MODE = builder
                .comment("Interruptor global del modo Reemplazar.",
                        "Si es false, ningun atajo ocultara su comando original del TAB aunque",
                        "tenga la opcion 'Reemplazar' activada de forma individual.")
                .define("enableReplaceMode", true);

        SHORTCUT_PRIORITY = builder
                .comment("Prioridad de resolucion cuando un atajo y un comando comparten literal.",
                        "SHORTCUT_FIRST = el atajo tiene preferencia.",
                        "ORIGINAL_FIRST = el comando original tiene preferencia.")
                .defineEnum("shortcutPriority", Priority.SHORTCUT_FIRST);

        AUDIT_ENABLED = builder
                .comment("Activa el sistema de auditoria en config/fantasticshortcuts/audit/.")
                .define("auditEnabled", true);

        builder.pop();
        SPEC = builder.build();
    }

    private FSConfig() {}

    public static boolean enableReplaceMode() {
        return ENABLE_REPLACE_MODE.get();
    }

    public static Priority shortcutPriority() {
        return SHORTCUT_PRIORITY.get();
    }

    public static boolean auditEnabled() {
        return AUDIT_ENABLED.get();
    }
}
