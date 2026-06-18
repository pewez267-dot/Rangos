package com.fantasticchest.gui.terminal;

import com.fantasticchest.config.ChestConfig;
import com.fantasticchest.gui.QuantityFormat;
import com.fantasticchest.network.OpenTerminalPacket;
import com.fantasticchest.network.PacketHandler;
import com.fantasticchest.network.TerminalEntry;
import com.fantasticchest.network.TerminalExtractPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * User terminal (Interface 2). Clean, focused list of stock with client-side search over
 * received data and click-to-extract. Pages are streamed on demand; the screen never holds
 * the whole inventory unless the user scrolls through it.
 */
public final class ChestTerminalScreen extends AbstractContainerScreen<ChestTerminalMenu> {

    private final net.minecraft.core.BlockPos chestPos;
    private final String chestName;
    private final List<TerminalEntry> loaded = new ArrayList<>();
    private final int pageSize;
    private int total;

    private EditBox searchBox;
    private EditBox amountBox;
    private String search = "";
    private String selectedItemId = "";
    private int scrollRow = 0;
    private boolean awaitingPage = false;

    private int listX;
    private int listY;
    private int listW;
    private int rowH = 18;
    private int visibleRows;

    public ChestTerminalScreen(final ChestTerminalMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.chestPos = menu.getChestPos();
        this.chestName = menu.getChestName();
        this.total = menu.getTotalEntries();
        this.loaded.addAll(menu.getFirstPage());
        this.pageSize = Math.max(1, menu.getFirstPage().isEmpty() ? ChestConfig.pageSize() : menu.getFirstPage().size());
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(this.width - 16, 540);
        this.imageHeight = Math.min(this.height - 16, 320);
        super.init();

        final int x = this.leftPos + 8;
        final int w = this.imageWidth - 16;

        this.searchBox = new EditBox(this.font, x, this.topPos + 22, w, 16, Component.empty());
        this.searchBox.setHint(Component.literal("Buscar item..."));
        this.searchBox.setValue(this.search);
        this.searchBox.setResponder(v -> {
            this.search = v;
            this.scrollRow = 0;
        });
        addRenderableWidget(this.searchBox);

        this.listX = x;
        this.listY = this.topPos + 42;
        this.listW = w;
        final int listBottom = this.topPos + this.imageHeight - 28;
        this.visibleRows = Math.max(1, (listBottom - this.listY) / this.rowH);

        this.amountBox = new EditBox(this.font, x, this.topPos + this.imageHeight - 24, 70, 16, Component.empty());
        this.amountBox.setHint(Component.literal("cant."));
        addRenderableWidget(this.amountBox);

        addRenderableWidget(Button.builder(Component.literal("§aExtraer"), b -> extractCustom())
                .bounds(x + 74, this.topPos + this.imageHeight - 24, 70, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(x + w - 70, this.topPos + this.imageHeight - 24, 70, 18).build());
    }

    private List<TerminalEntry> filtered() {
        if (this.search.isBlank()) {
            return this.loaded;
        }
        final String q = this.search.toLowerCase(java.util.Locale.ROOT);
        final List<TerminalEntry> out = new ArrayList<>();
        for (final TerminalEntry e : this.loaded) {
            final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.itemId()));
            final String name = item == null ? e.itemId() : new ItemStack(item).getHoverName().getString();
            if (name.toLowerCase(java.util.Locale.ROOT).contains(q) || e.itemId().toLowerCase(java.util.Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -12961206);
        final String name = this.chestName == null || this.chestName.isBlank() ? "Fantastic Chest" : this.chestName;
        g.drawString(this.font, name, this.leftPos + 8, this.topPos + 6, 16777215, false);

        final List<TerminalEntry> view = filtered();
        for (int i = 0; i < this.visibleRows; i++) {
            final int index = this.scrollRow + i;
            if (index < 0 || index >= view.size()) {
                break;
            }
            final TerminalEntry entry = view.get(index);
            final int rowY = this.listY + i * this.rowH;
            final boolean hovered = mouseX >= this.listX && mouseX < this.listX + this.listW
                    && mouseY >= rowY && mouseY < rowY + this.rowH;
            if (hovered) {
                g.fill(this.listX, rowY, this.listX + this.listW, rowY + this.rowH, 1090519039);
            }
            final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(entry.itemId()));
            if (item != null) {
                g.renderItem(new ItemStack(item), this.listX + 2, rowY + 1);
            }
            final String itemName = item == null ? entry.itemId() : new ItemStack(item).getHoverName().getString();
            final boolean depleted = entry.quantity() <= 0L;
            final String nameColor = depleted ? "§8" : "§f";
            g.drawString(this.font, this.font.plainSubstrByWidth(nameColor + itemName, this.listW - 150), this.listX + 22, rowY + 5, 16777215, false);
            final String qty = depleted ? "§8Agotado" : "§a" + QuantityFormat.format(entry.quantity());
            g.drawString(this.font, qty, this.listX + this.listW - 110, rowY + 5, 16777215, false);
        }

        final int shown = Math.min(view.size(), this.scrollRow + this.visibleRows);
        g.drawString(this.font, "§7" + shown + "/" + (this.search.isBlank() ? this.total : view.size())
                + " §8(izq: 1 · shift: 64 · der: elegir item)", this.leftPos + 150, this.topPos + 6, 10133680, false);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Suppress default labels.
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listW && mouseY >= this.listY && mouseY < this.listY + this.visibleRows * this.rowH) {
            final int row = (int) ((mouseY - this.listY) / this.rowH);
            final List<TerminalEntry> view = filtered();
            final int index = this.scrollRow + row;
            if (index >= 0 && index < view.size()) {
                final TerminalEntry entry = view.get(index);
                if (button == 1) {
                    this.selectedItemId = entry.itemId();
                } else if (button == 0 && entry.quantity() > 0L) {
                    final long amount = hasShiftDown() ? 64L : 1L;
                    PacketHandler.sendToServer(new TerminalExtractPacket(this.chestPos, entry.itemId(), amount));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void extractCustom() {
        if (this.selectedItemId.isBlank()) {
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(this.amountBox.getValue().trim());
        } catch (final NumberFormatException e) {
            amount = 0L;
        }
        if (amount > 0L) {
            PacketHandler.sendToServer(new TerminalExtractPacket(this.chestPos, this.selectedItemId, amount));
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta) {
        final List<TerminalEntry> view = filtered();
        final int maxScroll = Math.max(0, view.size() - this.visibleRows);
        this.scrollRow = Math.max(0, Math.min(maxScroll, this.scrollRow - (int) Math.signum(delta)));
        maybeRequestMore();
        return true;
    }

    private void maybeRequestMore() {
        if (!this.search.isBlank() || this.awaitingPage) {
            return;
        }
        if (this.loaded.size() < this.total && this.scrollRow + this.visibleRows >= this.loaded.size()) {
            final int nextPage = this.loaded.size() / this.pageSize;
            this.awaitingPage = true;
            PacketHandler.sendToServer(new OpenTerminalPacket(this.chestPos, nextPage));
        }
    }

    // ---- packet-driven updates (client) ----

    private void onPage(final int pageIndex, final int newTotal, final List<TerminalEntry> entries) {
        this.total = newTotal;
        this.awaitingPage = false;
        if (pageIndex == this.loaded.size() / this.pageSize) {
            this.loaded.addAll(entries);
        }
    }

    private void onUpdate(final String itemId, final long newQty, final int newTotal) {
        this.total = newTotal;
        for (int i = 0; i < this.loaded.size(); i++) {
            if (this.loaded.get(i).itemId().equals(itemId)) {
                if (newQty <= 0L && ChestConfig.hideEmptyItems()) {
                    this.loaded.remove(i);
                } else {
                    this.loaded.set(i, new TerminalEntry(itemId, newQty));
                }
                return;
            }
        }
    }

    public static void acceptPage(final int pageIndex, final int total, final List<TerminalEntry> entries) {
        if (Minecraft.getInstance().screen instanceof ChestTerminalScreen screen) {
            screen.onPage(pageIndex, total, entries);
        }
    }

    public static void acceptUpdate(final String itemId, final long newQty, final int total) {
        if (Minecraft.getInstance().screen instanceof ChestTerminalScreen screen) {
            screen.onUpdate(itemId, newQty, total);
        }
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        if (this.searchBox.isFocused() && this.searchBox.canConsumeInput()) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (this.amountBox.isFocused() && this.amountBox.canConsumeInput()) {
            this.amountBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
