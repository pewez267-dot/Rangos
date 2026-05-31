package com.arthurleray.invhistory.tracking;

import com.arthurleray.invhistory.InvHistory;
import com.arthurleray.invhistory.config.InvHistoryConfig;
import com.arthurleray.invhistory.data.InventorySnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Decides when to take inventory snapshots: periodic (if changed), join, leave and death. */
public class InventoryTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("invhistory");
    private final Map<UUID, byte[]> lastInventoryHashes = new ConcurrentHashMap<>();
    private int tickCounter = 0;

    public void onServerTick(MinecraftServer server) {
        InvHistoryConfig config = InvHistory.config();
        int intervalTicks = config.getSnapshotIntervalSeconds() * 20;
        if (intervalTicks <= 0) {
            return;
        }
        ++this.tickCounter;
        if (this.tickCounter < intervalTicks) {
            return;
        }
        this.tickCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            this.snapshotIfChanged(player, "periodic");
        }
    }


    public void onPlayerJoin(ServerPlayer player) {
        if (!InvHistory.config().isSnapshotOnJoin()) {
            return;
        }
        this.createSnapshot(player, "join");
    }

    public void onPlayerLeave(ServerPlayer player) {
        if (!InvHistory.config().isSnapshotOnLeave()) {
            return;
        }
        this.createSnapshot(player, "leave");
        this.lastInventoryHashes.remove(player.getUUID());
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (!InvHistory.config().isSnapshotOnDeath()) {
            return;
        }
        this.createSnapshot(player, "death");
    }

    private void snapshotIfChanged(ServerPlayer player, String reason) {
        byte[] currentHash = this.computeInventoryHash(player);
        byte[] previousHash = this.lastInventoryHashes.get(player.getUUID());
        if (previousHash == null || !Arrays.equals(currentHash, previousHash)) {
            this.lastInventoryHashes.put(player.getUUID(), currentHash);
            this.createSnapshot(player, reason);
        }
    }

    private void createSnapshot(ServerPlayer player, String reason) {
        if (!InvHistory.storage().isInitialized()) {
            return;
        }
        InventorySnapshot snapshot = InventorySnapshot.capture(player.getInventory(), reason);
        InvHistory.storage().saveSnapshot(player.getUUID(), player.getGameProfile().getName(), snapshot);
        byte[] hash = this.computeInventoryHash(player);
        this.lastInventoryHashes.put(player.getUUID(), hash);
    }


    private byte[] computeInventoryHash(ServerPlayer player) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                CompoundTag tag = new CompoundTag();
                stack.save(tag);
                md.update((byte) i);
                md.update(tag.toString().getBytes());
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("MD5 not available", e);
            return new byte[0];
        }
    }
}
