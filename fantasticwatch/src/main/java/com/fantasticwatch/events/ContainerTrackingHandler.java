package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.tracking.ItemTracker;
import com.fantasticwatch.util.NbtUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * Tracks marked items moving into and out of any container (vanilla or modded).
 *
 * <p>As with Fantastic Audit, Forge 1.20.1 has no per-slot move event, so this snapshots which
 * side (player inventory vs. container) each marked uid sits on when a menu opens, then diffs on
 * close: a uid that moved player→container is {@code ITEM_STORED}; container→player is
 * {@code ITEM_RETRIEVED} (and, if the retriever is not the current owner, an ownership transfer).
 * Container slots are identified structurally so this works for any mod's container.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticWatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainerTrackingHandler {

    private ContainerTrackingHandler() {
    }

    private enum Side {
        PLAYER, CONTAINER
    }

    private record Snapshot(Map<String, Side> sideByUid, String containerType, String containerPos, String dim) {
    }

    private record InteractedBlock(String pos, String blockId, String dim, long atMillis) {
    }

    private static final ConcurrentHashMap<UUID, Snapshot> OPEN_SNAPSHOTS = new ConcurrentHashMap<>();
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

        Map<String, Side> sideByUid = new HashMap<>();
        collectSides(menu, player, sideByUid, null);

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

        OPEN_SNAPSHOTS.put(player.getUUID(), new Snapshot(sideByUid, containerType, containerPos, dim));
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Snapshot snapshot = OPEN_SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null) {
            return;
        }

        Map<String, Side> after = new HashMap<>();
        Map<String, ItemStack> afterStacks = new HashMap<>();
        collectSides(event.getContainer(), player, after, afterStacks);

        Set<String> allUids = new HashSet<>();
        allUids.addAll(snapshot.sideByUid().keySet());
        allUids.addAll(after.keySet());

        boolean logForActor = ItemTracker.isOp(player) || WatchConfig.LOG_NON_OP_INTERACTIONS.get();
        if (!logForActor) {
            return;
        }

        ItemTracker tracker = ItemTracker.get();
        for (String uid : allUids) {
            Side before = snapshot.sideByUid().get(uid);
            Side now = after.get(uid);
            if (before == now) {
                continue;
            }
            ItemStack stack = afterStacks.get(uid);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (tracker.stripIfExpired(stack)) {
                continue;
            }
            if (before == Side.PLAYER && now == Side.CONTAINER) {
                tracker.onStored(stack, player, snapshot.containerType(), snapshot.containerPos(), snapshot.dim());
            } else if (before == Side.CONTAINER && now == Side.PLAYER) {
                tracker.onRetrieved(stack, player, snapshot.containerType(), snapshot.containerPos(), snapshot.dim());
            }
        }
    }

    /**
     * Records, for each marked uid in the menu, which side it currently sits on. When
     * {@code stacksOut} is provided it also captures the stack reference for later logging.
     */
    private static void collectSides(AbstractContainerMenu menu, ServerPlayer player,
                                     Map<String, Side> sideOut, Map<String, ItemStack> stacksOut) {
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !NbtUtil.isTracked(stack)) {
                continue;
            }
            String uid = NbtUtil.getUid(stack);
            if (uid == null) {
                continue;
            }
            Side side = (slot.container == player.getInventory()) ? Side.PLAYER : Side.CONTAINER;
            sideOut.put(uid, side);
            if (stacksOut != null) {
                stacksOut.put(uid, stack);
            }
        }
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
}
