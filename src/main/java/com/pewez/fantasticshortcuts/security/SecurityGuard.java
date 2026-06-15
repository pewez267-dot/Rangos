package com.pewez.fantasticshortcuts.security;

import com.pewez.fantasticshortcuts.shortcuts.Shortcut;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Capa de seguridad del mod.
 *
 * <p>Fantastic Shortcuts NUNCA otorga permisos ni ejecuta como consola. Esta clase se limita a
 * validar entradas:
 * <ul>
 *     <li>El alias debe ser una única palabra ({@code [a-z0-9_-]}).</li>
 *     <li>Está prohibido crear atajos cuyo alias o cuyo comando original toque la
 *     <b>lista protegida</b> (comandos críticos del sistema).</li>
 *     <li>Se detectan intentos de inyección de comandos (";", saltos de línea, etc.).</li>
 * </ul>
 */
public final class SecurityGuard {

    /** Comandos críticos del sistema: ni se pueden usar como alias ni como comando reemplazable. */
    public static final Set<String> PROTECTED = new HashSet<>(Arrays.asList(
            "fshortcuts", "fshortcut",
            "stop", "reload", "help",
            "op", "deop", "ban", "ban-ip", "banlist", "pardon", "pardon-ip",
            "whitelist", "kick", "execute", "save-all", "save-off", "save-on",
            "setidletimeout", "debug", "perf", "jfr", "datapack", "forceload"
    ));

    /** Caracteres / patrones de inyección que jamás deben aceptarse en un comando o alias. */
    private static final Pattern INJECTION = Pattern.compile("[;\\n\\r\\u0000`|&]");

    private static final Pattern VALID_ALIAS = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private SecurityGuard() {}

    public enum Result {
        OK,
        EMPTY_ALIAS,
        INVALID_ALIAS,
        EMPTY_COMMAND,
        PROTECTED_ALIAS,
        PROTECTED_COMMAND,
        INJECTION_ALIAS,
        INJECTION_COMMAND;

        public boolean ok() {
            return this == OK;
        }

        public String message() {
            return switch (this) {
                case OK -> "Correcto.";
                case EMPTY_ALIAS -> "El alias no puede estar vacio.";
                case INVALID_ALIAS -> "Alias invalido. Debe ser UNA palabra: minusculas, numeros, _ o - (max 32).";
                case EMPTY_COMMAND -> "El comando original no puede estar vacio.";
                case PROTECTED_ALIAS -> "Ese alias esta protegido y no puede usarse.";
                case PROTECTED_COMMAND -> "Ese comando es critico del sistema y no puede tener atajo.";
                case INJECTION_ALIAS -> "El alias contiene caracteres no permitidos (posible inyeccion).";
                case INJECTION_COMMAND -> "El comando contiene caracteres no permitidos (posible inyeccion).";
            };
        }
    }

    /** {@code true} si {@code value} contiene caracteres usados para inyección de comandos. */
    public static boolean hasInjection(String value) {
        return value != null && INJECTION.matcher(value).find();
    }

    public static boolean isValidAlias(String alias) {
        return alias != null && VALID_ALIAS.matcher(alias).matches();
    }

    public static boolean isProtectedName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        while (n.startsWith("/")) {
            n = n.substring(1);
        }
        return PROTECTED.contains(n);
    }

    /**
     * Valida un atajo completo antes de crearlo o guardarlo.
     */
    public static Result validate(Shortcut shortcut) {
        return validate(shortcut.alias(), shortcut.command());
    }

    public static Result validate(String rawAlias, String rawCommand) {
        final String alias = Shortcut.normalizeAlias(rawAlias);
        final String command = Shortcut.normalizeCommand(rawCommand);

        if (alias.isBlank()) {
            return Result.EMPTY_ALIAS;
        }
        if (hasInjection(rawAlias)) {
            return Result.INJECTION_ALIAS;
        }
        if (!isValidAlias(alias)) {
            return Result.INVALID_ALIAS;
        }
        if (isProtectedName(alias)) {
            return Result.PROTECTED_ALIAS;
        }
        if (command.isBlank()) {
            return Result.EMPTY_COMMAND;
        }
        if (hasInjection(rawCommand)) {
            return Result.INJECTION_COMMAND;
        }
        // Root del comando original (primer token) contra la lista protegida.
        final int sp = command.indexOf(' ');
        final String root = (sp < 0 ? command : command.substring(0, sp));
        if (isProtectedName(root)) {
            return Result.PROTECTED_COMMAND;
        }
        return Result.OK;
    }
}
