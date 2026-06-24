package com.fantasticshortcuts.data;

import com.fantasticshortcuts.FantasticShortcuts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Authoritative in-memory cache of all shortcuts, indexed by alias for O(1) resolution
 * at execution time. The cache is built once at load and updated on every create / edit /
 * delete. All public methods are {@code synchronized} and return detached copies, and all
 * persistence is delegated to {@link ShortcutSerializer} (asynchronous + atomic), so the
 * store is safe under concurrent admin operations.
 */
public final class ShortcutManager {

    private static final ShortcutManager INSTANCE = new ShortcutManager();

    private final ShortcutSerializer serializer = new ShortcutSerializer();
    private final Map<String, Shortcut> byId = new LinkedHashMap<>();
    private final Map<String, Shortcut> byAlias = new LinkedHashMap<>();
    private boolean loaded = false;

    private ShortcutManager() {
    }

    public static ShortcutManager get() {
        synchronized (INSTANCE) {
            if (!INSTANCE.loaded) {
                INSTANCE.loadInternal();
                INSTANCE.loaded = true;
            }
        }
        return INSTANCE;
    }

    private void loadInternal() {
        this.byId.clear();
        this.byAlias.clear();
        for (final Shortcut s : this.serializer.load()) {
            this.byId.put(s.getId(), s);
            this.byAlias.put(s.aliasKey(), s);
        }
        FantasticShortcuts.LOGGER.info("[FantasticShortcuts] {} shortcut(s) cargados.", this.byId.size());
    }

    public synchronized void reload() {
        loadInternal();
    }

    public synchronized List<Shortcut> all() {
        final List<Shortcut> out = new ArrayList<>();
        for (final Shortcut s : this.byId.values()) {
            out.add(s.copy());
        }
        return out;
    }

    public synchronized Shortcut byAlias(final String alias) {
        final Shortcut s = this.byAlias.get(Shortcut.stripSlash(alias).toLowerCase(Locale.ROOT));
        return s == null ? null : s.copy();
    }

    public synchronized Shortcut byId(final String id) {
        final Shortcut s = this.byId.get(id);
        return s == null ? null : s.copy();
    }

    public synchronized boolean aliasExists(final String alias) {
        return this.byAlias.containsKey(Shortcut.stripSlash(alias).toLowerCase(Locale.ROOT));
    }

    /** Adds or replaces a shortcut (keyed by id), re-indexing its alias, then persists. */
    public synchronized void put(final Shortcut shortcut) {
        final Shortcut existing = this.byId.get(shortcut.getId());
        if (existing != null) {
            this.byAlias.remove(existing.aliasKey());
        }
        final Shortcut stored = shortcut.copy();
        this.byId.put(stored.getId(), stored);
        this.byAlias.put(stored.aliasKey(), stored);
        persist();
    }

    public synchronized boolean remove(final String id) {
        final Shortcut removed = this.byId.remove(id);
        if (removed == null) {
            return false;
        }
        this.byAlias.remove(removed.aliasKey());
        persist();
        return true;
    }

    public synchronized Set<String> aliasKeys() {
        return new TreeSet<>(this.byAlias.keySet());
    }

    /** Root literals (e.g. "gamemode") of every shortcut whose original is to be replaced. */
    public synchronized Set<String> replacedOriginalRootLiterals() {
        final Set<String> out = new TreeSet<>();
        for (final Shortcut s : this.byId.values()) {
            if (s.isReplaceOriginal()) {
                final String root = s.originalRootLiteral();
                if (!root.isEmpty()) {
                    out.add(root);
                }
            }
        }
        return out;
    }

    private void persist() {
        this.serializer.saveAsync(new ArrayList<>(this.byId.values()));
    }

    public synchronized void shutdown() {
        this.serializer.shutdown();
    }
}
