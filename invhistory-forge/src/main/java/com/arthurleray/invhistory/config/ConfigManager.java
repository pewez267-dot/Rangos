package com.arthurleray.invhistory.config;

import com.arthurleray.invhistory.InvHistoryPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads/saves the YAML config (invhistory.yml) via snakeyaml - identical to the original. */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("invhistory");
    private static final String CONFIG_FILE = "invhistory.yml";
    private InvHistoryConfig config;

    public void load() {
        Path configPath = InvHistoryPlatform.get().getConfigDir().resolve(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            this.config = new InvHistoryConfig();
            this.save();
            LOGGER.info("Created default config at {}", configPath);
            return;
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(in);
            this.config = this.parseConfig(data);
            LOGGER.info("Loaded config from {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to load config, using defaults", e);
            this.config = new InvHistoryConfig();
        }
    }


    public void save() {
        Path configPath = InvHistoryPlatform.get().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.createDirectories(configPath.getParent());
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                yaml.dump(this.configToMap(this.config), writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public InvHistoryConfig getConfig() {
        return this.config;
    }

    private InvHistoryConfig parseConfig(Map<String, Object> data) {
        InvHistoryConfig cfg = new InvHistoryConfig();
        if (data == null) {
            return cfg;
        }
        if (data.containsKey("snapshot-interval")) {
            cfg.setSnapshotIntervalSeconds(((Number) data.get("snapshot-interval")).intValue());
        }
        if (data.containsKey("max-snapshots-per-player")) {
            cfg.setMaxSnapshotsPerPlayer(((Number) data.get("max-snapshots-per-player")).intValue());
        }
        if (data.containsKey("storage")) {
            cfg.setStorage((String) data.get("storage"));
        }
        if (data.containsKey("permission-level")) {
            cfg.setPermissionLevel(((Number) data.get("permission-level")).intValue());
        }
        Object snapshotOn = data.get("snapshot-on");
        if (snapshotOn instanceof Map) {
            Map<?, ?> events = (Map<?, ?>) snapshotOn;
            if (events.containsKey("player-join")) {
                cfg.setSnapshotOnJoin(Boolean.TRUE.equals(events.get("player-join")));
            }
            if (events.containsKey("player-leave")) {
                cfg.setSnapshotOnLeave(Boolean.TRUE.equals(events.get("player-leave")));
            }
            if (events.containsKey("player-death")) {
                cfg.setSnapshotOnDeath(Boolean.TRUE.equals(events.get("player-death")));
            }
        }
        return cfg;
    }


    private Map<String, Object> configToMap(InvHistoryConfig cfg) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("snapshot-interval", cfg.getSnapshotIntervalSeconds());
        map.put("max-snapshots-per-player", cfg.getMaxSnapshotsPerPlayer());
        LinkedHashMap<String, Boolean> events = new LinkedHashMap<>();
        events.put("player-join", cfg.isSnapshotOnJoin());
        events.put("player-leave", cfg.isSnapshotOnLeave());
        events.put("player-death", cfg.isSnapshotOnDeath());
        map.put("snapshot-on", events);
        map.put("storage", cfg.getStorage());
        map.put("permission-level", cfg.getPermissionLevel());
        return map;
    }
}
