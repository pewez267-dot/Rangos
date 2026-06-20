package com.fantasticterraform.client.hud;

import com.fantasticterraform.client.hud.panels.AmbiencePanel;
import com.fantasticterraform.client.hud.panels.BiomePanel;
import com.fantasticterraform.client.hud.panels.BrushesPanel;
import com.fantasticterraform.client.hud.panels.DungeonPanel;
import com.fantasticterraform.client.hud.panels.EditingPanel;
import com.fantasticterraform.client.hud.panels.HistoryPanel;
import com.fantasticterraform.client.hud.panels.MasksPanel;
import com.fantasticterraform.client.hud.panels.ParticlesPanel;
import com.fantasticterraform.client.hud.panels.PopulationPanel;
import com.fantasticterraform.client.hud.panels.SchematicsPanel;
import com.fantasticterraform.client.hud.panels.SelectionPanel;
import com.fantasticterraform.client.hud.panels.TerrainPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ventana de control centrada (estilo de la familia Fantastic): barra de titulo,
 * columna de pestanas a la izquierda y area de controles desplazable a la derecha,
 * con pie de estado. No pausa el mundo. Los cambios de pestana y los botones que
 * modifican estado reconstruyen el contenido de forma diferida (en el render
 * siguiente) para que las etiquetas se actualicen sin romper la lista de widgets.
 */
public class TerraformPanelScreen extends Screen {

    private static final int FOOTER_H = 24;
    private static final int TITLE_H = 18;

    // Dimensiones de la ventana, calculadas en init() segun el tamano de pantalla
    // (ventana grande y comoda, al estilo de las GUIs de la familia Fantastic).
    private int panelW = 460;
    private int tabW = 108;

    private static int lastTab = 0;

    private final List<HudPanel> panels = new ArrayList<>();
    private final List<AbstractWidget> tabWidgets = new ArrayList<>();
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private int active;
    private int scroll;
    private int contentExtent;

    private int panelLeft;
    private int panelTop;
    private int panelH;

    private int pendingTab = -1;
    private boolean pendingRebuild;

    public TerraformPanelScreen() {
        super(Component.literal("Fantastic Terraform"));
        panels.add(new SelectionPanel());
        panels.add(new EditingPanel());
        panels.add(new BrushesPanel());
        panels.add(new TerrainPanel());
        panels.add(new MasksPanel());
        panels.add(new SchematicsPanel());
        panels.add(new ParticlesPanel());
        panels.add(new AmbiencePanel());
        panels.add(new HistoryPanel());
        panels.add(new BiomePanel());
        panels.add(new PopulationPanel());
        panels.add(new DungeonPanel());
        active = Math.min(lastTab, panels.size() - 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.fantasticterraform.client.Keybinds.OPEN_PANELS.matches(keyCode, scanCode)
                && !(getFocused() instanceof EditBox)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        // Ventana grande y responsiva: ~58% del ancho y ~82% del alto, con topes comodos.
        panelW = Math.max(420, Math.min(620, (int) (this.width * 0.58)));
        tabW = Math.max(100, Math.min(140, panelW / 5));
        panelH = Math.max(240, Math.min(470, this.height - 30));
        panelLeft = (this.width - panelW) / 2;
        panelTop = (this.height - panelH) / 2;

        tabWidgets.clear();
        int ty = panelTop + TITLE_H + 5;
        for (int i = 0; i < panels.size(); i++) {
            final int index = i;
            Button tab = Button.builder(Component.literal(panels.get(i).title()), b -> pendingTab = index)
                    .bounds(panelLeft + 5, ty, tabW - 8, 16)
                    .build();
            tabWidgets.add(addRenderableWidget(tab));
            ty += 18;
        }
        rebuildContent();
    }

    private void applyTab(int index) {
        active = index;
        lastTab = index;
        scroll = 0;
        rebuildContent();
    }

    private void rebuildContent() {
        for (AbstractWidget w : contentWidgets) {
            removeWidget(w);
        }
        contentWidgets.clear();
        baseY.clear();
        panels.get(active).build(this, contentX(), contentY(), contentWidth(), panelH);
        int maxBottom = contentY();
        for (int i = 0; i < contentWidgets.size(); i++) {
            maxBottom = Math.max(maxBottom, baseY.get(i) + contentWidgets.get(i).getHeight());
        }
        contentExtent = maxBottom - contentY();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        reflow();
    }

    private int contentBottom() {
        return panelTop + panelH - FOOTER_H;
    }

    private int visibleHeight() {
        return contentBottom() - contentY();
    }

    private int maxScroll() {
        return Math.max(0, contentExtent - visibleHeight());
    }

    private void reflow() {
        int top = contentY();
        int bottom = contentBottom();
        for (int i = 0; i < contentWidgets.size(); i++) {
            AbstractWidget w = contentWidgets.get(i);
            int ny = baseY.get(i) - scroll;
            w.setY(ny);
            boolean visible = ny >= top && (ny + w.getHeight()) <= bottom;
            w.visible = visible;
            w.active = visible;
        }
    }

    public int contentX() {
        return panelLeft + tabW + 8;
    }

    public int contentY() {
        return panelTop + TITLE_H + 5;
    }

    public int contentWidth() {
        return panelW - tabW - 18;
    }

    // ----- fabricas de widgets (registran su Y base; los botones refrescan etiquetas) -----

    public <T extends AbstractWidget> T addContent(T widget) {
        contentWidgets.add(widget);
        baseY.add(widget.getY());
        return addRenderableWidget(widget);
    }

    /** Encabezado de seccion: titulo destacado que separa visualmente grupos de controles. */
    public StringWidget addHeader(int x, int y, int w, String text) {
        StringWidget s = new StringWidget(x, y, w, 11, Component.literal("\u00a7e\u00a7l" + text), this.font);
        s.alignLeft();
        return addContent(s);
    }

    public Button addButton(int x, int y, int w, int h, String label, Runnable action, String tooltip) {        Button b = Button.builder(Component.literal(label), btn -> {
            action.run();
            pendingRebuild = true;
        }).bounds(x, y, w, h).build();
        if (tooltip != null) {
            b.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return addContent(b);
    }

    public EditBox addEditBox(int x, int y, int w, int h, String initial, String tooltip, Consumer<String> onChange) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.empty());
        box.setMaxLength(256);
        box.setValue(initial == null ? "" : initial);
        box.setResponder(onChange);
        if (tooltip != null) {
            box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return addContent(box);
    }

    public SliderWidget addSlider(int x, int y, int w, int h, String label, double min, double max,
                                  double initial, boolean integer, String tooltip, Consumer<Double> onChange) {
        SliderWidget s = new SliderWidget(x, y, w, h, label, min, max, initial, integer, onChange);
        if (tooltip != null) {
            s.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return addContent(s);
    }

    public Button addPicker(int x, int y, int w, int h, String prefix, Supplier<String> current,
                            List<String> options, boolean blockIcons, String tooltip, Consumer<String> onSelect) {
        return addButton(x, y, w, h, prefix + ": " + shorten(current.get()),
                () -> openPicker(prefix, options, current.get(), blockIcons, onSelect), tooltip);
    }

    public void openPicker(String header, List<String> options, String current, boolean blockIcons, Consumer<String> onSelect) {
        Minecraft.getInstance().setScreen(new PickerScreen(new TerraformPanelScreen(), header, options, current, blockIcons, onSelect));
    }

    public static String shorten(String id) {
        if (id == null) {
            return "";
        }
        String s = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        return s.length() > 18 ? s.substring(0, 17) + "\u2026" : s;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= contentX() - 4 && mouseX <= panelLeft + panelW && mouseY >= contentY() && mouseY <= contentBottom()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * 16)));
            reflow();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Reconstruccion diferida (segura, fuera de la iteracion de eventos).
        if (pendingTab >= 0) {
            int t = pendingTab;
            pendingTab = -1;
            applyTab(t);
        } else if (pendingRebuild) {
            pendingRebuild = false;
            rebuildContent();
        }

