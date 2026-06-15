package com.pewez.fantasticshortcuts.brigadier;

import com.pewez.fantasticshortcuts.config.ModConfig;
import com.pewez.fantasticshortcuts.security.SecurityRules;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which original commands are currently "replaced" and therefore must be hidden from the
 * client command tree (tab / suggestions). Populated from every shortcut whose {@code replaceOriginal}
 * flag is set, provided the global replace mode is enabled.
 *
 * The {@link com.pewez.fantasticshortcuts.mixin.CommandsMixin} reads this set when building the
 * per-client command packet, so hidden originals never appear in tab completion - while the command
 * itself remains fully executable on the server (so the alias keeps working).
 */
public final class ReplaceRegistry {

    private static final Set<String> HIDDEN = ConcurrentHashMap.newKeySet();

    private ReplaceRegistry() {
    }

    public static void rebuild() {
        HIDDEN.clear();
        boolean replaceEnabled;
        try {
            replaceEnabled = ModConfig.ENABLE_REPLACE_MODE.get();
        } catch (Exception e) {
            replaceEnabled = true;
        }
        if (!replaceEnabled) {
            return;
        }
        for (Shortcut shortcut : ShortcutManager.get().all()) {
            if (!shortcut.replaceOriginal) {
                continue;
            }
            String token = SecurityRules.firstToken(shortcut.command);
            if (token != null && !token.isBlank() && !SecurityRules.isProtected(token)) {
                HIDDEN.add(token.toLowerCase());
            }
        }
    }

    public static boolean isHidden(String literal) {
        return literal != null && HIDDEN.contains(literal.toLowerCase());
    }

    public static boolean isEmpty() {
        return HIDDEN.isEmpty();
    }
}
