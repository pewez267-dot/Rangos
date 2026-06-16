package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.fantasticaudit.util.ItemSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
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
 * Captures the CONTAINERS &amp; INVENTORY category for any container of any mod, with
 * <b>per-action</b> fidelity.
 *
 * <p>Forge 1.20.1 has no dedicated per-slot move event, but vanilla's own
 * {@link ContainerListener} mechanism (the same hook the server uses to sync slots to the client)
 * is fully usable: we attach a listener to the opened menu via
 * {@link AbstractContainerMenu#addSlotListener(ContainerListener)}. On every slot change we
 * recompute the container-side totals per item id and diff them against the previous totals,
 * emitting a {@code CONTAINER_PUT}/{@code CONTAINER_TAKE} for the exact amount that entered or
 * left the container during that interaction.</p>
 *
 * <p>Diffing <em>container-wide totals</em> (rather than individual slots) is deliberate: it
 * captures each individual click/shift-click/drag as it happens, yet a purely internal slot→slot
 * reshuffle leaves the totals unchanged and therefore produces no spurious event. Slots are
 * identified structurally (any slot not backed by the player's own inventory), so this works
 * uniformly for vanilla and modded containers.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainerEventHandler {

    private ContainerEventHandler() {
    }

    /** The last block a player right-clicked, used to attribute an opened container to a position. */
    private record InteractedBlock(String pos, String blockId, String dim, long atMillis) {
    }

    private static final ConcurrentHashMap<UUID, InteractedBlock> LAST_INTERACTED = new ConcurrentHashMap<>();

    /** A right-click is only used to identify a container opened within this window. */
    private static final long INTERACT_WINDOW_MILLIS = 1500L;

    /**
     * Records the last block a player right-clicked. Called from {@link BlockEventHandler} so the
     * container handler can attribute a freshly opened container to its world position/type.
     */
    public static void rememberInteractedBlock(UUID playerUuid, BlockPos pos, String blockId, String dim) {
        LAST_INTERACTED.put(playerUuid,
                new InteractedBlock(ItemSerializer.pos(pos), blockId, dim, System.currentTimeMillis()));
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!AuditConfig.LOG_CONTAINERS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        // The player's own inventory menu is not a "container" for audit purposes.
        if (menu == player.inventoryMenu) {
            return;
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
            // Entity containers (minecarts, horses) and command-opened menus have no tracked block.
            containerType = menuTypeId(menu);
            containerPos = ItemSerializer.pos(player);
            dim = ItemSerializer.dimShort(player.level());
        }

        // The listener lives for the lifetime of this menu instance and is collected with it when
        // the player closes the container, so no explicit removal is required.
        menu.addSlotListener(new AuditContainerListener(menu, player, containerType, containerPos, dim));
    }

    /** Sums item counts across every container-side slot, keyed by full registry id. */
    private static Map<String, Integer> containerTotals(AbstractContainerMenu menu, ServerPlayer player) {
        Map<String, Integer> totals = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            totals.merge(ItemSerializer.itemId(stack), stack.getCount(), Integer::sum);
        }
        return totals;
    }

    /**
     * @return the menu type's registry id, or {@code unknown} when the menu has no registered type
     * (some modded/programmatic menus throw on {@code getType()}).
     */
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
     * Observes a single opened container and emits put/take events per interaction by diffing the
     * container-wide item totals on each slot change.
     */
    private static final class AuditContainerListener implements ContainerListener {

        private final AbstractContainerMenu menu;
        private final ServerPlayer player;
        private final String containerType;
        private final String containerPos;
        private final String dim;
        private Map<String, Integer> totals;

        private AuditContainerListener(AbstractContainerMenu menu, ServerPlayer player,
                                       String containerType, String containerPos, String dim) {
            this.menu = menu;
            this.player = player;
            this.containerType = containerType;
            this.containerPos = containerPos;
            this.dim = dim;
            this.totals = containerTotals(menu, player);
        }

        @Override
        public void slotChanged(AbstractContainerMenu changedMenu, int slotId, ItemStack newStack) {
            if (!AuditConfig.LOG_CONTAINERS.get()) {
                return;
            }
            // The menu's slots are already in their final post-interaction state when this fires, so a
            // single full recompute captures the complete delta; redundant calls within the same
            // broadcast then diff to zero.
            Map<String, Integer> now = containerTotals(menu, player);
            if (now.equals(totals)) {
                return;
            }

            Set<String> ids = new HashSet<>();
            ids.addAll(totals.keySet());
            ids.addAll(now.keySet());
            for (String itemId : ids) {
                int delta = now.getOrDefault(itemId, 0) - totals.getOrDefault(itemId, 0);
                if (delta == 0) {
                    continue;
                }
                String eventType = delta > 0 ? "CONTAINER_PUT" : "CONTAINER_TAKE";
                String data = itemId + " x" + Math.abs(delta)
                        + " " + containerType
                        + " @(" + containerPos + ") " + dim;
                AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), eventType, data);
            }
            this.totals = now;
        }

        @Override
        public void dataChanged(AbstractContainerMenu changedMenu, int dataSlotIndex, int value) {
            // Data slots carry synchronized integers (furnace burn time, brewing progress, etc.),
            // never inventory contents, so they are irrelevant to item auditing and ignored here.
        }
    }
}
