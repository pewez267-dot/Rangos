package com.pewez.fantasticshortcuts.storage;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia de atajos en {@code config/fantasticshortcuts/shortcuts.json}.
 *
 * <p>El fichero arranca como una lista VACÍA: el mod no trae ningún atajo predefinido. El usuario
 * crea los suyos desde la GUI. El formato es JSON legible y editable a mano.
 */
public final class ShortcutStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;

    public ShortcutStorage(Path baseDir) {
        this.file = baseDir.resolve("shortcuts.json");
    }

    public Path file() {
        return file;
    }

    /** Carga la lista de atajos. Si el fichero no existe devuelve una lista vacía. */
    public synchronized List<Shortcut> load() {
        final List<Shortcut> out = new ArrayList<>();
        if (!Files.exists(file)) {
            return out;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root == null || !root.has("shortcuts")) {
                return out;
            }
            final JsonArray array = root.getAsJsonArray("shortcuts");
            for (int i = 0; i < array.size(); i++) {
                final JsonObject o = array.get(i).getAsJsonObject();
                final String alias = getString(o, "alias", "");
                final String command = getString(o, "command", "");
                final String description = getString(o, "description", "");
                final boolean useArgs = o.has("useArgs") ? o.get("useArgs").getAsBoolean()
                        : Shortcut.commandUsesArgs(command);
                final boolean replaceOriginal = o.has("replaceOriginal") && o.get("replaceOriginal").getAsBoolean();
                if (alias.isBlank() || command.isBlank()) {
                    continue;
                }
                out.add(new Shortcut(alias, command, description, useArgs, replaceOriginal));
            }
        } catch (Exception e) {
            FantasticShortcuts.LOGGER.error("[F-Shortcuts] No se pudo leer shortcuts.json: {}", e.toString());
        }
        return out;
    }

    /** Guarda la lista completa de atajos, creando los directorios si faltan. */
    public synchronized void save(List<Shortcut> shortcuts) {
        try {
            Files.createDirectories(file.getParent());
            final JsonObject root = new JsonObject();
            final JsonArray array = new JsonArray();
            for (Shortcut s : shortcuts) {
                final JsonObject o = new JsonObject();
                o.addProperty("alias", s.alias());
                o.addProperty("command", s.command());
                o.addProperty("description", s.description());
                o.addProperty("useArgs", s.useArgs());
                o.addProperty("replaceOriginal", s.replaceOriginal());
                array.add(o);
            }
            root.add("shortcuts", array);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            FantasticShortcuts.LOGGER.error("[F-Shortcuts] No se pudo guardar shortcuts.json: {}", e.toString());
        }
    }

    private static String getString(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }
}
