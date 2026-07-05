package com.fscrates.client.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ScrollSelector<T>
extends AbstractWidget {
    private final List<T> all = new ArrayList<T>();
    private final List<T> filtered = new ArrayList<T>();
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon;
    private Consumer<T> onSelect = t -> {};
    private final int rowHeight;
    private int scroll = 0;
    private int selectedIndex = -1;
    private String query = "";

    public ScrollSelector(int x, int y, int width, int height, int rowHeight, Function<T, String> displayName, Function<T, String> filterText, Function<T, ItemStack> icon) {
        super(x, y, width, height, (Component)Component.empty());
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
        this.applyFilter();
    }

    public void setQuery(String q) {
        this.query = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        this.applyFilter();
    }

    private void applyFilter() {
        this.filtered.clear();
        if (this.query.isEmpty()) {
            this.filtered.addAll(this.all);
        } else {
            for (T t : this.all) {
                if (!this.filterText.apply(t).toLowerCase(Locale.ROOT).contains(this.query)) continue;
                this.filtered.add(t);
            }
        }
        this.scroll = 0;
        this.selectedIndex = -1;
    }

    public T getSelected() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.filtered.size() ? (T)this.filtered.get(this.selectedIndex) : null;
    }

    private int visibleRows() {
        return Math.max(1, this.height / this.rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, this.filtered.size() - this.visibleRows());
    }

    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int index;
        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -1072689128);
        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, -12961206);
        g.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, -12961206);
        Font font = Minecraft.getInstance().font;
        int rows = this.visibleRows();
        for (int i = 0; i < rows && (index = this.scroll + i) >= 0 && index < this.filtered.size(); ++i) {
            boolean hovered;
            T entry = this.filtered.get(index);
            int rowY = this.getY() + i * this.rowHeight;
            boolean bl = hovered = mouseX >= this.getX() && mouseX < this.getX() + this.width - 6 && mouseY >= rowY && mouseY < rowY + this.rowHeight;
            if (index == this.selectedIndex) {
                g.fill(this.getX(), rowY, this.getX() + this.width - 6, rowY + this.rowHeight, -13800225);
            } else if (hovered) {
                g.fill(this.getX(), rowY, this.getX() + this.width - 6, rowY + this.rowHeight, 0x40FFFFFF);
            }
            int textX = this.getX() + 3;
            if (this.icon != null) {
                ItemStack stack = this.icon.apply(entry);
                if (stack != null && !stack.isEmpty()) {
                    g.renderItem(stack, this.getX() + 1, rowY + (this.rowHeight - 16) / 2);
                }
                textX = this.getX() + 20;
            }
            String name = this.displayName.apply(entry);
            String trimmed = font.plainSubstrByWidth(name, this.width - (textX - this.getX()) - 8);
            g.drawString(font, trimmed, textX, rowY + (this.rowHeight - 8) / 2, 0xE0E0E0, false);
        }
        if (this.maxScroll() > 0) {
            rows = this.getX() + this.width - 5;
            g.fill(rows, this.getY(), rows + 4, this.getY() + this.height, 0x60000000);
            int trackH = this.height;
            int thumbH = Math.max(10, trackH * this.visibleRows() / Math.max(1, this.filtered.size()));
            int thumbY = this.getY() + (trackH - thumbH) * this.scroll / Math.max(1, this.maxScroll());
            g.fill(rows, thumbY, rows + 4, thumbY + thumbH, -8355680);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && button == 0) {
            int row = (int)((mouseY - (double)this.getY()) / (double)this.rowHeight);
            int index = this.scroll + row;
            if (index >= 0 && index < this.filtered.size() && mouseX < (double)(this.getX() + this.width - 6)) {
                this.selectedIndex = index;
                this.onSelect.accept(this.filtered.get(index));
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - (int)Math.signum(delta)));
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double)this.getX() && mouseX < (double)(this.getX() + this.width) && mouseY >= (double)this.getY() && mouseY < (double)(this.getY() + this.height);
    }

    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}

