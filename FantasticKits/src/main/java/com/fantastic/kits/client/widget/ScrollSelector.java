package com.fantastic.kits.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scrollable, filterable, single-selection list widget used across every
 * Fantastic Kits screen. The look-and-feel is intentionally identical to the
 * widget that ships in FantasticCrates / FantasticSpawners (rounded dark
 * panel, hover highlight, selected highlight, right-side scrollbar).
 *
 * @param <T> the type of element rendered in the list.
 */
public class ScrollSelector<T> extends AbstractWidget {

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon;
    private Consumer<T> onSelect = t -> {};
    private final int rowHeight;
    private int scroll = 0;
    private int selectedIndex = -1;
    private String query = "";

    public ScrollSelector(int x, int y, int width, int height, int rowHeight,
                          Function<T, String> displayName,
                          Function<T, String> filterText,
                          Function<T, ItemStack> icon) {
        super(x, y, width, height, Component.empty());
        this.rowHeight = rowHeight;
        this.displayName = displayName;
        this.filterText = filterText;
        this.icon = icon;
    }

    public ScrollSelector<T> onSelect(Consumer<T> cb) {
        this.onSelect = cb;
        return this;
    }

    public void setItems(List<T> items) {
        this.all.clear();
        this.all.addAll(items);
        applyFilter();
    }

    public void setQuery(String q) {
        this.query = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        applyFilter();
    }

    private void applyFilter() {
        filtered.clear();
        if (query.isEmpty()) {
            filtered.addAll(all);
        } else {
            for (T t : all) {
                String f = filterText == null ? "" : filterText.apply(t);
                if (f != null && f.toLowerCase(Locale.ROOT).contains(query)) filtered.add(t);
            }
        }
        scroll = 0;
        selectedIndex = -1;
    }

    public T getSelected() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) return null;
        return filtered.get(selectedIndex);
    }

    private int visibleRows() { return Math.max(1, height / rowHeight); }
    private int maxScroll() { return Math.max(0, filtered.size() - visibleRows()); }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Dark translucent panel.
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xC0101018);
        g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF3C3C4A);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF3C3C4A);

        Font font = Minecraft.getInstance().font;
        int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index < 0) break;
            if (index >= filtered.size()) break;
            T entry = filtered.get(index);
            int rowY = getY() + i * rowHeight;
            boolean hovered = mouseX >= getX() && mouseX < getX() + width - 6
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
            if (index == selectedIndex) {
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0xFF2E7DBF);
            } else if (hovered) {
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0x40FFFFFF);
            }

            int textX = getX() + 3;
            if (icon != null) {
                ItemStack stack = icon.apply(entry);
                if (stack != null && !stack.isEmpty()) {
                    g.renderItem(stack, getX() + 1, rowY + (rowHeight - 16) / 2);
                }
                textX = getX() + 20;
            }
            String name = displayName == null ? "" : displayName.apply(entry);
            String trimmed = font.plainSubstrByWidth(name, width - (textX - getX()) - 8);
            g.drawString(font, trimmed, textX, rowY + (rowHeight - 8) / 2, 0xE0E0E0, false);
        }

        // Scrollbar.
        if (maxScroll() > 0) {
            int barX = getX() + width - 5;
            g.fill(barX, getY(), barX + 4, getY() + height, 0x60000000);
            int trackH = height;
            int thumbH = Math.max(10, trackH * visibleRows() / Math.max(1, filtered.size()));
            int thumbY = getY() + (trackH - thumbH) * scroll / Math.max(1, maxScroll());
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF8090A0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) return false;
        int row = (int) ((mouseY - getY()) / rowHeight);
        int index = scroll + row;
        if (index >= 0 && index < filtered.size() && mouseX < getX() + width - 6) {
            selectedIndex = index;
            onSelect.accept(filtered.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}
}
