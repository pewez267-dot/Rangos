package com.fantasticshortcuts.util;

import com.fantasticshortcuts.data.Shortcut;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure, side-effect-free conflict detection for a proposed alias. Reused by the client
 * GUI (live warnings) and the server (authoritative validation before saving), so both
 * agree on the verdict. Never throws; always returns a clear result.
 */
public final class ConflictChecker {

    /** Commands that must never be shadowed by an alias. */
    private static final Set<String> CRITICAL = new HashSet<>(Arrays.asList(
            "stop", "op", "deop", "ban", "ban-ip", "pardon", "pardon-ip", "whitelist",
            "save-all", "save-off", "save-on", "kick", "reload", "debug"
    ));

    public enum Severity {
        OK, WARNING, ERROR
    }

    public record Result(Severity severity, String message) {
        public boolean blocking() {
            return severity == Severity.ERROR;
        }
    }

    private ConflictChecker() {
    }

    /**
     * @param alias                  the proposed alias (with or without slash)
     * @param aliasToShortcutName    existing alias keys -> owning shortcut name, EXCLUDING
     *                               the shortcut currently being edited
     * @param registeredCommandExists predicate: does a root command with this name exist?
     */
    public static Result check(final String alias,
                               final Map<String, String> aliasToShortcutName,
                               final Predicate<String> registeredCommandExists) {
        final String key = Shortcut.stripSlash(alias).toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return new Result(Severity.ERROR, "El alias no puede estar vacío.");
        }
        if (key.contains(" ")) {
            return new Result(Severity.ERROR, "El alias no puede contener espacios (debe ser un solo literal).");
        }
        if (!key.matches("[a-z0-9_\\-]+")) {
            return new Result(Severity.ERROR, "El alias solo admite letras minúsculas, números, '_' y '-'.");
        }
        if (CRITICAL.contains(key)) {
            return new Result(Severity.ERROR, "El alias '/" + key + "' colisiona con un comando vanilla crítico y está bloqueado.");
        }
        if (aliasToShortcutName != null && aliasToShortcutName.containsKey(key)) {
            return new Result(Severity.ERROR, "El alias ya lo usa el shortcut '" + aliasToShortcutName.get(key) + "'.");
        }
        if (registeredCommandExists != null && registeredCommandExists.test(key)) {
            return new Result(Severity.WARNING, "Ya existe un comando '/" + key + "' en el servidor. Confirma para crear el alias de todos modos.");
        }
        return new Result(Severity.OK, "Alias disponible.");
    }
}
