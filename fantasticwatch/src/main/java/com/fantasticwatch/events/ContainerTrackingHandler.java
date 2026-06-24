package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.tracking.ItemTracker;
import com.fantasticwatch.util.NbtUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks marked items moving into and out of any container (vanilla or modded) with
 * <b>per-action</b> fidelity.
 *
 * <p>A vanilla {@link ContainerListener} is attached to each opened menu. On every slot change we
 * recompute the container-side count of each tracked {@code item_uid} and diff it against the
 * previous totals: an increase is an {@code ITEM_STORED}, a decrease is an {@code ITEM_RETRIEVED}
 * (and, when the retriever is not the current owner, an ownership transfer). Diffing container-wide
 * totals means internal slot→slot reshuffles within the same container produce no false events,
 * while every genuine put/take is captured as it happens.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticWatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainerTrackingHandler {

    private ContainerTrackingHandler() {
    }

    private record InteractedBlock(String pos, String blockId, String dim, long atMillis) {
    }

    private static final ConcurrentHashMap<UUID, InteractedBlock> LAST_INTERACTED = new ConcurrentHashMap<>();
    private static final long INTERACT_WINDOW_MILLIS = 1500L;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String blockId = key != null ? key.toString() : "unknown";
        LAST_INTERACTED.put(player.getUUID(),
                new InteractedBlock(ItemTracker.pos(pos), blockId, ItemTracker.dim(player.level()),
                        System.currentTimeMillis()));
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        if (menu == player.inventoryMenu) {
            return;
        }

        // Restore stacking for any tracked item in this container (or the player's inventory) that
        // shouldn't be marked under the current mode. This heals legacy marks in chests on open.
        ItemTracker healer = ItemTracker.get();
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && NbtUtil.isTracked(stack)) {
                healer.healStacking(stack);
            }
        }

        String containerType;
        String containerPos;
        String dim;
        InteractedBlock recent = LAST_INTERACTED.get(player.getUUID());
        if (recent != null && (System.currentTimeMillis() - recent.atMillis()) <= INTERACT_WINDOW_MILLIS) {
            containerType = recent.blockId();
            containerPos = recent.pos();
            dim = recent.dim();
        } else {
            containerType = menuTypeId(menu);
            containerPos = ItemTracker.pos(player);
            dim = ItemTracker.dim(player.level());
        }

        menu.addSlotListener(new WatchContainerListener(menu, player, containerType, containerPos, dim));
    }

    /** Counts each tracked uid across the container-side slots only. */
    private static Map<String, Integer> containerTrackedTotals(AbstractContainerMenu menu, ServerPlayer player) {
        Map<String, Integer> totals = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !NbtUtil.isTracked(stack)) {
                continue;
            }
            String uid = NbtUtil.getUid(stack);
            if (uid != null) {
                totals.merge(uid, stack.getCount(), Integer::sum);
            }
        }
        return totals;
    }

    /** @return a container-side stack carrying the given uid, or {@link ItemStack#EMPTY}. */
    private static ItemStack findContainerStackByUid(AbstractContainerMenu menu, ServerPlayer player, String uid) {
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (matchesUid(stack, uid)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Finds the live stack carrying the uid after it left the container: the cursor, the player's
     * inventory, or any remaining menu slot. Returning the live stack (not a copy) lets ownership
     * updates persist on the real item NBT.
     */
    private static ItemStack findLiveByUid(AbstractContainerMenu menu, ServerPlayer player, String uid) {
        ItemStack carried = menu.getCarried();
        if (matchesUid(carried, uid)) {
            return carried;
        }
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) {
            if (matchesUid(stack, uid)) {
                return stack;
            }
        }
        for (ItemStack stack : inv.offhand) {
            if (matchesUid(stack, uid)) {
                return stack;
            }
        }
        for (ItemStack stack : inv.armor) {
            if (matchesUid(stack, uid)) {
                return stack;
            }
        }
        for (Slot slot : menu.slots) {
            if (matchesUid(slot.getItem(), uid)) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesUid(ItemStack stack, String uid) {
        return NbtUtil.isTracked(stack) && uid.equals(NbtUtil.getUid(stack));
    }

    private static String menuTypeId(AbstractContainerMenu menu) {
        try {
            MenuType<?> type = menu.getType();
            if (type == null) {
                return "unknown";
            }
            return String.valueOf(ForgeRegistries.MENU_TYPES.getKey(type));
        } catch (UnsupportedOperationException e) {
            return "unknown";
        }
    }

    /**
     * Per-menu listener that emits store/retrieve events for tracked uids by diffing container
     * totals on each slot change.
     */
    private static final class WatchContainerListener implements ContainerListener {

        private final AbstractContainerMenu menu;
        private final ServerPlayer player;
        private final String containerType;
        private final String containerPos;
        private final String dim;
        private Map<String, Integer> totals;

        private WatchContainerListener(AbstractContainerMenu menu, ServerPlayer player,
                                       String containerType, String containerPos, String dim) {
            this.menu = menu;
            this.player = player;
            this.containerType = containerType;
            this.containerPos = containerPos;
            this.dim = dim;
            this.totals = containerTrackedTotals(menu, player);
        }

        @Override
        public void slotChanged(AbstractContainerMenu changedMenu, int slotId, ItemStack newStack) {
            Map<String, Integer> now = containerTrackedTotals(menu, player);
            if (now.equals(totals)) {
                return;
            }

            boolean logForActor = ItemTracker.isOp(player) || WatchConfig.LOG_NON_OP_INTERACTIONS.get();
            ItemTracker tracker = ItemTracker.get();

            Set<String> uids = new HashSet<>();
            uids.addAll(totals.keySet());
            uids.addAll(now.keySet());
            for (String uid : uids) {
                int delta = now.getOrDefault(uid, 0) - totals.getOrDefault(uid, 0);
                if (delta == 0) {
                    continue;
                }
                if (delta > 0) {
                    // Items of this uid entered the container.
                    ItemStack ref = findContainerStackByUid(menu, player, uid);
                    if (ref.isEmpty() || tracker.stripIfExpired(ref)) {
                        continue;
                    }
                    if (logForActor) {
                        tracker.onStored(ref, delta, player, containerType, containerPos, dim);
                    }
                } else {
                    // Items of this uid left the container; resolve the live stack for ownership update.
                    ItemStack live = findLiveByUid(menu, player, uid);
                    if (!live.isEmpty() && tracker.stripIfExpired(live)) {
                        continue;
                    }
                    if (logForActor && !live.isEmpty()) {
                        tracker.onRetrieved(live, -delta, player, containerType, containerPos, dim);
                    }
                }
            }
            this.totals = now;
        }

        @Override
        public void dataChanged(AbstractContainerMenu changedMenu, int dataSlotIndex, int value) {
            // Data slots are synchronized integers (progress bars, fuel, etc.), not inventory
            // contents, so they are irrelevant to item tracking and ignored here.
        }
    }
}
