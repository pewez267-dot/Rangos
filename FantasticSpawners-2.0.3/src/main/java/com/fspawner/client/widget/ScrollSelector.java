// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.client.widget;

import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Iterator;
import java.util.Locale;
import java.util.Collection;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Function;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

public class ScrollSelector<T> extends AbstractWidget
{
    private static final int SCROLLBAR_WIDTH = 6;
    private final List<T> all;
    private final List<T> filtered;
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon;
    private Consumer<T> onSelect;
    private Function<T, Boolean> checked;
    private final int rowHeight;
    private int scroll;
    private int selectedIndex;
    private String query;
    private boolean draggingThumb;
    private int dragOffsetY;
    
    public ScrollSelector(final int x, final int y, final int width, final int height, final int rowHeight, final Function<T, String> displayName, final Function<T, String> filterText, final Function<T, ItemStack> icon) {
        super(x, y, width, height, (Component)Component.empty());
        this.all = new ArrayList<T>();
        this.filtered = new ArrayList<T>();
        this.onSelect = (t -> {});
        this.scroll = 0;
        this.selectedIndex = -1;
        this.query = "";
        this.draggingThumb = false;
        this.dragOffsetY = 0;
        this.rowHeight = rowHeight;
        this.displayName = displayName;
        this.filterText = filterText;
        this.icon = icon;
    }
    
    public ScrollSelector<T> onSelect(final Consumer<T> cb) {
        this.onSelect = cb;
        return this;
    }
    
    public ScrollSelector<T> withCheckbox(final Function<T, Boolean> checkedState) {
        this.checked = checkedState;
        return this;
    }
    
    public void setItems(final List<T> items) {
        this.all.clear();
        this.all.addAll((Collection<? extends T>)items);
        this.applyFilter();
    }
    
    public void setQuery(final String q) {
        this.query = ((q == null) ? "" : q.toLowerCase(Locale.ROOT).trim());
        this.applyFilter();
    }
    
    public T getSelected() {
        return (this.selectedIndex >= 0 && this.selectedIndex < this.filtered.size()) ? this.filtered.get(this.selectedIndex) : null;
    }
    
    public void setSelected(final T item) {
        this.selectedIndex = ((item == null) ? -1 : this.filtered.indexOf(item));
        if (this.selectedIndex >= 0) {
            this.ensureVisible();
        }
    }
    
