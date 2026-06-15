package com.pewez.fantasticshortcuts.security;

import java.util.Set;

/**
 * Central security policy for Fantastic Shortcuts.
 *
 * Enforces the protected command list and validates aliases / target commands. The mod never
 * elevates permissions; these rules only decide what may be turned into a shortcut.
 */
public final class SecurityRules {

    /** Aliases that may never be created, and commands that may never be the target of a shortcut. */
    private static final Set<String> PROTECTED = Set.of(
            "fshortcuts",
            "fantasticshortcuts",
            "stop",
            "reload",
            "help",
            "op",
            "deop",
            "ban",
            "ban-ip",
            "pardon",
            "pardon-ip",
            "whitelist",
            "save-all",
            "save-off",
            "save-on",
            "kick",
            "execute"
    );

    private SecurityRules() {
    }

    public static boolean isProtected(String firstToken) {
        if (firstToken == null) {
            return true;
        }
        return PROTECTED.contains(firstToken.toLowerCase().trim());
    }

    public static Set<String> protectedCommands() {
        return PROTECTED;
    }

    /** Aliases must be a single word: letters, digits, underscores and dashes only. */
    public static boolean isValidAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        return alias.matches("[A-Za-z0-9_\\-]{1,32}");
    }

    /** First word of a command string. */
    public static String firstToken(String command) {
        if (command == null) {
            return "";
        }
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    /**
     * Detect characters that could be used to chain or inject extra commands. Command targets must be
     * a single command (variables {args} are allowed and handled separately).
     */
    public static boolean looksLikeInjection(String command) {
        if (command == null) {
            return true;
        }
        return command.contains("\n") || command.contains("\r") || command.contains(";")
                || command.contains("\u0000");
    }

    /**
     * Validate a target command. Returns null if valid, otherwise a human readable reason.
     */
    public static String validateTarget(String command) {
        if (command == null || command.isBlank()) {
            return "The target command is empty.";
        }
        if (looksLikeInjection(command)) {
            return "The target command contains illegal characters.";
        }
        if (isProtected(firstToken(command))) {
            return "You cannot create a shortcut for the protected command '/" + firstToken(command) + "'.";
        }
        return null;
    }
}
