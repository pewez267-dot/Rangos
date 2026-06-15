package com.pewez.fantasticshortcuts.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists shortcuts to {@code config/fantasticshortcuts/shortcuts.json}.
 */
public final class ShortcutStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type LIST_TYPE = new TypeToken<List<Shortcut>>() {
    }.getType();

    private Path file;

    public void init(Path configDir) {
        this.file = configDir.resolve("shortcuts.json");
    }

    /**
     * Load shortcuts from disk into an ordered map keyed by alias. The mod ships with NO predefined
     * shortcuts: it is a system for the administrator to define their own. On first run an empty file
     * is created.
     */
    public Map<String, Shortcut> load() {
        Map<String, Shortcut> result = new LinkedHashMap<>();
        if (file == null) {
            return result;
        }
        if (!Files.exists(file)) {
            save(result); // create an empty shortcuts.json
            return result;
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            List<Shortcut> list = GSON.fromJson(content, LIST_TYPE);
            if (list != null) {
                for (Shortcut shortcut : list) {
                    if (shortcut != null && shortcut.alias != null && !shortcut.alias.isBlank()) {
                        result.put(shortcut.alias.toLowerCase(), shortcut);
                    }
                }
            }
        } catch (Exception e) {
            FantasticShortcutsMod.LOGGER.error("Failed to read shortcuts.json", e);
        }
        return result;
    }

    public void save(Map<String, Shortcut> shortcuts) {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            List<Shortcut> list = new ArrayList<>(shortcuts.values());
            Files.writeString(file, GSON.toJson(list, LIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FantasticShortcutsMod.LOGGER.error("Failed to write shortcuts.json", e);
        }
    }
}
