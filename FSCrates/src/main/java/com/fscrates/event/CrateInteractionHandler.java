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
 * Listens for the player using a KEY (right click) while holding/looking to
 * open the matching crate. The crate definition is resolved from the held
 * crate item, the off-hand crate, or the server registry by id.
 *
 * <p>Flow: player must hold the KEY in main hand and a CRATE in off hand (or
 * have the crate in inventory). This keeps interaction simple and reliable
 * across all blocks/entities.
 */
public final class CrateInteractionHandler {

    private CrateInteractionHandler() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        if (!CrateItems.isKey(main)) {
            return;
        }

        // Find a matching crate: off-hand first, then inventory.
        String keyCrateId = CrateItems.crateId(main);
        ItemStack crateStack = findMatchingCrate(player, keyCrateId);
        if (crateStack == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00A7eSostén la \u00A7fcrate\u00A7e correspondiente en la \u00A7fmano secundaria\u00A7e para abrirla con esta llave."));
            return;
        }

        CrateConfig crate = CrateItems.readConfig(crateStack);
        if (crate == null) {
            crate = CrateRegistry.get(player.serverLevel()).get(keyCrateId);
        }
        if (crate == null) {
            player.sendSystemMessage(Component.literal("\u00A7cNo se encontró la definición de esta crate."));
            return;
        }

        boolean skip = crate.allowSkip && player.isShiftKeyDown();
        CrateOpeningService.open(player, crate, main, skip);
        event.setCanceled(true);
    }

    private static ItemStack findMatchingCrate(ServerPlayer player, String crateId) {
        ItemStack off = player.getOffhandItem();
        if (CrateItems.isCrate(off) && CrateItems.crateId(off).equalsIgnoreCase(crateId)) {
            return off;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (CrateItems.isCrate(stack) && CrateItems.crateId(stack).equalsIgnoreCase(crateId)) {
                return stack;
            }
        }
        return null;
    }
}
