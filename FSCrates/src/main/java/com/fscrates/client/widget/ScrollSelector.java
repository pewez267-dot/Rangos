package com.fscrates.client.widget;

import net.minecraft.client.Minecraft;
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
 * A modern, scrollable + searchable selection list (mouse wheel, click select,
 * draggable thumb). Reused across the crate editor tabs.
 */
public class ScrollSelector<T> extends AbstractWidget {

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();

    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon; // may be null
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
        all.clear();
        all.addAll(items);
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
                if (filterText.apply(t).toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(t);
                }
            }
        }
        scroll = 0;
        selectedIndex = -1;
    }

    public T getSelected() {
        return selectedIndex >= 0 && selectedIndex < filtered.size() ? filtered.get(selectedIndex) : null;
    }

    private int visibleRows() {
        return Math.max(1, height / rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xC0101018);
        g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF3A3A4A);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF3A3A4A);

        var font = Minecraft.getInstance().font;
        int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index < 0 || index >= filtered.size()) {
                break;
            }
            T entry = filtered.get(index);
            int rowY = getY() + i * rowHeight;
            boolean hovered = mouseX >= getX() && mouseX < getX() + width - 6
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
            if (index == selectedIndex) {
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0xFF2D6CDF);
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
            String name = displayName.apply(entry);
            String trimmed = font.plainSubstrByWidth(name, width - (textX - getX()) - 8);
            g.drawString(font, trimmed, textX, rowY + (rowHeight - 8) / 2, 0xE0E0E0, false);
        }

        if (maxScroll() > 0) {
            int barX = getX() + width - 5;
            g.fill(barX, getY(), barX + 4, getY() + height, 0x60000000);
            int trackH = height;
            int thumbH = Math.max(10, trackH * visibleRows() / Math.max(1, filtered.size()));
            int thumbY = getY() + (trackH - thumbH) * scroll / Math.max(1, maxScroll());
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF8080A0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
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
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
