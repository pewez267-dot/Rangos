package com.fantasticterraform.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Menu desplegable a pantalla: lista completa, desplazable y con filtro opcional, para
 * elegir un valor (bloque, particula, sonido...) sin escribir comandos. No pausa el
 * mundo. Al elegir una entrada vuelve al panel anterior.
 */
public final class PickerScreen extends Screen {

    private static final int ROW_H = 14;

    private final Screen parent;
    private final String header;
    private final List<String> allOptions;
    private final String current;
    private final Consumer<String> onSelect;
    private final boolean blockIcons;

    private List<String> filtered;
    private EditBox search;
    private int scroll;
    private int listTop;
    private int listBottom;
    private int boxLeft;
    private int boxRight;

    public PickerScreen(Screen parent, String header, List<String> options, String current,
                        boolean blockIcons, Consumer<String> onSelect) {
        super(Component.literal(header));
        this.parent = parent;
        this.header = header;
        this.allOptions = options;
        this.current = current;
        this.blockIcons = blockIcons;
        this.onSelect = onSelect;
        this.filtered = new ArrayList<>(options);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int w = 260;
        boxLeft = (this.width - w) / 2;
        boxRight = boxLeft + w;
        int top = Math.max(20, this.height / 2 - 130);
        listTop = top + 46;
        listBottom = Math.min(this.height - 30, top + 250);

        search = new EditBox(this.font, boxLeft + 8, top + 22, w - 16, 16, Component.literal("Buscar"));
        search.setHint(Component.literal("Escribe para filtrar (opcional)..."));
        search.setResponder(this::applyFilter);
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(boxRight - 70, listBottom + 4, 66, 18).build());
    }

    private void applyFilter(String text) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        filtered = new ArrayList<>();
        for (String opt : allOptions) {
            if (q.isEmpty() || opt.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(opt);
            }
        }
        scroll = 0;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= boxLeft && mouseX <= boxRight && mouseY >= listTop && mouseY <= listBottom) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseX >= boxLeft + 4 && mouseX <= boxRight - 4
                && mouseY >= listTop && mouseY < listBottom) {
            int row = (int) ((mouseY - listTop) / ROW_H) + scroll;
            if (row >= 0 && row < filtered.size()) {
                onSelect.accept(filtered.get(row));
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int top = listTop - 46;
        g.fill(boxLeft, top, boxRight, listBottom + 26, 0xFF1A1A24);
        g.fill(boxLeft, top, boxRight, top + 18, 0xFF2B2B3A);
        g.drawString(this.font, "\u00a7f" + header + "  \u00a77(" + filtered.size() + ")", boxLeft + 8, top + 5, 0xFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick);

        // Filas visibles.
        g.fill(boxLeft + 4, listTop, boxRight - 4, listBottom, 0xFF101018);
        int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= filtered.size()) {
                break;
            }
            String opt = filtered.get(index);
            int ry = listTop + i * ROW_H;
            boolean hover = mouseX >= boxLeft + 4 && mouseX <= boxRight - 4 && mouseY >= ry && mouseY < ry + ROW_H;
            boolean isCurrent = opt.equals(current);
            if (hover) {
                g.fill(boxLeft + 4, ry, boxRight - 4, ry + ROW_H, 0x553AA0FF);
            } else if (isCurrent) {
                g.fill(boxLeft + 4, ry, boxRight - 4, ry + ROW_H, 0x3340C040);
            }
            int textX = boxLeft + 8;
            if (blockIcons) {
                ItemStack icon = iconFor(opt);
                if (!icon.isEmpty()) {
                    g.renderItem(icon, boxLeft + 6, ry - 1);
                }
                textX = boxLeft + 26;
            }
            g.drawString(this.font, (isCurrent ? "\u00a7a" : "\u00a7f") + opt, textX, ry + 3, 0xFFFFFF, false);
        }

        // Barra de scroll.
        if (maxScroll() > 0) {
            int trackH = listBottom - listTop;
            int knobH = Math.max(12, trackH * visibleRows() / Math.max(1, filtered.size()));
            int knobY = listTop + (trackH - knobH) * scroll / maxScroll();
            g.fill(boxRight - 4, listTop, boxRight - 2, listBottom, 0xFF303040);
            g.fill(boxRight - 4, knobY, boxRight - 2, knobY + knobH, 0xFF6090FF);
        }
    }

    private static ItemStack iconFor(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return ItemStack.EMPTY;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(rl);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }
}
