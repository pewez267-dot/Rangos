package com.fantastickits.gui;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
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
 * Server-side container menu for the Kit Editor.
 * Uses a 6-row (54-slot) container for kit items.
 * The top 4 rows (36 slots) are for kit items.
 * The bottom section contains functional slots and player inventory.
 *
 * Layout (FantasticCrates style):
 * - Rows 0-3 (slots 0-35): Kit item slots
 * - Row 4 (slots 36-44): Action bar (save, group select, commands, preview, etc.)
 * - Below: Player inventory (27 + 9 hotbar)
 */
public class KitEditMenu extends AbstractContainerMenu {

    private static final int KIT_ITEM_SLOTS = 36;
    private static final int ACTION_SLOTS = 9;
    private static final int TOTAL_MENU_SLOTS = KIT_ITEM_SLOTS + ACTION_SLOTS;

    private final String kitName;
    private final SimpleContainer kitContainer;
    private final Inventory playerInventory;

    public KitEditMenu(int containerId, Inventory playerInventory, String kitName) {
        super(KitMenuRegistry.KIT_EDIT_MENU.get(), containerId);
        this.kitName = kitName;
        this.playerInventory = playerInventory;
        this.kitContainer = new SimpleContainer(TOTAL_MENU_SLOTS);

        // Load existing kit items into container
        loadKitItems();

        // Add kit item slots (4 rows of 9)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new Slot(kitContainer, index, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add action bar slots (row 5, non-interactable for items)
        for (int col = 0; col < 9; col++) {
            int index = KIT_ITEM_SLOTS + col;
            this.addSlot(new ActionSlot(kitContainer, index, 8 + col * 18, 104));
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

    private void loadKitItems() {
        KitData kitData = FantasticKits.getInstance().getKitData();
        KitDefinition kit = kitData.getKit(kitName);
        if (kit != null) {
            List<CompoundTag> items = kit.getItemsAsNbt();
            for (int i = 0; i < items.size() && i < KIT_ITEM_SLOTS; i++) {
                ItemStack stack = NBTSerializer.deserializeItemStack(items.get(i));
                if (!stack.isEmpty()) {
                    kitContainer.setItem(i, stack);
                }
            }
        }
    }

    /**
     * Save all items currently in the kit container back to the kit definition.
     */
    public void saveKitItems(Player player) {
        KitData kitData = FantasticKits.getInstance().getKitData();
        KitDefinition kit = kitData.getKit(kitName);
        if (kit == null) return;

        kit.getItemNbtList().clear();
        for (int i = 0; i < KIT_ITEM_SLOTS; i++) {
            ItemStack stack = kitContainer.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag tag = NBTSerializer.serializeItemStack(stack);
                kit.getItemNbtList().add(tag.toString());
            }
        }
        kitData.updateKit(kit);

        // Audit log
        if (player instanceof ServerPlayer serverPlayer) {
            FantasticKits.getInstance().getAuditLog().logKitEdited(
                    serverPlayer.getUUID(), serverPlayer.getName().getString(), kitName);
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Action bar slots (36-44) handle special actions via screen-side logic
        if (slotId >= KIT_ITEM_SLOTS && slotId < TOTAL_MENU_SLOTS) {
            int actionIndex = slotId - KIT_ITEM_SLOTS;
            handleActionClick(actionIndex, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void handleActionClick(int actionIndex, Player player) {
        switch (actionIndex) {
            case 0 -> {
                // Save button
                saveKitItems(player);
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§aKit '" + kitName + "' saved successfully."));
                }
            }
            case 8 -> {
                // Close button
                if (player instanceof ServerPlayer sp) {
                    saveKitItems(player);
                    sp.closeContainer();
                }
            }
            // Actions 1-7 are handled client-side (group select, commands, etc.)
            // They trigger screen tab switches, not server actions
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index < KIT_ITEM_SLOTS) {
                // Move from kit slots to player inventory
                if (!this.moveItemStackTo(stackInSlot, TOTAL_MENU_SLOTS, TOTAL_MENU_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= TOTAL_MENU_SLOTS) {
                // Move from player inventory to kit slots
                if (!this.moveItemStackTo(stackInSlot, 0, KIT_ITEM_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        // Validate admin permission server-side
        if (player instanceof ServerPlayer sp) {
            return SecurityManager.hasAdminPermission(sp);
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Auto-save on close
        saveKitItems(player);
    }

    public String getKitName() {
        return kitName;
    }

    /**
     * Non-interactable slot used for action bar buttons.
     * Items cannot be placed in or taken from these slots.
     */
    private static class ActionSlot extends Slot {
        public ActionSlot(Container container, int index, int x, int y) {
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
