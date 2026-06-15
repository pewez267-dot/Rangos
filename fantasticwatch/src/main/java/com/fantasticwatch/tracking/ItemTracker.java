package com.fantasticwatch.tracking;

import com.fantasticwatch.events.CreativeSessionHandler;
import com.fantasticwatch.logging.WatchLogger;
import com.fantasticwatch.util.NbtUtil;
import com.fantasticwatch.util.UidGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.Instant;
import java.util.UUID;

/**
 * Core forensic tracking engine.
 *
 * <p>Owns the act of marking an item, writing every lifecycle event to the originating
 * operator's log, keeping the item's embedded {@code current_owner}/{@code transfer_count}
 * up to date, and maintaining the global {@link TrackingIndex}. Every event is always written to
 * the log of the operator who originally spawned the item, regardless of who currently holds it.</p>
 */
public final class ItemTracker {

    private static final ItemTracker INSTANCE = new ItemTracker();

    private ItemTracker() {
    }

    public static ItemTracker get() {
        return INSTANCE;
    }

    // ---- Shared serialization helpers (used by handlers too) ----------------------------------

    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air";
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null ? key.toString() : "unknown:unregistered_item";
    }

    public static String pos(BlockPos pos) {
        return pos == null ? "0,0,0" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String pos(Entity entity) {
        return entity == null ? "0,0,0" : pos(entity.blockPosition());
    }

    public static String dim(Level level) {
        return level == null ? "unknown:unknown" : level.dimension().location().toString();
    }

    public static boolean isOp(Player player) {
        // Permission level 4 is the maximum (full operator) level in vanilla/Forge.
        return player != null && player.hasPermissions(4);
    }

    /** @return the canonical relative log path recorded in the index for an operator. */
    public static String logFilePath(UUID opUuid) {
        return "config/fantasticwatch/ops/" + opUuid + ".log";
    }

    // ---- Expiry (lazy NBT strip on encounter) -------------------------------------------------

    /**
     * If the stack's mark predates the active purge boundary, strip the mark and drop its index
     * entry so it stops being tracked. Scanning every chunk for expired items is infeasible and
     * unsafe for a production server, so expired marks are removed lazily the moment a tracked
     * item is next encountered (pickup, login scan, container move, etc.).
     *
     * @return {@code true} if the stack was expired and has been untracked
     */
    public boolean stripIfExpired(ItemStack stack) {
        if (!NbtUtil.isTracked(stack)) {
            return false;
        }
        String spawnedAt = NbtUtil.getSpawnedAt(stack);
        Instant spawned = parseInstant(spawnedAt);
        Instant cutoff = LifecycleManager.purgeCutoff();
        if (spawned != null && spawned.isBefore(cutoff)) {
            String uid = NbtUtil.getUid(stack);
            NbtUtil.removeMark(stack);
            if (uid != null) {
                TrackingIndex.get().remove(uid);
            }
            WatchLogger.get().system("[EXPIRED_STRIP] uid=" + uid + " spawned_at=" + spawnedAt);
            return true;
        }
        return false;
    }

    // ---- Spawn / mark -------------------------------------------------------------------------

    /**
     * Marks an item leaving an operator's creative inventory (if not already marked) and logs the
     * spawn. Idempotent: an already-tracked stack is left untouched.
     *
     * @return the item's uid (existing or newly generated), or {@code null} for an empty stack
     */
    public String markAndLogSpawn(ServerPlayer op, ItemStack stack, String method) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (NbtUtil.isTracked(stack)) {
            return NbtUtil.getUid(stack);
        }
        UUID opUuid = op.getUUID();
        String opName = op.getGameProfile().getName();
        String uid = UidGenerator.generate(opUuid);
        String spawnedAt = WatchLogger.nowIso();
        String posStr = pos(op);
        String dimStr = dim(op.level());
        String itemId = itemId(stack);

        NbtUtil.writeMark(stack, uid, opUuid, opName, spawnedAt, posStr, dimStr);

        TrackingIndex.get().put(uid, new TrackingIndex.IndexEntry(
                opUuid.toString(), opName, itemId, stack.getCount(), spawnedAt, logFilePath(opUuid)));

        String payload = "uid={" + uid + "}"
                + " item_id={" + itemId + "}"
                + " quantity={" + stack.getCount() + "}"
                + " op={" + opName + "}"
                + " uuid={" + opUuid + "}"
                + " pos={" + posStr + "}"
                + " dim={" + dimStr + "}"
                + " method={" + method + "}";
        WatchLogger.get().record(opUuid, "ITEM_SPAWNED", payload);

        CreativeSessionHandler.incrementSpawned(opUuid);
        return uid;
    }

    /**
     * Records a block placed by a creative operator. Blocks cannot carry the item NBT mark, so the
     * placement is tracked purely through the log and index (a uid is still generated so the event
     * is cross-referenceable).
     */
    public void logBlockPlacement(ServerPlayer op, String itemId, BlockPos blockPos, Level level) {
        UUID opUuid = op.getUUID();
        String opName = op.getGameProfile().getName();
        String uid = UidGenerator.generate(opUuid);
        String spawnedAt = WatchLogger.nowIso();
        String posStr = pos(blockPos);
        String dimStr = dim(level);

        TrackingIndex.get().put(uid, new TrackingIndex.IndexEntry(
                opUuid.toString(), opName, itemId, 1, spawnedAt, logFilePath(opUuid)));

        String spawnPayload = "uid={" + uid + "}"
                + " item_id={" + itemId + "}"
                + " quantity={1}"
                + " op={" + opName + "}"
                + " uuid={" + opUuid + "}"
                + " pos={" + posStr + "}"
                + " dim={" + dimStr + "}"
                + " method={block_place}";
        WatchLogger.get().record(opUuid, "ITEM_SPAWNED", spawnPayload);

        String placedPayload = "uid={" + uid + "}"
                + " item_id={" + itemId + "}"
                + " pos={" + posStr + "}"
                + " dim={" + dimStr + "}"
                + " placed_by={" + opName + "}"
                + " placed_by_uuid={" + opUuid + "}"
                + " timestamp={" + spawnedAt + "}";
        WatchLogger.get().record(opUuid, "ITEM_PLACED", placedPayload);

        CreativeSessionHandler.incrementSpawned(opUuid);
    }

    // ---- Lifecycle events ---------------------------------------------------------------------

    public void onDropped(ItemStack stack, ServerPlayer dropper) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId(stack) + "}"
                + " quantity={" + stack.getCount() + "}"
                + " dropped_by={" + dropper.getGameProfile().getName() + "}"
                + " dropped_by_uuid={" + dropper.getUUID() + "}"
                + " pos={" + pos(dropper) + "}"
                + " dim={" + dim(dropper.level()) + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_DROPPED", payload);
    }

    public void onPickedUp(ItemStack stack, ServerPlayer picker) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        boolean isOp = isOp(picker);
        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId(stack) + "}"
                + " quantity={" + stack.getCount() + "}"
                + " picked_by={" + picker.getGameProfile().getName() + "}"
                + " picked_by_uuid={" + picker.getUUID() + "}"
                + " is_op={" + isOp + "}"
                + " pos={" + pos(picker) + "}"
                + " dim={" + dim(picker.level()) + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_PICKED_UP", payload);

        maybeRecordTransfer(stack, mark, picker, "drop_pickup", pos(picker));
    }

    public void onStored(ItemStack stack, int quantity, ServerPlayer player, String containerType,
                         String containerPos, String dim) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId(stack) + "}"
                + " quantity={" + quantity + "}"
                + " stored_by={" + player.getGameProfile().getName() + "}"
                + " stored_by_uuid={" + player.getUUID() + "}"
                + " container_type={" + containerType + "}"
                + " container_pos={" + containerPos + "}"
                + " dim={" + dim + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_STORED", payload);
    }

    public void onRetrieved(ItemStack stack, int quantity, ServerPlayer player, String containerType,
                            String containerPos, String dim) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        boolean isOp = isOp(player);
        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId(stack) + "}"
                + " quantity={" + quantity + "}"
                + " retrieved_by={" + player.getGameProfile().getName() + "}"
                + " retrieved_by_uuid={" + player.getUUID() + "}"
                + " is_op={" + isOp + "}"
                + " container_type={" + containerType + "}"
                + " container_pos={" + containerPos + "}"
                + " dim={" + dim + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_RETRIEVED", payload);

        maybeRecordTransfer(stack, mark, player, "container", containerPos);
    }

    public void onConsumed(ItemStack stack, ServerPlayer player, String method) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        // Capture the id up-front: consumption may empty the stack before we finish logging.
        String itemId = itemId(stack);
        int quantity = Math.max(1, stack.getCount());
        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId + "}"
                + " quantity={" + quantity + "}"
                + " consumed_by={" + player.getGameProfile().getName() + "}"
                + " consumed_by_uuid={" + player.getUUID() + "}"
                + " method={" + method + "}"
                + " pos={" + pos(player) + "}"
                + " dim={" + dim(player.level()) + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_CONSUMED", payload);

        // Only end the lifecycle when the marked stack is fully depleted; partial consumption of a
        // stack (e.g. eating one of several marked apples) leaves the remainder tracked.
        if (stack.isEmpty()) {
            LifecycleManager.logLifecycleEnd(mark, itemId, player.getGameProfile().getName(),
                    player.getUUID(), method);
            TrackingIndex.get().remove(mark.uid());
        }
    }

    public void onFoundOnLogin(ItemStack stack, ServerPlayer player, String slot) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        boolean isOp = isOp(player);
        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId(stack) + "}"
                + " quantity={" + stack.getCount() + "}"
                + " found_on={" + player.getGameProfile().getName() + "}"
                + " found_on_uuid={" + player.getUUID() + "}"
                + " is_op={" + isOp + "}"
                + " slot={" + slot + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_FOUND_ON_LOGIN", payload);
    }

    /**
     * If the holder differs from the recorded current owner, update the embedded owner and
     * transfer count on the item NBT and log an {@code ITEM_TRANSFERRED} event to the origin log.
     */
    private void maybeRecordTransfer(ItemStack stack, NbtUtil.MarkData mark, ServerPlayer newHolder,
                                     String method, String posStr) {
        UUID currentOwner = mark.currentOwner();
        if (currentOwner != null && currentOwner.equals(newHolder.getUUID())) {
            return; // no change of hands
        }
        NbtUtil.setCurrentOwner(stack, newHolder.getUUID());
        NbtUtil.incrementTransferCount(stack);

        MinecraftServer server = newHolder.getServer();
        String fromName = resolveName(server, currentOwner);
        String fromUuid = currentOwner != null ? currentOwner.toString() : "unknown";

        String payload = "uid={" + mark.uid() + "}"
                + " item_id={" + itemId(stack) + "}"
                + " quantity={" + stack.getCount() + "}"
                + " from={" + fromName + "}"
                + " from_uuid={" + fromUuid + "}"
                + " to={" + newHolder.getGameProfile().getName() + "}"
                + " to_uuid={" + newHolder.getUUID() + "}"
                + " to_is_op={" + isOp(newHolder) + "}"
                + " method={" + method + "}"
                + " pos={" + posStr + "}"
                + " timestamp={" + WatchLogger.nowIso() + "}";
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_TRANSFERRED", payload);
    }

    private static String resolveName(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return "unknown";
        }
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        if (server.getProfileCache() != null) {
            return server.getProfileCache().get(uuid)
                    .map(profile -> profile.getName())
                    .orElse("unknown");
        }
        return "unknown";
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
