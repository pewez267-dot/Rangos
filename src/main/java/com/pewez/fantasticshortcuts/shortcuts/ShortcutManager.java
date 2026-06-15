package com.pewez.fantasticshortcuts.shortcuts;

import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.audit.AuditLog;
import com.pewez.fantasticshortcuts.security.SecurityRules;
import com.pewez.fantasticshortcuts.storage.ShortcutStorage;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory registry and CRUD service for shortcuts. All mutations are validated against the
 * security rules and recorded in the audit log. This class never executes commands itself.
 */
public final class ShortcutManager {

    private static final ShortcutManager INSTANCE = new ShortcutManager();

    private final ShortcutStorage storage = new ShortcutStorage();
    private final Map<String, Shortcut> shortcuts = new LinkedHashMap<>();

    private ShortcutManager() {
    }

    public static ShortcutManager get() {
        return INSTANCE;
    }

    public void init(Path configDir) {
        storage.init(configDir);
        reload();
    }

    public void reload() {
        shortcuts.clear();
        shortcuts.putAll(storage.load());
    }

    public Collection<Shortcut> all() {
        return shortcuts.values();
    }

    public Shortcut get(String alias) {
        return alias == null ? null : shortcuts.get(alias.toLowerCase());
    }

    public boolean exists(String alias) {
        return alias != null && shortcuts.containsKey(alias.toLowerCase());
    }

    /** Result of a CRUD operation. */
    public record Result(boolean success, String message) {
        public static Result ok(String message) {
            return new Result(true, message);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    public Result create(String alias, String command, String actor) {
        if (!SecurityRules.isValidAlias(alias)) {
            AuditLog.record(AuditEvent.INVALID_SHORTCUT, actor, "Invalid alias: " + alias);
            return Result.fail("Invalid alias. Use 1-32 letters, digits, '_' or '-'.");
        }
        if (SecurityRules.isProtected(alias)) {
            AuditLog.record(AuditEvent.INVALID_ACCESS, actor, "Attempt to use protected alias: " + alias);
            return Result.fail("'" + alias + "' is a protected command and cannot be a shortcut.");
        }
        String reason = SecurityRules.validateTarget(command);
        if (reason != null) {
            AuditLog.record(AuditEvent.INVALID_SHORTCUT, actor, "Bad target '" + command + "': " + reason);
            return Result.fail(reason);
        }
        if (exists(alias)) {
            return Result.fail("A shortcut named '" + alias + "' already exists. Use edit instead.");
        }
        Shortcut shortcut = new Shortcut(alias.toLowerCase(), stripSlash(command));
        shortcut.createdBy = actor == null ? "" : actor;
        shortcuts.put(shortcut.alias, shortcut);
        storage.save(shortcuts);
        AuditLog.record(AuditEvent.CREATE_SHORTCUT, actor, alias + " -> " + shortcut.command);
        return Result.ok("Created shortcut /" + alias + " -> /" + shortcut.command);
    }

    public Result edit(String alias, String newCommand, String actor) {
        Shortcut shortcut = get(alias);
        if (shortcut == null) {
            return Result.fail("No shortcut named '" + alias + "'.");
        }
        String reason = SecurityRules.validateTarget(newCommand);
        if (reason != null) {
            AuditLog.record(AuditEvent.INVALID_SHORTCUT, actor, "Bad target '" + newCommand + "': " + reason);
            return Result.fail(reason);
        }
        String old = shortcut.command;
        shortcut.command = stripSlash(newCommand);
        storage.save(shortcuts);
        AuditLog.record(AuditEvent.EDIT_SHORTCUT, actor, alias + ": '" + old + "' -> '" + shortcut.command + "'");
        return Result.ok("Updated /" + alias + " -> /" + shortcut.command);
    }

    public Result setReplaceOriginal(String alias, boolean replace, String actor) {
        Shortcut shortcut = get(alias);
        if (shortcut == null) {
            return Result.fail("No shortcut named '" + alias + "'.");
        }
        shortcut.replaceOriginal = replace;
        storage.save(shortcuts);
        AuditLog.record(AuditEvent.EDIT_SHORTCUT, actor, alias + " replaceOriginal=" + replace);
        return Result.ok("Set replaceOriginal=" + replace + " for /" + alias);
    }

    public Result delete(String alias, String actor) {
        if (!exists(alias)) {
            return Result.fail("No shortcut named '" + alias + "'.");
        }
        shortcuts.remove(alias.toLowerCase());
        storage.save(shortcuts);
        AuditLog.record(AuditEvent.DELETE_SHORTCUT, actor, alias);
        return Result.ok("Deleted shortcut /" + alias);
    }

    public void save() {
        storage.save(shortcuts);
    }

    private static String stripSlash(String command) {
        String trimmed = command == null ? "" : command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }
}
