package com.fantastickits.gui;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
import com.fantastickits.data.PlayerData;
import com.fantastickits.security.SecurityManager;
import com.fantastickits.util.NBTSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server-side container for claiming a kit.
 * Shows the kit's items as a preview (non-interactable) and a "Claim" button.
 *
 * Layout:
 * - Rows 0-3 (slots 0-35): Kit item preview (read-only)
 * - Row 4 (slots 36-44): Action bar with claim button at center
 */
public class KitClaimMenu extends AbstractContainerMenu {

    private static final int PREVIEW_SLOTS = 36;
    private static final int ACTION_SLOTS = 9;
    private static final int TOTAL_SLOTS = PREVIEW_SLOTS + ACTION_SLOTS;
    private static final int CLAIM_BUTTON_INDEX = 4; // Center of action bar

    private final String kitName;
    private final SimpleContainer container;

    public KitClaimMenu(int containerId, Inventory playerInventory, String kitName) {
        super(KitMenuRegistry.KIT_CLAIM_MENU.get(), containerId);
        this.kitName = kitName;
        this.container = new SimpleContainer(TOTAL_SLOTS);

        // Load kit items as preview
        loadPreviewItems();

        // Preview slots (read-only)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new PreviewSlot(container, index, 8 + col * 18, 18 + row * 18));
            }
        }

        // Action bar (claim button in center)
        for (int col = 0; col < 9; col++) {
            int index = PREVIEW_SLOTS + col;
            this.addSlot(new PreviewSlot(container, index, 8 + col * 18, 104));
        }

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 134 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 192));
        }
    }

    private void loadPreviewItems() {
        KitData kitData = FantasticKits.getInstance().getKitData();
        KitDefinition kit = kitData.getKit(kitName);
        if (kit != null) {
            List<CompoundTag> items = kit.getItemsAsNbt();
            for (int i = 0; i < items.size() && i < PREVIEW_SLOTS; i++) {
                ItemStack stack = NBTSerializer.deserializeItemStack(items.get(i));
                if (!stack.isEmpty()) {
                    container.setItem(i, stack);
                }
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Only handle claim button click
        if (slotId == PREVIEW_SLOTS + CLAIM_BUTTON_INDEX) {
            handleClaim(player);
            return;
        }
        // Prevent interaction with preview slots
        if (slotId >= 0 && slotId < TOTAL_SLOTS) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private synchronized void handleClaim(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Full server-side validation
        SecurityManager.ClaimResult result = SecurityManager.validateClaim(serverPlayer, kitName);

        if (!result.isAllowed()) {
            serverPlayer.sendSystemMessage(Component.literal("§c" + result.getReason()));
            FantasticKits.getInstance().getAuditLog().logKitClaimDenied(
                    serverPlayer.getUUID(), serverPlayer.getName().getString(), kitName, result.getReason());
            return;
        }

        // Claim is valid - deliver items atomically
        KitData kitData = FantasticKits.getInstance().getKitData();
        KitDefinition kit = kitData.getKit(kitName);
        if (kit == null) return;

        PlayerData playerData = FantasticKits.getInstance().getPlayerData();

        // Double-check claim status (race condition protection with synchronized)
        if (playerData.hasClaimed(serverPlayer.getUUID(), kitName)) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou have already claimed this kit."));
            return;
        }

        // Mark as claimed FIRST (prevent race conditions)
        playerData.markClaimed(serverPlayer.getUUID(), kitName);

        // Deliver items
        List<CompoundTag> items = kit.getItemsAsNbt();
        for (CompoundTag tag : items) {
            ItemStack stack = NBTSerializer.deserializeItemStack(tag);
            if (!stack.isEmpty()) {
                if (!serverPlayer.getInventory().add(stack)) {
                    serverPlayer.drop(stack, false);
                }
            }
        }

        // Audit log
        FantasticKits.getInstance().getAuditLog().logKitClaimed(
                serverPlayer.getUUID(), serverPlayer.getName().getString(), kitName);

        serverPlayer.sendSystemMessage(Component.literal("§aYou have successfully claimed the kit '" + kitName + "'!"));
        serverPlayer.closeContainer();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No shift-clicking allowed in this menu
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public String getKitName() {
        return kitName;
    }

    /**
     * Read-only slot that prevents all interaction.
     */
    private static class PreviewSlot extends Slot {
        public PreviewSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
