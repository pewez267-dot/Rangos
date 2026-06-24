package com.fantasticchest.gui.terminal;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.config.ChestConfig;
import com.fantasticchest.gui.ModMenus;
import com.fantasticchest.network.TerminalEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Container menu backing the user terminal (Interface 2). No item slots — it carries the
 * chest position, name, total distinct-item count and the first page of entries. Further
 * pages are streamed on demand via packets; the client never receives the whole inventory
 * at once.
 */
public final class ChestTerminalMenu extends AbstractContainerMenu {

    private final BlockPos chestPos;
    private final String chestName;
    private final int totalEntries;
    private final List<TerminalEntry> firstPage;

    /** Server-side constructor. */
    public ChestTerminalMenu(final int containerId, final Inventory inv, final BlockPos pos,
                             final String name, final int totalEntries, final List<TerminalEntry> firstPage) {
        super(ModMenus.TERMINAL_MENU.get(), containerId);
        this.chestPos = pos;
        this.chestName = name;
        this.totalEntries = totalEntries;
        this.firstPage = new ArrayList<>(firstPage);
    }

    /** Client-side constructor. */
    public ChestTerminalMenu(final int containerId, final Inventory inv, final FriendlyByteBuf buf) {
        super(ModMenus.TERMINAL_MENU.get(), containerId);
        this.chestPos = buf.readBlockPos();
        this.chestName = buf.readUtf();
        this.totalEntries = buf.readVarInt();
        final int count = buf.readVarInt();
        this.firstPage = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.firstPage.add(TerminalEntry.read(buf));
        }
    }

    public static void writeOpen(final FriendlyByteBuf buf, final BlockPos pos, final String name,
                                 final int totalEntries, final List<TerminalEntry> firstPage) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name == null ? "" : name);
        buf.writeVarInt(totalEntries);
        buf.writeVarInt(firstPage.size());
        for (final TerminalEntry entry : firstPage) {
            entry.write(buf);
        }
    }

    public BlockPos getChestPos() {
        return this.chestPos;
    }

    public String getChestName() {
        return this.chestName;
    }

    public int getTotalEntries() {
        return this.totalEntries;
    }

    public List<TerminalEntry> getFirstPage() {
        return this.firstPage;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(final Player player, final int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    // ---- server-side page building (event-driven; never per tick) ----

    /**
     * Builds the full, deterministically-ordered entry list (sorted by registry id). When
     * {@code hide_empty_items} is false, depleted items still present in the original stock
     * appear with quantity 0.
     */
    public static List<TerminalEntry> buildFullList(final ChestBlockEntity chest) {
        final Map<Item, Long> inv = chest.inventory().snapshot();
        final TreeSet<String> ids = new TreeSet<>();
        for (final Item item : inv.keySet()) {
            final ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl != null) {
                ids.add(rl.toString());
            }
        }
        if (!ChestConfig.hideEmptyItems()) {
            for (final Item item : chest.originalStock().snapshot().keySet()) {
                final ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                if (rl != null) {
                    ids.add(rl.toString());
                }
            }
        }
        final List<TerminalEntry> out = new ArrayList<>(ids.size());
        for (final String id : ids) {
            final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
            final long qty = item == null ? 0L : chest.inventory().get(item);
            out.add(new TerminalEntry(id, qty));
        }
        return out;
    }

    /** Slices {@code page_size} entries for the given page index. */
    public static List<TerminalEntry> page(final List<TerminalEntry> full, final int pageIndex) {
        final int size = ChestConfig.pageSize();
        final int from = Math.max(0, pageIndex) * size;
        if (from >= full.size()) {
            return new ArrayList<>();
        }
        final int to = Math.min(full.size(), from + size);
        return new ArrayList<>(full.subList(from, to));
    }
}
