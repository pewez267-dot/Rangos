package com.fantastickits.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Maps a LuckPerms group name to the set of root commands its members are allowed to
 * run, persisted to {@code config/fantastickits/group_commands.json}.
 *
 * <p>Commands are stored as bare root labels (no leading slash, lower-cased), e.g.
 * {@code "fly"}, {@code "heal"}. A command that appears in <em>any</em> group's list
 * becomes "gated": from then on only members of a group that lists it may execute it
 * (see {@code CommandGuard}). Commands that appear in no list are never gated.</p>
 */
public final class GroupCommandStore {

    private static final GroupCommandStore INSTANCE = new GroupCommandStore();

    /** group (lower-case) -> set of allowed root command labels (lower-case). */
    private final Map<String, Set<String>> groups = new LinkedHashMap<>();
    private boolean loaded = false;

    private GroupCommandStore() {
    }

    public static GroupCommandStore get() {
        synchronized (INSTANCE) {
            if (!INSTANCE.loaded) {
                INSTANCE.load();
                INSTANCE.loaded = true;
            }
        }
        return INSTANCE;
    }

    /** Normalises any command-ish string to a bare lower-case root label. */
    public static String normalizeCommand(final String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        final int space = s.indexOf(' ');
        if (space >= 0) {
            s = s.substring(0, space);
        }
        return s.toLowerCase(Locale.ROOT);
    }

    public synchronized void load() {
        this.groups.clear();
        final JsonObject root = JsonIO.read(DataPaths.groupCommands());
        if (!root.has("groups") || !root.get("groups").isJsonObject()) {
            return;
        }
        final JsonObject stored = root.getAsJsonObject("groups");
        for (final Map.Entry<String, JsonElement> entry : stored.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            final Set<String> commands = new LinkedHashSet<>();
            for (final JsonElement element : entry.getValue().getAsJsonArray()) {
                final String command = normalizeCommand(element.getAsString());
                if (!command.isEmpty()) {
                    commands.add(command);
                }
            }
            this.groups.put(entry.getKey().toLowerCase(Locale.ROOT), commands);
        }
    }

    public synchronized void save() {
        final JsonObject root = new JsonObject();
        final JsonObject stored = new JsonObject();
        for (final Map.Entry<String, Set<String>> entry : this.groups.entrySet()) {
            final JsonArray array = new JsonArray();
            for (final String command : entry.getValue()) {
                array.add(command);
            }
            stored.add(entry.getKey(), array);
        }
        root.add("groups", stored);
        JsonIO.write(DataPaths.groupCommands(), root);
    }

    /** Replaces the command list for {@code group}. An empty list removes the group entry. */
    public synchronized void setCommands(final String group, final List<String> commands) {
        if (group == null || group.isBlank()) {
            return;
        }
        final String key = group.toLowerCase(Locale.ROOT);
        if (commands == null || commands.isEmpty()) {
            this.groups.remove(key);
        } else {
            final Set<String> normalized = new LinkedHashSet<>();
            for (final String command : commands) {
                final String normalizedCommand = normalizeCommand(command);
                if (!normalizedCommand.isEmpty()) {
                    normalized.add(normalizedCommand);
                }
            }
            this.groups.put(key, normalized);
        }
        save();
    }

    public synchronized Set<String> commandsFor(final String group) {
        if (group == null) {
            return new LinkedHashSet<>();
        }
        final Set<String> commands = this.groups.get(group.toLowerCase(Locale.ROOT));
        return commands == null ? new LinkedHashSet<>() : new LinkedHashSet<>(commands);
    }

    public synchronized boolean isAllowed(final String group, final String command) {
        if (group == null) {
            return false;
        }
        final Set<String> commands = this.groups.get(group.toLowerCase(Locale.ROOT));
        return commands != null && commands.contains(normalizeCommand(command));
    }

    /** The union of every gated command across all groups. */
    public synchronized Set<String> allGatedCommands() {
        final Set<String> all = new TreeSet<>();
        for (final Set<String> commands : this.groups.values()) {
            all.addAll(commands);
        }
        return all;
    }

    /** Every group that currently has at least one gated command. */
    public synchronized Set<String> allGroups() {
        return new LinkedHashSet<>(this.groups.keySet());
    }

    /** Every group whose allow-list contains {@code command}. */
    public synchronized Set<String> groupsAllowing(final String command) {
        final String normalized = normalizeCommand(command);
        final Set<String> out = new LinkedHashSet<>();
        for (final Map.Entry<String, Set<String>> entry : this.groups.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    public synchronized void removeGroup(final String group) {
        if (group != null && this.groups.remove(group.toLowerCase(Locale.ROOT)) != null) {
            save();
        }
    }
}
