package com.fantastickits.data;

import com.fantastickits.FantasticKits;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the mapping of LuckPerms groups to allowed commands.
 * Persists to config/fantastickits/group_commands.json
 */
public class GroupCommandData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;
    // Map of group name (lowercase) -> set of allowed command names
    private final Map<String, Set<String>> groupCommands = new ConcurrentHashMap<>();

    public GroupCommandData() {
        this.filePath = DataPaths.getConfigDir().resolve("group_commands.json");
    }

    public synchronized void load() {
        groupCommands.clear();
        if (!Files.exists(filePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String group = entry.getKey().toLowerCase();
                JsonArray arr = entry.getValue().getAsJsonArray();
                Set<String> commands = ConcurrentHashMap.newKeySet();
                for (int i = 0; i < arr.size(); i++) {
                    commands.add(arr.get(i).getAsString());
                }
                groupCommands.put(group, commands);
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to load group_commands.json", e);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(filePath.getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<String, Set<String>> entry : groupCommands.entrySet()) {
                JsonArray arr = new JsonArray();
                List<String> sorted = new ArrayList<>(entry.getValue());
                Collections.sort(sorted);
                for (String cmd : sorted) {
                    arr.add(cmd);
                }
                root.add(entry.getKey(), arr);
            }
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to save group_commands.json", e);
        }
    }

    /**
     * Get the set of commands allowed for a group.
     */
    public Set<String> getCommandsForGroup(String group) {
        Set<String> commands = groupCommands.get(group.toLowerCase());
        return commands != null ? new HashSet<>(commands) : new HashSet<>();
    }

    /**
     * Set the commands allowed for a group (replaces existing).
     */
    public synchronized void setCommandsForGroup(String group, Set<String> commands) {
        Set<String> set = ConcurrentHashMap.newKeySet();
        set.addAll(commands);
        groupCommands.put(group.toLowerCase(), set);
        save();
    }

    /**
     * Add a single command to a group's allowed list.
     */
    public synchronized void addCommandToGroup(String group, String command) {
        groupCommands.computeIfAbsent(group.toLowerCase(), k -> ConcurrentHashMap.newKeySet())
                .add(command);
        save();
    }

    /**
     * Remove a command from a group's allowed list.
     */
    public synchronized void removeCommandFromGroup(String group, String command) {
        Set<String> commands = groupCommands.get(group.toLowerCase());
        if (commands != null) {
            commands.remove(command);
            save();
        }
    }

    /**
     * Check if a command is allowed for a given group.
     */
    public boolean isCommandAllowed(String group, String command) {
        Set<String> commands = groupCommands.get(group.toLowerCase());
        return commands != null && commands.contains(command);
    }

    /**
     * Get all groups that have command mappings.
     */
    public Set<String> getAllGroups() {
        return new HashSet<>(groupCommands.keySet());
    }

    /**
     * Remove all command mappings for a group.
     */
    public synchronized void removeGroup(String group) {
        groupCommands.remove(group.toLowerCase());
        save();
    }
}
