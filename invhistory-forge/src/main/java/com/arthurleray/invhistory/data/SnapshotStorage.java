package com.arthurleray.invhistory.data;

import com.arthurleray.invhistory.InvHistory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Per-player snapshot storage in {@code <world>/invhistory/<uuid>.dat} (gzipped NBT). */
public class SnapshotStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("invhistory");
    private Path storageDir;

    public void init(Path worldDir) {
        this.storageDir = worldDir.resolve("invhistory");
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create invhistory storage directory", e);
        }
    }

    public boolean isInitialized() {
        return this.storageDir != null;
    }


    public void saveSnapshot(UUID playerUuid, String playerName, InventorySnapshot snapshot) {
        File file = this.storageDir.resolve(playerUuid.toString() + ".dat").toFile();
        int maxSnapshots = InvHistory.config().getMaxSnapshotsPerPlayer();
        CompoundTag root;
        if (file.exists()) {
            try {
                root = NbtIo.readCompressed(file);
            } catch (IOException e) {
                LOGGER.error("Failed to read snapshot file for {}, creating new", playerUuid, e);
                root = new CompoundTag();
            }
        } else {
            root = new CompoundTag();
        }
        root.putString("playerName", playerName);
        ListTag snapshots = root.getList("snapshots", 10);
        snapshots.add(snapshot.toNbt());
        while (snapshots.size() > maxSnapshots) {
            snapshots.remove(0);
        }
        root.put("snapshots", snapshots);
        try {
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            LOGGER.error("Failed to save snapshot for {}", playerUuid, e);
        }
    }

    public List<InventorySnapshot> loadSnapshots(UUID playerUuid) {
        File file = this.storageDir.resolve(playerUuid.toString() + ".dat").toFile();
        if (!file.exists()) {
            return Collections.emptyList();
        }
        try {
            CompoundTag root = NbtIo.readCompressed(file);
            ListTag snapshots = root.getList("snapshots", 10);
            ArrayList<InventorySnapshot> result = new ArrayList<>();
            for (int i = 0; i < snapshots.size(); ++i) {
                result.add(InventorySnapshot.fromNbt(snapshots.getCompound(i)));
            }
            return result;
        } catch (IOException e) {
            LOGGER.error("Failed to load snapshots for {}", playerUuid, e);
            return Collections.emptyList();
        }
    }
}
