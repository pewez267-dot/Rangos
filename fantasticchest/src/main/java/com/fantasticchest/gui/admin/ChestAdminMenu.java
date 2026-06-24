package com.fantasticchest.gui.admin;

import com.fantasticchest.gui.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu backing the admin GUI (Interface 1), for both creation and edit. No item
 * slots — it carries the mode, the edited chest's position (edit only), its current id /
 * name / permitted list and the set of existing ids (for client-side uniqueness hints).
 */
public final class ChestAdminMenu extends AbstractContainerMenu {

    private final boolean editMode;
    private final BlockPos chestPos;
    private final String chestId;
    private final String chestName;
    private final List<String> permitted;
    private final List<String> existingIds;

    /** Server-side constructor. */
    public ChestAdminMenu(final int containerId, final Inventory inv, final boolean editMode, final BlockPos pos,
                          final String chestId, final String chestName,
                          final List<String> permitted, final List<String> existingIds) {
        super(ModMenus.ADMIN_MENU.get(), containerId);
        this.editMode = editMode;
        this.chestPos = pos;
        this.chestId = chestId == null ? "" : chestId;
        this.chestName = chestName == null ? "" : chestName;
        this.permitted = new ArrayList<>(permitted == null ? List.of() : permitted);
        this.existingIds = new ArrayList<>(existingIds == null ? List.of() : existingIds);
    }

    /** Client-side constructor. */
    public ChestAdminMenu(final int containerId, final Inventory inv, final FriendlyByteBuf buf) {
        super(ModMenus.ADMIN_MENU.get(), containerId);
        this.editMode = buf.readBoolean();
        this.chestPos = buf.readBoolean() ? buf.readBlockPos() : null;
        this.chestId = buf.readUtf();
        this.chestName = buf.readUtf();
        this.permitted = readList(buf);
        this.existingIds = readList(buf);
    }

    public static void writeOpen(final FriendlyByteBuf buf, final boolean editMode, final BlockPos pos,
                                 final String chestId, final String chestName,
                                 final List<String> permitted, final List<String> existingIds) {
        buf.writeBoolean(editMode);
        buf.writeBoolean(pos != null);
        if (pos != null) {
            buf.writeBlockPos(pos);
        }
        buf.writeUtf(chestId == null ? "" : chestId);
        buf.writeUtf(chestName == null ? "" : chestName);
        writeList(buf, permitted);
        writeList(buf, existingIds);
    }

    private static void writeList(final FriendlyByteBuf buf, final List<String> list) {
        final List<String> safe = list == null ? List.of() : list;
        buf.writeVarInt(safe.size());
        for (final String s : safe) {
            buf.writeUtf(s == null ? "" : s);
        }
    }

    private static List<String> readList(final FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final List<String> out = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            out.add(buf.readUtf());
        }
        return out;
    }

    public boolean isEditMode() {
        return this.editMode;
    }

    public BlockPos getChestPos() {
        return this.chestPos;
    }

    public String getChestId() {
        return this.chestId;
    }

    public String getChestName() {
        return this.chestName;
    }

    public List<String> getPermitted() {
        return this.permitted;
    }

    public List<String> getExistingIds() {
        return this.existingIds;
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
