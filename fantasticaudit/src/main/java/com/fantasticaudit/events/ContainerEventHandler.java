package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.fantasticaudit.util.ItemSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * Captures the CONTAINERS &amp; INVENTORY category for any container of any mod.
 *
 * <p>Forge 1.20.1 exposes no per-slot "item moved" event, so this handler uses the supported
 * {@link PlayerContainerEvent} open/close pair: it snapshots the container-side contents on open
 * and diffs them on close. The result is the <em>net</em> change of each item id during that
 * open→close session, logged as {@code CONTAINER_PUT}/{@code CONTAINER_TAKE}. This is the
 * highest-fidelity container tracking achievable through stable Forge events without mixins, and
 * it works uniformly for vanilla and modded containers because slots are identified structurally
 * (any slot whose backing container is not the player's own inventory).</p>
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainerEventHandler {

    private ContainerEventHandler() {
    }

    /** The container-side contents captured when a menu was opened, plus the container's identity. */
    private record Snapshot(Map<String, Integer> counts, String containerType, String containerPos, String dim) {
    }

    /** The last block a player right-clicked, used to attribute an opened container to a position. */
    private record InteractedBlock(String pos, String blockId, String dim, long atMillis) {
    }

    private static final ConcurrentHashMap<UUID, Snapshot> OPEN_SNAPSHOTS = new ConcurrentHashMap<>();
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

        Map<String, Integer> counts = containerSideCounts(menu, player);

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
            dim = ItemSerializer.dimension(player.level());
        }

        OPEN_SNAPSHOTS.put(player.getUUID(), new Snapshot(counts, containerType, containerPos, dim));
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!AuditConfig.LOG_CONTAINERS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Snapshot snapshot = OPEN_SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null) {
            return;
        }

        Map<String, Integer> before = snapshot.counts();
        Map<String, Integer> after = containerSideCounts(event.getContainer(), player);

        Set<String> allIds = new HashSet<>();
        allIds.addAll(before.keySet());
        allIds.addAll(after.keySet());

        String name = player.getGameProfile().getName();
        UUID uuid = player.getUUID();

        for (String itemId : allIds) {
            int delta = after.getOrDefault(itemId, 0) - before.getOrDefault(itemId, 0);
            if (delta == 0) {
                continue;
            }
            String eventType = delta > 0 ? "CONTAINER_PUT" : "CONTAINER_TAKE";
            int quantity = Math.abs(delta);
            String data = "item_id={" + itemId + "}"
                    + " quantity={" + quantity + "}"
                    + " container_type={" + snapshot.containerType() + "}"
                    + " container_pos={" + snapshot.containerPos() + "}"
                    + " dim={" + snapshot.dim() + "}";
            AuditLogger.get().record(uuid, name, eventType, data);
        }
    }

    /**
     * Sums item counts across every container-side slot (i.e. slots not backed by the player's own
     * inventory). Keyed by full registry id so modded items aggregate correctly.
     */
    private static Map<String, Integer> containerSideCounts(AbstractContainerMenu menu, ServerPlayer player) {
        Map<String, Integer> counts = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            counts.merge(ItemSerializer.itemId(stack), stack.getCount(), Integer::sum);
        }
        return counts;
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
}
