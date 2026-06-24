package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.config.WatchConfig;
import com.fantasticwatch.logging.AliasTracker;
import com.fantasticwatch.logging.WatchLogger;
import com.fantasticwatch.tracking.ItemTracker;
import com.fantasticwatch.util.NbtUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scans a connecting player's entire inventory for tracked items and records where each was found.
 *
 * <p>The scan runs on the server thread inside {@code PlayerLoggedInEvent}; it touches only the
 * just-connected player's inventory (a fixed, tiny number of slots), so it never measurably
 * delays login. Expired marks encountered here are stripped lazily via the tracker.</p>
 */
@Mod.EventBusSubscriber(modid = FantasticWatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LoginScanHandler {

    private static final String[] ARMOR_SLOT_NAMES = {"armor_feet", "armor_legs", "armor_chest", "armor_head"};

    private LoginScanHandler() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Cross-link an operator's old/new log files when their username changed. Limited to
        // operators so non-OP players never get a Watch file created (Watch is OP-only).
        if (ItemTracker.isOp(player)) {
            String name = player.getGameProfile().getName();
            String previousName = AliasTracker.get().recordAndGetPrevious(player.getUUID(), name);
            if (previousName != null && !previousName.equals(name)) {
                WatchLogger.get().record(player.getUUID(), name, "NAME_CHANGE",
                        "previous=" + previousName + " uuid=" + player.getUUID());
                WatchLogger.get().record(player.getUUID(), previousName, "NAME_CHANGE",
                        "renamed_to=" + name + " uuid=" + player.getUUID());
            }
        }

        if (!WatchConfig.SCAN_INVENTORY_ON_LOGIN.get()) {
            return;
        }

        boolean logForActor = ItemTracker.isOp(player) || WatchConfig.LOG_NON_OP_INTERACTIONS.get();
        ItemTracker tracker = ItemTracker.get();
        Inventory inv = player.getInventory();

        // Main inventory + hotbar (slots 0-8 are the hotbar).
        for (int i = 0; i < inv.items.size(); i++) {
            String slot = i < 9 ? "hotbar_" + i : "inventory_" + i;
            handleSlot(tracker, player, inv.items.get(i), slot, logForActor);
        }
        // Armor (feet, legs, chest, head).
        for (int i = 0; i < inv.armor.size(); i++) {
            String slot = i < ARMOR_SLOT_NAMES.length ? ARMOR_SLOT_NAMES[i] : "armor_" + i;
            handleSlot(tracker, player, inv.armor.get(i), slot, logForActor);
        }
        // Off-hand.
        for (int i = 0; i < inv.offhand.size(); i++) {
            handleSlot(tracker, player, inv.offhand.get(i), "offhand", logForActor);
        }
    }

    private static void handleSlot(ItemTracker tracker, ServerPlayer player, ItemStack stack,
                                   String slot, boolean logForActor) {
        if (stack.isEmpty() || !NbtUtil.isTracked(stack)) {
            return;
        }
        // Restore stacking for any item that shouldn't be marked under the current mode (runs for
        // every player, not just operators, so legacy unstackable marks heal on next login).
        if (tracker.healStacking(stack)) {
            return;
        }
        if (tracker.stripIfExpired(stack)) {
            return;
        }
        if (logForActor) {
            tracker.onFoundOnLogin(stack, player, slot);
        }
    }
}
