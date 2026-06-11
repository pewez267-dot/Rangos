package com.gbaminecraft.minecraft.gui;

import com.gbaminecraft.minecraft.registry.ModMenuTypes;
import com.gbaminecraft.minecraft.tileentity.GBATileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Container/Menu for the GBA Console.
 * The screen is rendered client-side by GBAScreen.
 * This menu handles the server-side logic and slot management.
 */
public class GBAMenu extends AbstractContainerMenu {

    private final GBATileEntity tileEntity;

    // Client-side constructor (from network packet)
    public GBAMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getTileEntity(playerInv, buf));
    }

    private static GBATileEntity getTileEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof GBATileEntity gba) return gba;
        throw new IllegalStateException("GBATileEntity not found at position from packet!");
    }

    // Server-side constructor
    public GBAMenu(int id, Inventory playerInv, GBATileEntity tileEntity) {
        super(ModMenuTypes.GBA_MENU.get(), id);
        this.tileEntity = tileEntity;

        // Add player inventory slots (for reference, though GBA screen has custom UI)
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9,
                        8 + col * 18, 168 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 226));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return tileEntity.getBlockPos().distToCenterSqr(
                player.getX(), player.getY(), player.getZ()) < 64.0;
    }

    public GBATileEntity getTileEntity() {
        return tileEntity;
    }
}