        int right = panelLeft + panelW;
        int bottom = panelTop + panelH;
        // Marco de la ventana (estilo familia: panel oscuro con borde y barra de titulo).
        g.fill(panelLeft - 1, panelTop - 1, right + 1, bottom + 1, 0xFF000000);
        g.fill(panelLeft, panelTop, right, bottom, 0xF21A1A24);
        g.fill(panelLeft, panelTop, right, panelTop + TITLE_H, 0xFF7B2FBE);
        g.drawString(this.font, "\u00a7f\u2726 Fantastic Terraform \u2014 " + panels.get(active).title(),
                panelLeft + 6, panelTop + 4, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a77[G] Cerrar", right - 58, panelTop + 4, 0xFFFFFF, false);
        // Separador columna de pestanas.
        g.fill(panelLeft + tabW, panelTop + TITLE_H, panelLeft + tabW + 1, bottom, 0xFF000000);

        // Resaltado de la pestana activa.
        int ty = panelTop + TITLE_H + 5 + active * 18;
        g.fill(panelLeft + 2, ty - 1, panelLeft + tabW - 3, ty + 16, 0x55A05AFF);

        super.render(g, mouseX, mouseY, partialTick);

        // Pie de estado fijo.
        int footerTop = bottom - FOOTER_H;
        g.fill(panelLeft + tabW + 1, footerTop, right, bottom, 0xFF12121A);
        String status = panels.get(active).status();
        if (status != null) {
            drawWrapped(g, status, contentX(), footerTop + 3, contentWidth(), 2);
        }

        // Barra de scroll.
        if (maxScroll() > 0) {
            int trackTop = contentY();
            int trackBottom = contentBottom();
            int trackH = trackBottom - trackTop;
            int knobH = Math.max(16, trackH * visibleHeight() / Math.max(1, contentExtent));
            int knobY = trackTop + (trackH - knobH) * scroll / maxScroll();
            int barX = right - 3;
            g.fill(barX, trackTop, barX + 2, trackBottom, 0xFF303040);
            g.fill(barX, knobY, barX + 2, knobY + knobH, 0xFF9A5AFF);
        }
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int width, int maxLines) {
        int perLine = Math.max(10, width / 6);
        for (int li = 0; li < maxLines; li++) {
            int s = li * perLine;
            if (s >= text.length()) {
                break;
            }
            g.drawString(this.font, text.substring(s, Math.min(text.length(), s + perLine)), x, y + li * 10, 0xC8C8D8, false);
        }
    }

    public void drawLabel(GuiGraphics g, String text, int x, int y) {
        g.drawString(this.font, text, x, y, 0xC8C8D8, false);
    }
}
