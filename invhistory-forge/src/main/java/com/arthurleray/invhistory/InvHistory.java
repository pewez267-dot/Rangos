package com.arthurleray.invhistory;

import com.arthurleray.invhistory.config.ConfigManager;
import com.arthurleray.invhistory.config.InvHistoryConfig;
import com.arthurleray.invhistory.data.SnapshotStorage;
import com.arthurleray.invhistory.tracking.InventoryTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic core. Holds the config manager, snapshot storage and the inventory tracker.
 */
public class InvHistory {
    public static final String MOD_ID = "invhistory";
    private static final Logger LOGGER = LoggerFactory.getLogger("invhistory");
    private static final ConfigManager configManager = new ConfigManager();
    private static final SnapshotStorage snapshotStorage = new SnapshotStorage();
    private static final InventoryTracker inventoryTracker = new InventoryTracker();

    public static void init() {
        LOGGER.info("InvHistory initializing...");
        configManager.load();
        LOGGER.info("InvHistory initialized. Snapshot interval: {}s, Max snapshots: {}",
                configManager.getConfig().getSnapshotIntervalSeconds(),
                configManager.getConfig().getMaxSnapshotsPerPlayer());
    }

    public static InvHistoryConfig config() {
        return configManager.getConfig();
    }

    public static SnapshotStorage storage() {
        return snapshotStorage;
    }

    public static InventoryTracker tracker() {
        return inventoryTracker;
    }
}
