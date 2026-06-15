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
     * Load shortcuts from disk into an ordered map keyed by alias. Creates defaults on first run.
     */
    public Map<String, Shortcut> load() {
        Map<String, Shortcut> result = new LinkedHashMap<>();
        if (file == null) {
            return result;
        }
        if (!Files.exists(file)) {
            for (Shortcut shortcut : defaults()) {
                result.put(shortcut.alias.toLowerCase(), shortcut);
            }
            save(result);
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

    private static List<Shortcut> defaults() {
        List<Shortcut> list = new ArrayList<>();
        list.add(describe(new Shortcut("gc", "gamemode creative"), "Set creative mode", true));
        list.add(describe(new Shortcut("gs", "gamemode survival"), "Set survival mode", true));
        list.add(describe(new Shortcut("ga", "gamemode adventure"), "Set adventure mode", true));
        list.add(describe(new Shortcut("ge", "gamemode spectator"), "Set spectator mode", true));
        list.add(describe(new Shortcut("day", "time set day"), "Set time to day", false));
        list.add(describe(new Shortcut("night", "time set night"), "Set time to night", false));
        Shortcut tp = new Shortcut("tpp", "tp {args}");
        tp.description = "Teleport using {args}, e.g. /tpp Steve";
        tp.allowArguments = true;
        list.add(tp);
        return list;
    }

    private static Shortcut describe(Shortcut shortcut, String description, boolean allowArgs) {
        shortcut.description = description;
        shortcut.allowArguments = allowArgs;
        return shortcut;
    }
}
