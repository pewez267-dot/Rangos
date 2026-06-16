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

    /** Compact dimension: path only for vanilla, full id for modded dimensions. */
    public static String dimShort(Level level) {
        if (level == null) {
            return "unknown";
        }
        ResourceLocation rl = level.dimension().location();
        return "minecraft".equals(rl.getNamespace()) ? rl.getPath() : rl.toString();
    }

    /** @return the player's name with a {@code  (OP)} suffix when they are an operator. */
    private static String nameWithOp(ServerPlayer player) {
        return player.getGameProfile().getName() + (isOp(player) ? " (OP)" : "");
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

        String payload = uid + " " + itemId + " x" + stack.getCount()
                + " by " + opName
                + " @(" + posStr + ") " + dimShort(op.level())
                + " via " + method;
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
        String dimStr = dimShort(level);

        TrackingIndex.get().put(uid, new TrackingIndex.IndexEntry(
                opUuid.toString(), opName, itemId, 1, spawnedAt, logFilePath(opUuid)));

        String spawnPayload = uid + " " + itemId + " x1"
                + " by " + opName
                + " @(" + posStr + ") " + dimStr
                + " via block_place";
        WatchLogger.get().record(opUuid, "ITEM_SPAWNED", spawnPayload);

        String placedPayload = uid + " " + itemId
                + " @(" + posStr + ") " + dimStr
                + " by " + opName;
        WatchLogger.get().record(opUuid, "ITEM_PLACED", placedPayload);

        CreativeSessionHandler.incrementSpawned(opUuid);
    }

    // ---- Lifecycle events ---------------------------------------------------------------------

    public void onDropped(ItemStack stack, ServerPlayer dropper) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        String payload = mark.uid() + " " + itemId(stack) + " x" + stack.getCount()
                + " by " + dropper.getGameProfile().getName()
                + " @(" + pos(dropper) + ") " + dimShort(dropper.level());
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_DROPPED", payload);
    }

    public void onPickedUp(ItemStack stack, ServerPlayer picker) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        String payload = mark.uid() + " " + itemId(stack) + " x" + stack.getCount()
                + " by " + nameWithOp(picker)
                + " @(" + pos(picker) + ") " + dimShort(picker.level());
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_PICKED_UP", payload);

        maybeRecordTransfer(stack, mark, picker, "drop_pickup", pos(picker));
    }

    public void onStored(ItemStack stack, int quantity, ServerPlayer player, String containerType,
                         String containerPos, String dim) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        String payload = mark.uid() + " " + itemId(stack) + " x" + quantity
                + " by " + player.getGameProfile().getName()
                + " -> " + containerType
                + " @(" + containerPos + ") " + dim;
        WatchLogger.get().record(mark.spawnedBy(), "ITEM_STORED", payload);
    }

    public void onRetrieved(ItemStack stack, int quantity, ServerPlayer player, String containerType,
                            String containerPos, String dim) {
        NbtUtil.MarkData mark = NbtUtil.toMarkData(stack);
        if (mark == null || mark.spawnedBy() == null) {
            return;
        }
        String payload = mark.uid() + " " + itemId(stack) + " x" + quantity
                + " by " + nameWithOp(player)
                + " <- " + containerType
                + " @(" + containerPos + ") " + dim;
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
        String payload = mark.uid() + " " + itemId + " x" + quantity
                + " by " + player.getGameProfile().getName()
                + " " + method
                + " @(" + pos(player) + ") " + dimShort(player.level());
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
        String payload = mark.uid() + " " + itemId(stack) + " x" + stack.getCount()
                + " on " + nameWithOp(player)
                + " slot=" + slot;
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

        String payload = mark.uid() + " " + itemId(stack) + " x" + stack.getCount()
                + " " + fromName + " -> " + nameWithOp(newHolder)
                + " via " + method
                + " @(" + posStr + ")";
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
