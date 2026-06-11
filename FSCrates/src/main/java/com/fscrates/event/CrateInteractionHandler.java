package com.fscrates.event;

import com.fscrates.config.CrateConfig;
import com.fscrates.crate.CrateOpeningService;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.item.CrateItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Opening flow: the player holds the KEY in the main hand and right-clicks
 * (in the air or on any block) — exactly like a normal interaction. The crate
 * to open is resolved from the id stored on the key itself, looking up its full
 * definition in the server {@link CrateRegistry}. No off-hand crate required.
 */
public final class CrateInteractionHandler {

    private CrateInteractionHandler() {}

    /** Right-click while looking at a block (most common case). */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        tryOpen(event);
    }

    /** Right-click in the air (no block targeted). */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        tryOpen(event);
    }

    private static void tryOpen(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack key = player.getMainHandItem();
        if (!CrateItems.isKey(key)) {
            return;
        }

        // Cancel the default interaction (don't open chests, place blocks, etc.)
        event.setCanceled(true);
        if (event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
            blockEvent.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            blockEvent.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }

        String crateId = CrateItems.crateId(key);
        CrateConfig crate = CrateRegistry.get(player.serverLevel()).get(crateId);
        if (crate == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00A7cNo se encontr\u00f3 la crate '" + crateId + "'. \u00BFFue eliminada?"));
            return;
        }

        boolean skip = crate.allowSkip && player.isShiftKeyDown();
        CrateOpeningService.open(player, crate, key, skip);
    }
}
