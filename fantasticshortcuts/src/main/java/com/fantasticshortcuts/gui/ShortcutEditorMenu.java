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
 * Container menu backing the create/edit screen. Carries the shortcut being edited (a
 * fresh instance for "create") plus the other existing shortcuts so the screen can show
 * live conflict warnings (e.g. which shortcut already uses an alias). No item slots.
 */
public final class ShortcutEditorMenu extends AbstractContainerMenu {

    private final Shortcut shortcut;
    private final List<Shortcut> others;

    /** Server-side constructor. */
    public ShortcutEditorMenu(final int containerId, final Inventory playerInventory,
                              final Shortcut shortcut, final List<Shortcut> others) {
        super(ModMenus.EDITOR.get(), containerId);
        this.shortcut = shortcut.copy();
        this.others = new ArrayList<>(others);
    }

    /** Client-side constructor. */
    public ShortcutEditorMenu(final int containerId, final Inventory playerInventory, final FriendlyByteBuf buf) {
        this(containerId, playerInventory, ShortcutCodec.read(buf), ShortcutCodec.readList(buf));
    }

    public Shortcut getShortcut() {
        return this.shortcut;
    }

    public List<Shortcut> getOthers() {
        return this.others;
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
