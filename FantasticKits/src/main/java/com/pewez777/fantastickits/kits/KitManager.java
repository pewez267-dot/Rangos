/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.kits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import com.pewez777.fantastickits.storage.KitRepository;
import com.pewez777.fantastickits.storage.PlayerDataRepository;

import org.slf4j.Logger;

/**
 * Authoritative, server-side in-memory registry of kits, backed by
 * {@link KitRepository}. Also exposes the shared {@link PlayerDataRepository}.
 *
 * <p>Kits are keyed by a normalized, filesystem-safe form of their display
 * name, guaranteeing uniqueness and a stable storage path.</p>
 */
public final class KitManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile KitManager instance;

    private final KitRepository kitRepository = new KitRepository();
    private final PlayerDataRepository playerRepository = new PlayerDataRepository();
    private final ConcurrentHashMap<String, Kit> kitsByKey = new ConcurrentHashMap<>();

    private KitManager() {
    }

    public static KitManager get() {
        KitManager local = instance;
        if (local == null) {
            synchronized (KitManager.class) {
                local = instance;
                if (local == null) {
                    local = new KitManager();
                    instance = local;
                }
            }
        }
        return local;
    }

    public PlayerDataRepository players() {
        return playerRepository;
    }

    /** Reloads all kits from disk, replacing the in-memory state. */
    public synchronized void reload() {
        kitsByKey.clear();
        List<Kit> loaded = kitRepository.loadAll();
        for (Kit kit : loaded) {
            kitsByKey.put(kit.storageKey(), kit);
        }
        LOGGER.info("[F-Kits] Loaded {} kit(s) from disk.", kitsByKey.size());
    }

    public Collection<Kit> getAll() {
        return new ArrayList<>(kitsByKey.values());
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<>();
        for (Kit kit : kitsByKey.values()) {
            names.add(kit.getName());
        }
        return names;
    }

    public Optional<Kit> getByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(kitsByKey.get(normalizeName(name)));
    }

    public Optional<Kit> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Kit kit : kitsByKey.values()) {
            if (id.equals(kit.getId())) {
                return Optional.of(kit);
            }
        }
        return Optional.empty();
    }

    public boolean exists(String name) {
        return name != null && kitsByKey.containsKey(normalizeName(name));
    }

    /** Persists and registers a kit. Returns {@code true} on success. */
    public synchronized boolean save(Kit kit) {
        if (kit == null || kit.getName().isBlank()) {
            return false;
        }
        boolean ok = kitRepository.save(kit);
        if (ok) {
            kitsByKey.put(kit.storageKey(), kit);
        }
        return ok;
    }

    /** Deletes a kit by name. Returns the removed kit when present. */
    public synchronized Optional<Kit> delete(String name) {
        Optional<Kit> existing = getByName(name);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Kit kit = existing.get();
        kitRepository.delete(kit.storageKey());
        kitsByKey.remove(kit.storageKey());
        return Optional.of(kit);
    }

    /**
     * Returns the set of kits that own a given command (case-insensitive on the
     * command's first literal). Used by the runtime command barrier.
     */
    public List<Kit> kitsOwningCommand(String commandLiteral) {
        List<Kit> result = new ArrayList<>();
        if (commandLiteral == null) {
            return result;
        }
        String needle = commandLiteral.toLowerCase(Locale.ROOT);
        for (Kit kit : kitsByKey.values()) {
            for (String owned : kit.getCommands()) {
                String firstLiteral = owned.trim().toLowerCase(Locale.ROOT);
                int space = firstLiteral.indexOf(' ');
                if (space > 0) {
                    firstLiteral = firstLiteral.substring(0, space);
                }
                if (firstLiteral.equals(needle)) {
                    result.add(kit);
                    break;
                }
            }
        }
        return result;
    }

    /** Normalizes a kit name into a stable, filesystem-safe storage key. */
    public static String normalizeName(String name) {
        if (name == null) {
            return "unnamed";
        }
        String lower = name.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('_');
            }
            // all other characters (color codes, symbols) are dropped
        }
        String out = sb.toString();
        return out.isEmpty() ? "unnamed" : out;
    }
}