    private void applyFilter() {
        this.filtered.clear();
        if (this.query.isEmpty()) {
            this.filtered.addAll((Collection<? extends T>)this.all);
        }
        else {
            for (final T t : this.all) {
                final String text = this.filterText.apply(t).toLowerCase(Locale.ROOT);
                if (text.contains(this.query)) {
                    this.filtered.add(t);
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
        return Math.max(0, this.filtered.size() - this.visibleRows());
    }
    
    private int contentRight() {
        return this.getX() + this.width - 6;
    }
    
    private boolean overScrollbar(final double mouseX, final double mouseY) {
        return mouseX >= this.contentRight() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
    
    private int thumbHeight() {
        if (this.filtered.isEmpty()) {
            return this.height;
        }
        return Math.max(12, this.height * this.visibleRows() / Math.max(1, this.filtered.size()));
    }
    
    private int thumbY() {
        final int max = this.maxScroll();
        if (max <= 0) {
            return this.getY();
        }
        return this.getY() + (this.height - this.thumbHeight()) * this.scroll / max;
    }
    
    protected void renderWidget(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -1072689128);
        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, -12961206);
        g.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, -12961206);
        final Font font = Minecraft.getInstance().font;
        for (int rows = this.visibleRows(), i = 0; i < rows; ++i) {
            final int index = this.scroll + i;
            if (index < 0) {
                break;
            }
            if (index >= this.filtered.size()) {
                break;
            }
            final T entry = this.filtered.get(index);
            final int rowY = this.getY() + i * this.rowHeight;
            final boolean hovered = mouseX >= this.getX() && mouseX < this.contentRight() && mouseY >= rowY && mouseY < rowY + this.rowHeight;
            if (index == this.selectedIndex) {
                g.fill(this.getX(), rowY, this.contentRight(), rowY + this.rowHeight, -13800225);
            }
            else if (hovered) {
                g.fill(this.getX(), rowY, this.contentRight(), rowY + this.rowHeight, 1090519039);
            }
            int textX = this.getX() + 3;
            if (this.icon != null) {
                final ItemStack stack = this.icon.apply(entry);
                if (stack != null && !stack.isEmpty()) {
                    g.renderItem(stack, this.getX() + 1, rowY + (this.rowHeight - 16) / 2);
                }
                textX = this.getX() + 20;
            }
            if (this.checked != null) {
                final boolean on = Boolean.TRUE.equals(this.checked.apply(entry));
                g.drawString(font, on ? "§a[x]" : "§7[ ]", textX, rowY + (this.rowHeight - 8) / 2, 16777215, false);
                textX += 22;
            }
            final String name = this.displayName.apply(entry);
            final String trimmed = font.plainSubstrByWidth(name, this.width - (textX - this.getX()) - 8);
            g.drawString(font, trimmed, textX, rowY + (this.rowHeight - 8) / 2, 14737632, false);
        }
        final int barX = this.contentRight();
        g.fill(barX, this.getY(), barX + 6, this.getY() + this.height, 1610612736);
        if (this.maxScroll() > 0) {
            final int ty = this.thumbY();
            final int th = this.thumbHeight();
            final int color = this.draggingThumb ? -3092272 : -8355712;
            g.fill(barX + 1, ty, barX + 6 - 1, ty + th, color);
        }
    }
    
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (!this.isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        if (this.overScrollbar(mouseX, mouseY) && this.maxScroll() > 0) {
            final int ty = this.thumbY();
            final int th = this.thumbHeight();
            if (mouseY >= ty && mouseY < ty + th) {
                this.draggingThumb = true;
                this.dragOffsetY = (int)(mouseY - ty);
            }
            else if (mouseY < ty) {
                this.scroll = Math.max(0, this.scroll - this.visibleRows());
            }
            else {
                this.scroll = Math.min(this.maxScroll(), this.scroll + this.visibleRows());
            }
            return true;
        }
        final int row = (int)((mouseY - this.getY()) / this.rowHeight);
        final int index = this.scroll + row;
        if (index >= 0 && index < this.filtered.size() && mouseX < this.contentRight()) {
            this.selectedIndex = index;
            this.onSelect.accept(this.filtered.get(index));
            return true;
        }
        return false;
    }
    
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dx, final double dy) {
        if (this.draggingThumb && this.maxScroll() > 0) {
            final int trackTop = this.getY();
            final int trackHeight = this.height - this.thumbHeight();
            if (trackHeight > 0) {
                final int newThumbY = (int)Math.max(trackTop, Math.min(trackTop + trackHeight, mouseY - this.dragOffsetY));
                this.scroll = (newThumbY - trackTop) * this.maxScroll() / trackHeight;
            }
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && this.draggingThumb) {
            this.draggingThumb = false;
            return true;
        }
        return false;
    }
    
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - (int)Math.signum(delta)));
        return true;
    }
    
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (!this.isFocused()) {
            return false;
        }
        switch (keyCode) {
            case 264: {
                if (this.filtered.isEmpty()) {
                    return true;
                }
                this.selectedIndex = Math.min(this.filtered.size() - 1, Math.max(0, this.selectedIndex) + 1);
                this.ensureVisible();
                this.onSelect.accept(this.filtered.get(this.selectedIndex));
                return true;
            }
            case 265: {
                if (this.filtered.isEmpty()) {
                    return true;
                }
                this.selectedIndex = Math.max(0, (this.selectedIndex < 0) ? 0 : (this.selectedIndex - 1));
                this.ensureVisible();
                this.onSelect.accept(this.filtered.get(this.selectedIndex));
                return true;
            }
            case 267: {
                this.scroll = Math.min(this.maxScroll(), this.scroll + this.visibleRows());
                return true;
            }
            case 266: {
                this.scroll = Math.max(0, this.scroll - this.visibleRows());
                return true;
            }
            case 268: {
                this.scroll = 0;
                return true;
            }
            case 269: {
                this.scroll = this.maxScroll();
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    private void ensureVisible() {
        if (this.selectedIndex < this.scroll) {
            this.scroll = this.selectedIndex;
        }
        else if (this.selectedIndex >= this.scroll + this.visibleRows()) {
            this.scroll = this.selectedIndex - this.visibleRows() + 1;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
    }
    
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
    
    protected void updateWidgetNarration(final NarrationElementOutput out) {
    }
}
