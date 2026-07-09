package com.fsmobs.client.widget;

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

/** Lista scrolleable con icono, nombre y checkbox opcional. Estilo GUI Fantastic. */
public class ScrollSelector<T> extends AbstractWidget {

    private static final int SCROLLBAR_WIDTH = 6;

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final Function<T, ItemStack> icon;
    private Consumer<T> onSelect = t -> {};
    private Function<T, Boolean> checked;

    private final int rowHeight;
    private int scroll = 0;
    private int selectedIndex = -1;
    private String query = "";
    private boolean draggingThumb = false;
    private int dragOffsetY = 0;

    public ScrollSelector(int x, int y, int width, int height, int rowHeight,
                          Function<T, String> displayName, Function<T, String> filterText,
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

    public ScrollSelector<T> withCheckbox(Function<T, Boolean> checkedState) {
        this.checked = checkedState;
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
                String text = this.filterText.apply(t).toLowerCase(Locale.ROOT);
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
        return this.getX() + this.width - SCROLLBAR_WIDTH;
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return mouseX >= contentRight() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    private int thumbHeight() {
        return this.filtered.isEmpty() ? this.height
                : Math.max(12, this.height * visibleRows() / Math.max(1, this.filtered.size()));
    }

    private int thumbY() {
        int max = maxScroll();
        return max <= 0 ? this.getY() : this.getY() + (this.height - thumbHeight()) * this.scroll / max;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -1072689128);
        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, -12961206);
        g.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, -12961206);
        Font font = Minecraft.getInstance().font;
        int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            int index = this.scroll + i;
            if (index < 0 || index >= this.filtered.size()) {
                break;
            }
            T entry = this.filtered.get(index);
            int rowY = this.getY() + i * this.rowHeight;
            boolean hovered = mouseX >= this.getX() && mouseX < contentRight() && mouseY >= rowY && mouseY < rowY + this.rowHeight;
            if (index == this.selectedIndex) {
                g.fill(this.getX(), rowY, contentRight(), rowY + this.rowHeight, -13800225);
            } else if (hovered) {
                g.fill(this.getX(), rowY, contentRight(), rowY + this.rowHeight, 0x40FFFFFF);
            }
            int textX = this.getX() + 3;
            if (this.icon != null) {
                ItemStack stack = this.icon.apply(entry);
                if (stack != null && !stack.isEmpty()) {
                    g.renderItem(stack, this.getX() + 1, rowY + (this.rowHeight - 16) / 2);
                }
                textX = this.getX() + 20;
            }
            if (this.checked != null) {
                boolean on = Boolean.TRUE.equals(this.checked.apply(entry));
                g.drawString(font, on ? "\u00a7c[BAN]" : "\u00a77[   ]", textX, rowY + (this.rowHeight - 8) / 2, 0xFFFFFF, false);
                textX += 34;
            }
            String name = this.displayName.apply(entry);
            String trimmed = font.plainSubstrByWidth(name, this.width - (textX - this.getX()) - 8);
            g.drawString(font, trimmed, textX, rowY + (this.rowHeight - 8) / 2, 0xE0E0E0, false);
        }
        int sbX = contentRight();
        g.fill(sbX, this.getY(), sbX + SCROLLBAR_WIDTH, this.getY() + this.height, 0x60000000);
        if (maxScroll() > 0) {
            int ty = thumbY();
            int th = thumbHeight();
            int color = this.draggingThumb ? -3092272 : -8355712;
            g.fill(sbX + 1, ty, sbX + SCROLLBAR_WIDTH - 1, ty + th, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        if (overScrollbar(mouseX, mouseY) && maxScroll() > 0) {
            int ty = thumbY();
            int th = thumbHeight();
            if (mouseY >= ty && mouseY < ty + th) {
                this.draggingThumb = true;
                this.dragOffsetY = (int) (mouseY - ty);
            } else {
                this.scroll = mouseY < ty ? Math.max(0, this.scroll - visibleRows())
                        : Math.min(maxScroll(), this.scroll + visibleRows());
            }
            return true;
        }
        int row = (int) ((mouseY - this.getY()) / this.rowHeight);
        int index = this.scroll + row;
        if (index >= 0 && index < this.filtered.size() && mouseX < contentRight()) {
            this.selectedIndex = index;
            this.onSelect.accept(this.filtered.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.draggingThumb && maxScroll() > 0) {
            int trackTop = this.getY();
            int trackHeight = this.height - thumbHeight();
            if (trackHeight > 0) {
                int newThumbY = (int) Math.max(trackTop, Math.min(trackTop + trackHeight, mouseY - this.dragOffsetY));
                this.scroll = (newThumbY - trackTop) * maxScroll() / trackHeight;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingThumb) {
            this.draggingThumb = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
