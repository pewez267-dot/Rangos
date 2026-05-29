package com.revivemod.gui;

import com.revivemod.ReviveMod;
import com.revivemod.state.DownManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A small on-screen menu (single chest row) shown to a downed player with two
 * clickable item-buttons: surrender and self-revive. Implemented as a normal
 * container screen so vanilla clients see it without any client mod.
 */
public final class OptionsScreenHandler extends ScreenHandler {

    private static final int SURRENDER_SLOT = 2;
    private static final int SELF_SLOT = 6;

    private final SimpleInventory menu;

    public OptionsScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ScreenHandlerType.GENERIC_9X1, syncId);
        this.menu = new SimpleInventory(9);
        buildButtons();

        // Top row: the 9 menu slots (buttons). Non-interactive.
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            this.addSlot(new Slot(menu, idx, 8 + idx * 18, 20) {
                @Override public boolean canInsert(ItemStack stack) { return false; }
                @Override public boolean canTakeItems(PlayerEntity player) { return false; }
            });
        }
        // Player inventory + hotbar so slot indices match the 9x1 client layout.
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 51 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInv, c, 8 + c * 18, 109));
        }
    }

    private void buildButtons() {
        int cost = ReviveMod.getConfig().selfReviveLevelCost;

        ItemStack surrender = new ItemStack(Items.RED_BED);
        surrender.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Rendirse").formatted(Formatting.RED, Formatting.BOLD));
        menu.setStack(SURRENDER_SLOT, surrender);

        if (ReviveMod.getConfig().allowSelfRevive) {
            ItemStack self = new ItemStack(Items.EXPERIENCE_BOTTLE);
            self.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Auto-revivir (" + cost + " niveles)").formatted(Formatting.GREEN, Formatting.BOLD));
            menu.setStack(SELF_SLOT, self);
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;

        // Only treat plain left-click as the "press the button" gesture; ignore
        // Q-drop, shift-click, hotbar-swap, etc. Surrender is destructive so we
        // want to be picky.
        if (actionType != SlotActionType.PICKUP || button != 0) return;

        if (slotIndex == SURRENDER_SLOT) {
            sp.closeHandledScreen();
            if (DownManager.isDown(sp)) {
                sp.sendMessage(Text.literal("Te has rendido.").formatted(Formatting.DARK_RED), false);
                DownManager.forceDeath(sp, sp.getDamageSources().genericKill());
            }
            return;
        }
        if (slotIndex == SELF_SLOT && ReviveMod.getConfig().allowSelfRevive) {
            sp.closeHandledScreen();
            if (!DownManager.isDown(sp)) return;
            int cost = ReviveMod.getConfig().selfReviveLevelCost;
            if (sp.experienceLevel < cost) {
                sp.sendMessage(Text.literal("Necesitas " + cost + " niveles (tienes " + sp.experienceLevel + ").")
                        .formatted(Formatting.RED), false);
                return;
            }
            if (DownManager.selfRevive(sp)) {
                sp.sendMessage(Text.literal("Te has revivido por " + cost + " niveles.").formatted(Formatting.GREEN), false);
            }
            return;
        }
        // Ignore every other click so nothing in the inventory can be moved.
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player instanceof ServerPlayerEntity sp && DownManager.isDown(sp);
    }
}
