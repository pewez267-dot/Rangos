package com.fantasticchest.gui.widget;

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
 * Scrollable, searchable single-column list widget (Fantastic family style): dark rows,
 * highlighted selection, optional item icon, slim scrollbar.
 *
 * @param <T> element type
 */
public class ScrollSelector<T> extends AbstractWidget {

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon;
    private Consumer<T> onSelect = t -> {
    };
    private final int rowHeight;
    private int scroll;
    private int selectedIndex = -1;
    private String query = "";

    public ScrollSelector(final int x, final int y, final int width, final int height, final int rowHeight,
                          final Function<T, String> displayName, final Function<T, String> filterText,
                          final Function<T, ItemStack> icon) {
        super(x, y, width, height, Component.empty());
        this.rowHeight = rowHeight;
        this.displayName = displayName;
        this.filterText = filterText;
        this.icon = icon;
    }

    public ScrollSelector<T> onSelect(final Consumer<T> callback) {
        this.onSelect = callback == null ? t -> {
        } : callback;
        return this;
    }

    public void setItems(final List<T> items) {
        this.all.clear();
        if (items != null) {
            this.all.addAll(items);
        }
        applyFilter();
    }

    public void setQuery(final String q) {
        this.query = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        applyFilter();
    }

    private void applyFilter() {
        this.filtered.clear();
        if (this.query.isEmpty()) {
            this.filtered.addAll(this.all);
        } else {
            for (final T element : this.all) {
                if (this.filterText.apply(element).toLowerCase(Locale.ROOT).contains(this.query)) {
                    this.filtered.add(element);
                }
            }
        }
        this.scroll = 0;
        this.selectedIndex = -1;
    }

    private int visibleRows() {
        return Math.max(1, this.height / this.rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, this.filtered.size() - visibleRows());
    }

    @Override
    protected void renderWidget(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(getX(), getY(), getX() + this.width, getY() + this.height, -1072689128);
        g.fill(getX(), getY(), getX() + this.width, getY() + 1, -12961206);
        g.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, -12961206);

        final Font font = Minecraft.getInstance().font;
        final int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            final int index = this.scroll + i;
            if (index < 0 || index >= this.filtered.size()) {
                break;
            }
            final T element = this.filtered.get(index);
            final int rowY = getY() + i * this.rowHeight;
            final boolean hovered = mouseX >= getX() && mouseX < getX() + this.width - 6
                    && mouseY >= rowY && mouseY < rowY + this.rowHeight;
            if (index == this.selectedIndex) {
                g.fill(getX(), rowY, getX() + this.width - 6, rowY + this.rowHeight, -13800225);
            } else if (hovered) {
                g.fill(getX(), rowY, getX() + this.width - 6, rowY + this.rowHeight, 1090519039);
            }
            int textX = getX() + 3;
            if (this.icon != null) {
                final ItemStack stack = this.icon.apply(element);
                if (stack != null && !stack.isEmpty()) {
                    g.renderItem(stack, getX() + 2, rowY + (this.rowHeight - 16) / 2);
                }
                textX = getX() + 22;
            }
            final String name = this.displayName.apply(element);
            final String trimmed = font.plainSubstrByWidth(name, this.width - (textX - getX()) - 8);
            g.drawString(font, trimmed, textX, rowY + (this.rowHeight - 8) / 2, 14737632, false);
        }

        if (maxScroll() > 0) {
            final int barX = getX() + this.width - 5;
            g.fill(barX, getY(), barX + 4, getY() + this.height, 1610612736);
            final int trackH = this.height;
            final int thumbH = Math.max(10, trackH * visibleRows() / Math.max(1, this.filtered.size()));
            final int thumbY = getY() + (trackH - thumbH) * this.scroll / Math.max(1, maxScroll());
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, -8355680);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        final int row = (int) ((mouseY - getY()) / this.rowHeight);
        final int index = this.scroll + row;
        if (index >= 0 && index < this.filtered.size() && mouseX < getX() + this.width - 6) {
            this.selectedIndex = index;
            this.onSelect.accept(this.filtered.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return mouseX >= getX() && mouseX < getX() + this.width
                && mouseY >= getY() && mouseY < getY() + this.height;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }
}
