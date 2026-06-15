/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Reusable scrollable list widget with built-in text filtering and single
 * selection. Used across every Fantastic Kits screen (items, groups, commands,
 * lore lines, enchantments) to keep the experience consistent.
 *
 * @param <T> the element type held by the list
 */
public final class ScrollSelector<T> extends AbstractWidget {

    private static final int ROW_HEIGHT = 12;
    private static final int COLOR_BG = 0xC0101018;
    private static final int COLOR_BORDER = 0xFF3A3A4A;
    private static final int COLOR_ROW_HOVER = 0x40FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x803A6EA5;
    private static final int COLOR_TEXT = 0xFFE6E6E6;
    private static final int COLOR_TEXT_SELECTED = 0xFFFFFFFF;

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();
    private Function<T, Component> labelFunction = item -> Component.literal(String.valueOf(item));
    private Consumer<T> onSelect = item -> { };
    private String filter = "";
    private int scrollOffset;
    private int selectedIndex = -1;

    public ScrollSelector(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setLabelFunction(Function<T, Component> labelFunction) {
        if (labelFunction != null) {
            this.labelFunction = labelFunction;
        }
    }

    public void setOnSelect(Consumer<T> onSelect) {
        if (onSelect != null) {
            this.onSelect = onSelect;
        }
    }

    public void setItems(List<T> items) {
        all.clear();
        if (items != null) {
            all.addAll(items);
        }
        applyFilter();
    }

    public void setFilter(String text) {
        this.filter = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        applyFilter();
    }

    private void applyFilter() {
        T previouslySelected = getSelected();
        filtered.clear();
        for (T item : all) {
            if (filter.isEmpty()
                    || labelFunction.apply(item).getString().toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(item);
            }
        }
        // Preserve selection if still visible.
        selectedIndex = previouslySelected == null ? -1 : filtered.indexOf(previouslySelected);
        clampScroll();
    }

    public T getSelected() {
        if (selectedIndex < 0 || selectedIndex >= filtered.size()) {
            return null;
        }
        return filtered.get(selectedIndex);
    }

    public void clearSelection() {
        selectedIndex = -1;
    }

    private int visibleRows() {
        return Math.max(1, this.height / ROW_HEIGHT);
    }

    private void clampScroll() {
        int max = Math.max(0, filtered.size() - visibleRows());
        if (scrollOffset > max) {
            scrollOffset = max;
        }
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, COLOR_BG);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);

        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            int index = scrollOffset + i;
            if (index >= filtered.size()) {
                break;
            }
            int rowY = y + 1 + i * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (index == selectedIndex) {
                graphics.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, COLOR_ROW_SELECTED);
            } else if (hovered) {
                graphics.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, COLOR_ROW_HOVER);
            }
            Component label = labelFunction.apply(filtered.get(index));
            String text = mc.font.plainSubstrByWidth(label.getString(), width - 8);
            graphics.drawString(mc.font, text, x + 4, rowY + 2,
                    index == selectedIndex ? COLOR_TEXT_SELECTED : COLOR_TEXT, false);
        }
        graphics.disableScissor();

        // Simple scrollbar indicator.
        if (filtered.size() > rows) {
            int barHeight = Math.max(8, (int) ((float) rows / filtered.size() * height));
            int maxScroll = Math.max(1, filtered.size() - rows);
            int barY = y + (int) ((float) scrollOffset / maxScroll * (height - barHeight));
            graphics.fill(x + width - 3, barY, x + width - 1, barY + barHeight, 0xFF8888AA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int relative = (int) (mouseY - (getY() + 1));
        if (relative < 0) {
            return false;
        }
        int row = relative / ROW_HEIGHT;
        int index = scrollOffset + row;
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
        scrollOffset -= (int) Math.signum(delta);
        clampScroll();
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                this.getMessage().equals(CommonComponents.EMPTY)
                        ? Component.literal("List selector")
                        : this.getMessage());
    }
}
