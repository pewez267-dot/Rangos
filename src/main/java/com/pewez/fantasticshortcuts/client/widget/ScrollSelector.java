package com.pewez.fantasticshortcuts.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scrollable list selector widget with optional text filter.
 *
 * Used for the shortcuts list in the editor screen. Renders a translucent panel with rows, hover
 * highlight, selected highlight, and a scroll bar.
 */
@OnlyIn(Dist.CLIENT)
public class ScrollSelector<T> extends AbstractWidget {

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private Consumer<T> onSelect = t -> {
    };
    private final int rowHeight;
    private int scroll = 0;
    private int selectedIndex = -1;
    private String query = "";

    public ScrollSelector(int x, int y, int width, int height, int rowHeight,
                          Function<T, String> displayName, Function<T, String> filterText) {
        super(x, y, width, height, Component.empty());
        this.rowHeight = rowHeight;
        this.displayName = displayName;
        this.filterText = filterText;
    }

    public ScrollSelector<T> onSelect(Consumer<T> cb) {
        this.onSelect = cb;
        return this;
    }

    public void setItems(List<T> items) {
        this.all.clear();
        this.all.addAll(items);
        this.applyFilter();
    }

    public void setQuery(String q) {
        this.query = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        this.applyFilter();
    }

    public void setSelected(T item) {
        if (item == null) {
            this.selectedIndex = -1;
            return;
        }
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i) == item || filtered.get(i).equals(item)) {
                this.selectedIndex = i;
                return;
            }
        }
        this.selectedIndex = -1;
    }

    private void applyFilter() {
        T previousSelection = getSelected();
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
        setSelected(previousSelection);
    }

    public T getSelected() {
        return selectedIndex >= 0 && selectedIndex < filtered.size() ? filtered.get(selectedIndex) : null;
    }

    private int visibleRows() {
        return Math.max(1, this.height / rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // background
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xC0101418);
        // top + bottom border
        g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF3A4A55);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF3A4A55);

        Font font = Minecraft.getInstance().font;
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
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0xFF1F6FBF);
            } else if (hovered) {
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0x40FFFFFF);
            }
            String name = displayName.apply(entry);
            String trimmed = font.plainSubstrByWidth(name, width - 14);
            g.drawString(font, trimmed, getX() + 5, rowY + (rowHeight - 8) / 2, 0xE0E0E0, false);
        }
        // scroll bar
        if (maxScroll() > 0) {
            int barX = getX() + width - 5;
            g.fill(barX, getY(), barX + 4, getY() + height, 0x60000000);
            int trackH = height;
            int thumbH = Math.max(10, trackH * visibleRows() / Math.max(1, filtered.size()));
            int thumbY = getY() + (trackH - thumbH) * scroll / Math.max(1, maxScroll());
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF80AACC);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered() || button != 0) {
            return false;
        }
        if (mouseX >= getX() + width - 6) {
            return false; // click on scroll bar area, ignored
        }
        int row = (int) ((mouseY - getY()) / rowHeight);
        int index = scroll + row;
        if (index >= 0 && index < filtered.size()) {
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
