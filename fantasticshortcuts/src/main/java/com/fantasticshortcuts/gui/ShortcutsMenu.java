package com.fantasticshortcuts.gui;

import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.network.ShortcutCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu backing the main management screen. It carries no item slots — it is a
 * data anchor that ships the current shortcut list to the client and gives the screen a
 * proper {@code MenuType}/{@code AbstractContainerMenu} lifecycle.
 */
public final class ShortcutsMenu extends AbstractContainerMenu {

    private final List<Shortcut> shortcuts;

    /** Server-side constructor. */
    public ShortcutsMenu(final int containerId, final Inventory playerInventory, final List<Shortcut> shortcuts) {
        super(ModMenus.SHORTCUTS.get(), containerId);
        this.shortcuts = new ArrayList<>(shortcuts);
    }

    /** Client-side constructor (reads the data shipped by {@code NetworkHooks.openScreen}). */
    public ShortcutsMenu(final int containerId, final Inventory playerInventory, final FriendlyByteBuf buf) {
        this(containerId, playerInventory, ShortcutCodec.readList(buf));
    }

    public List<Shortcut> getShortcuts() {
        return this.shortcuts;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }
}
