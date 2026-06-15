package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.tracking.ItemTracker;
import com.fantasticwatch.util.NbtUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
            return;
        }

        Inventory inv = player.getInventory();
        scanList(player, inv.items);
        scanList(player, inv.armor);
        scanList(player, inv.offhand);
    }

    private static void scanList(ServerPlayer player, Iterable<ItemStack> stacks) {
        ItemTracker tracker = ItemTracker.get();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (NbtUtil.isTracked(stack)) {
                // Already tracked: only check whether its mark has expired and should be stripped.
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
