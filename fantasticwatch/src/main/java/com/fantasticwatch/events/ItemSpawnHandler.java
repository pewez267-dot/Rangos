package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.tracking.ItemTracker;
import com.fantasticwatch.util.NbtUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the tracking mark at the moment items leave an operator's creative inventory.
 *
 * <p>Two complementary capture paths are used, both verified against stable Forge events:</p>
 * <ul>
 *   <li><b>Inventory scan</b> — Forge 1.20.1 exposes no event for the creative "set slot" packet,
 *       so a throttled server-side scan of each operator-in-creative inventory tags any untagged
 *       stack ({@code method=inventory_pick}). Because every item present in a creative operator's
 *       inventory originated from creative, marking untagged stacks there is correct and
 *       conservative for forensic purposes.</li>
 *   <li><b>Block placement</b> — {@link BlockEvent.EntityPlaceEvent} captures blocks placed by an
 *       operator in creative ({@code method=block_place}). Placed blocks cannot carry the item NBT,
 *       so they are tracked through the log/index instead.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = FantasticWatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemSpawnHandler {

    /** Scan cadence in ticks (20 ticks ≈ 1 second) to keep per-tick overhead negligible. */
    private static final int SCAN_INTERVAL_TICKS = 20;

    /** Per-operator snapshot of stackable item totals (item id → count) used to detect spawns. */
    private static final ConcurrentHashMap<UUID, Map<String, Integer>> STACKABLE_BASELINE = new ConcurrentHashMap<>();

    private ItemSpawnHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        if (!player.isCreative() || !ItemTracker.isOp(player)) {
            // Not (or no longer) an operator in creative: drop any stale spawn baseline.
            STACKABLE_BASELINE.remove(player.getUUID());
            return;
        }

        Inventory inv = player.getInventory();
        scanList(player, inv.items);
        scanList(player, inv.armor);
        scanList(player, inv.offhand);

        // Detect stackable items materialised from creative (no NBT, so stacking is preserved).
        if (WatchConfig.LOG_STACKABLE_SPAWNS.get()) {
            logStackableSpawns(player, inv);
        } else {
            STACKABLE_BASELINE.remove(player.getUUID());
        }
    }

    /**
     * Detects net increases of stackable items in an operator's inventory (vs. the previous scan)
     * and logs them as spawns — without marking the items. Rearranging items keeps totals constant,
     * so only genuine materialisation is logged. The first scan only establishes the baseline.
     */
    private static void logStackableSpawns(ServerPlayer player, Inventory inv) {
        Map<String, Integer> current = new HashMap<>();
        addStackableTotals(current, inv.items);
        addStackableTotals(current, inv.armor);
        addStackableTotals(current, inv.offhand);

        Map<String, Integer> previous = STACKABLE_BASELINE.put(player.getUUID(), current);
        if (previous == null) {
            return; // first observation: baseline only, nothing to log
        }
        ItemTracker tracker = ItemTracker.get();
        for (Map.Entry<String, Integer> entry : current.entrySet()) {
            int delta = entry.getValue() - previous.getOrDefault(entry.getKey(), 0);
            if (delta > 0) {
                tracker.logStackableSpawn(player, entry.getKey(), delta);
            }
        }
    }

    private static void addStackableTotals(Map<String, Integer> totals, Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            // Only stackable, unmarked items: marked gear (max stack 1) is handled by the NBT path.
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1 || NbtUtil.isTracked(stack)) {
                continue;
            }
            totals.merge(ItemTracker.itemId(stack), stack.getCount(), Integer::sum);
        }
    }

    /** Drops a player's spawn baseline (called on logout and when leaving creative). */
    public static void clearStackableBaseline(UUID uuid) {
        STACKABLE_BASELINE.remove(uuid);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STACKABLE_BASELINE.remove(event.getEntity().getUUID());
    }

    private static void scanList(ServerPlayer player, Iterable<ItemStack> stacks) {
        ItemTracker tracker = ItemTracker.get();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (NbtUtil.isTracked(stack)) {
                // Restore stacking if this item shouldn't be marked under the current mode,
                // otherwise just check whether its mark has expired.
                if (tracker.healStacking(stack)) {
                    continue;
                }
                tracker.stripIfExpired(stack);
                continue;
            }
            tracker.markAndLogSpawn(player, stack, "inventory_pick");
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isCreative() || !ItemTracker.isOp(player)) {
            return;
        }

        BlockState placed = event.getPlacedBlock();
        ItemStack hand = player.getMainHandItem();
        String itemId = !hand.isEmpty()
                ? ItemTracker.itemId(hand)
                : ItemTracker.itemId(new ItemStack(placed.getBlock()));

        ItemTracker.get().logBlockPlacement(player, itemId, event.getPos(), player.level());
    }
}
