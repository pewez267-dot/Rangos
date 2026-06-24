package com.fantastickits.data;

import com.fantastickits.FantasticKits;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all kit definitions. Thread-safe via ConcurrentHashMap.
 * Persists to config/fantastickits/kits.json
 */
public class KitData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    private final Map<String, KitDefinition> kits = new ConcurrentHashMap<>();

    public KitData() {
        this.filePath = DataPaths.getConfigDir().resolve("kits.json");
    }

    public synchronized void load() {
        kits.clear();
        if (!Files.exists(filePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("kits")) {
                JsonArray arr = root.getAsJsonArray("kits");
                for (int i = 0; i < arr.size(); i++) {
                    KitDefinition kit = KitDefinition.fromJson(arr.get(i).getAsJsonObject());
                    kits.put(kit.getName().toLowerCase(), kit);
                }
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to load kits.json", e);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(filePath.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (KitDefinition kit : kits.values()) {
                arr.add(kit.toJson());
            }
            root.add("kits", arr);
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to save kits.json", e);
        }
    }

    public KitDefinition getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public boolean kitExists(String name) {
        return kits.containsKey(name.toLowerCase());
    }

    public void addKit(KitDefinition kit) {
        kits.put(kit.getName().toLowerCase(), kit);
        save();
    }

    public void removeKit(String name) {
        kits.remove(name.toLowerCase());
        save();
    }

    public void updateKit(KitDefinition kit) {
        kits.put(kit.getName().toLowerCase(), kit);
        save();
    }

    public Collection<KitDefinition> getAllKits() {
        return kits.values();
    }

    public int getKitCount() {
        return kits.size();
    }
}
