package com.pewez.fantasticshortcuts.gui;

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
 * Lista desplazable genérica con filtro de texto, idéntica en arquitectura y estética a la usada por
 * FantasticCrates / FantasticSpawners.
 *
 * <p>Características:
 * <ul>
 *     <li>Fondo translúcido y bordes superior/inferior de acento.</li>
 *     <li>Filas con texto recortado al ancho ({@code font.plainSubstrByWidth}).</li>
 *     <li>Resaltado de hover y de fila seleccionada.</li>
 *     <li>Barra de scroll (track + thumb) cuando hay overflow.</li>
 *     <li>Selección por clic y desplazamiento con la rueda.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class ScrollSelector<T> extends AbstractWidget {

    private final List<T> all = new ArrayList<>();
    private final List<T> filtered = new ArrayList<>();
    private final Function<T, String> displayName;
    private final Function<T, String> filterText;
    private final int rowHeight;

    private Consumer<T> onSelect = t -> {};
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
        this.onSelect = cb == null ? t -> {} : cb;
        return this;
    }

    public void setItems(List<T> items) {
        this.all.clear();
        if (items != null) {
            this.all.addAll(items);
        }
        applyFilter();
    }

    public void setQuery(String q) {
        this.query = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        applyFilter();
    }

    /** Selecciona un elemento que cumpla un predicado (p. ej. para conservar la selección tras refrescar). */
    public void selectMatching(Function<T, Boolean> predicate) {
        for (int i = 0; i < filtered.size(); i++) {
            if (Boolean.TRUE.equals(predicate.apply(filtered.get(i)))) {
                this.selectedIndex = i;
                ensureVisible();
                return;
            }
        }
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
        return (selectedIndex >= 0 && selectedIndex < filtered.size()) ? filtered.get(selectedIndex) : null;
    }

    public boolean isEmpty() {
        return filtered.isEmpty();
    }

    private int visibleRows() {
        return Math.max(1, this.height / this.rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    private void ensureVisible() {
        if (selectedIndex < scroll) {
            scroll = selectedIndex;
        } else if (selectedIndex >= scroll + visibleRows()) {
            scroll = selectedIndex - visibleRows() + 1;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Fondo translúcido + bordes de acento (mismos tonos que la suite Fantastic).
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xC0101418);
        g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF2DD4FF); // borde superior aqua
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF2DD4FF);

        final Font font = Minecraft.getInstance().font;
        final int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            final int index = scroll + i;
            if (index < 0 || index >= filtered.size()) {
                break;
            }
            final T entry = filtered.get(index);
            final int rowY = getY() + i * rowHeight;
            final boolean hovered = mouseX >= getX() && mouseX < getX() + width - 6
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
            if (index == selectedIndex) {
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0xFF1E5A7A);
            } else if (hovered) {
                g.fill(getX(), rowY, getX() + width - 6, rowY + rowHeight, 0x40FFFFFF);
            }
            final String name = displayName.apply(entry);
            final String trimmed = font.plainSubstrByWidth(name, width - 12);
            g.drawString(font, trimmed, getX() + 4, rowY + (rowHeight - 8) / 2, 0xE0E0E0, false);
        }

        if (maxScroll() > 0) {
            final int barX = getX() + width - 5;
            g.fill(barX, getY(), barX + 4, getY() + height, 0x60000000);
            final int trackH = height;
            final int thumbH = Math.max(10, trackH * visibleRows() / Math.max(1, filtered.size()));
            final int thumbY = getY() + (trackH - thumbH) * scroll / Math.max(1, maxScroll());
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF2DD4FF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        final int row = (int) ((mouseY - getY()) / rowHeight);
        final int index = scroll + row;
        if (index >= 0 && index < filtered.size() && mouseX < getX() + width - 6) {
            this.selectedIndex = index;
            this.onSelect.accept(filtered.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        // Sin narración: lista visual.
    }
}
