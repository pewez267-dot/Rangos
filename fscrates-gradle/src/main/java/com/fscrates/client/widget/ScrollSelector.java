// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.widget;

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
    private final List<T> all;
    private final List<T> filtered;
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon;
    private Consumer<T> onSelect;
    private final int rowHeight;
    private int scroll;
    private int selectedIndex;
    private String query;
    
    public ScrollSelector(final int x, final int y, final int width, final int height, final int rowHeight, final Function<T, String> displayName, final Function<T, String> filterText, final Function<T, ItemStack> icon) {
        super(x, y, width, height, (Component)Component.m_237119_());
        this.all = new ArrayList<T>();
        this.filtered = new ArrayList<T>();
        this.onSelect = (t -> {});
        this.scroll = 0;
        this.selectedIndex = -1;
        this.query = "";
        this.rowHeight = rowHeight;
        this.displayName = displayName;
        this.filterText = filterText;
        this.icon = icon;
    }
    
    public ScrollSelector<T> onSelect(final Consumer<T> cb) {
        this.onSelect = cb;
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
    
    private void applyFilter() {
        this.filtered.clear();
        if (this.query.isEmpty()) {
            this.filtered.addAll((Collection<? extends T>)this.all);
        }
        else {
            for (final T t : this.all) {
                if (this.filterText.apply(t).toLowerCase(Locale.ROOT).contains(this.query)) {
                    this.filtered.add(t);
                }
            }
        }
        this.scroll = 0;
        this.selectedIndex = -1;
    }
    
    public T getSelected() {
        return (this.selectedIndex >= 0 && this.selectedIndex < this.filtered.size()) ? this.filtered.get(this.selectedIndex) : null;
    }
    
    private int visibleRows() {
        return Math.max(1, this.f_93619_ / this.rowHeight);
    }
    
    private int maxScroll() {
        return Math.max(0, this.filtered.size() - this.visibleRows());
    }
    
    protected void m_87963_(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, -1072689128);
        g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + 1, -12961206);
        g.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 1, this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, -12961206);
        final Font font = Minecraft.m_91087_().f_91062_;
        for (int rows = this.visibleRows(), i = 0; i < rows; ++i) {
            final int index = this.scroll + i;
            if (index < 0) {
                break;
            }
            if (index >= this.filtered.size()) {
                break;
            }
            final T entry = this.filtered.get(index);
            final int rowY = this.m_252907_() + i * this.rowHeight;
            final boolean hovered = mouseX >= this.m_252754_() && mouseX < this.m_252754_() + this.f_93618_ - 6 && mouseY >= rowY && mouseY < rowY + this.rowHeight;
            if (index == this.selectedIndex) {
                g.m_280509_(this.m_252754_(), rowY, this.m_252754_() + this.f_93618_ - 6, rowY + this.rowHeight, -13800225);
            }
            else if (hovered) {
                g.m_280509_(this.m_252754_(), rowY, this.m_252754_() + this.f_93618_ - 6, rowY + this.rowHeight, 1090519039);
            }
            int textX = this.m_252754_() + 3;
            if (this.icon != null) {
                final ItemStack stack = this.icon.apply(entry);
                if (stack != null && !stack.m_41619_()) {
                    g.m_280480_(stack, this.m_252754_() + 1, rowY + (this.rowHeight - 16) / 2);
                }
                textX = this.m_252754_() + 20;
            }
            final String name = this.displayName.apply(entry);
            final String trimmed = font.m_92834_(name, this.f_93618_ - (textX - this.m_252754_()) - 8);
            g.m_280056_(font, trimmed, textX, rowY + (this.rowHeight - 8) / 2, 14737632, false);
        }
        if (this.maxScroll() > 0) {
            final int barX = this.m_252754_() + this.f_93618_ - 5;
            g.m_280509_(barX, this.m_252907_(), barX + 4, this.m_252907_() + this.f_93619_, 1610612736);
            final int trackH = this.f_93619_;
            final int thumbH = Math.max(10, trackH * this.visibleRows() / Math.max(1, this.filtered.size()));
            final int thumbY = this.m_252907_() + (trackH - thumbH) * this.scroll / Math.max(1, this.maxScroll());
            g.m_280509_(barX, thumbY, barX + 4, thumbY + thumbH, -8355680);
        }
    }
    
    public boolean m_6375_(final double mouseX, final double mouseY, final int button) {
        if (!this.m_5953_(mouseX, mouseY) || button != 0) {
            return false;
        }
        final int row = (int)((mouseY - this.m_252907_()) / this.rowHeight);
        final int index = this.scroll + row;
        if (index >= 0 && index < this.filtered.size() && mouseX < this.m_252754_() + this.f_93618_ - 6) {
            this.selectedIndex = index;
            this.onSelect.accept(this.filtered.get(index));
            return true;
        }
        return false;
    }
    
    public boolean m_6050_(final double mouseX, final double mouseY, final double delta) {
        if (!this.m_5953_(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - (int)Math.signum(delta)));
        return true;
    }
    
    public boolean m_5953_(final double mouseX, final double mouseY) {
        return mouseX >= this.m_252754_() && mouseX < this.m_252754_() + this.f_93618_ && mouseY >= this.m_252907_() && mouseY < this.m_252907_() + this.f_93619_;
    }
    
    protected void m_168797_(final NarrationElementOutput out) {
    }
}
