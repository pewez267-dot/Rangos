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

    private static final int FOOTER_H = 28;

    // Layout a pantalla completa (estilo FantasticCrates): pestanas horizontales arriba,
    // contenido en columna y barra inferior. Calculado en init() segun el tamano de pantalla.
    private static final int MARGIN = 10;
    private int tabsBottomY;
    private int contentColW;
    private final List<int[]> tabBounds = new ArrayList<>(); // [x,y,w,h] por pestana

    private static int lastTab = 0;

    private final List<HudPanel> panels = new ArrayList<>();
    private final List<AbstractWidget> tabWidgets = new ArrayList<>();
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private int active;
    private int scroll;
    private int contentExtent;

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
        tabWidgets.clear();
        tabBounds.clear();
        int availW = this.width - 2 * MARGIN;
        int minTab = 92;
        int gap = 4;
        int perRow = Math.max(1, Math.min(panels.size(), (availW + gap) / (minTab + gap)));
        int rows = (panels.size() + perRow - 1) / perRow;
        int btnW = (availW - (perRow - 1) * gap) / perRow;
        int btnH = 18;
        int top = 22;
        for (int i = 0; i < panels.size(); i++) {
            final int index = i;
            int rowIdx = i / perRow;
            int col = i % perRow;
            int bx = MARGIN + col * (btnW + gap);
            int by = top + rowIdx * (btnH + gap);
            Button tab = Button.builder(Component.literal(panels.get(i).title()), b -> pendingTab = index)
                    .bounds(bx, by, btnW, btnH).build();
            tabWidgets.add(addRenderableWidget(tab));
            tabBounds.add(new int[] {bx, by, btnW, btnH});
        }
        tabsBottomY = top + rows * (btnH + gap);

        // Columna de contenido comoda (no estirar los controles a todo el ancho).
        contentColW = Math.min(availW - 16, 560);

        // Boton Cerrar en la barra inferior (izquierda).
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(MARGIN, this.height - 23, 96, 18).build());

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
        panels.get(active).build(this, contentX(), contentY(), contentWidth(), visibleHeight());
        int maxBottom = contentY();
        for (int i = 0; i < contentWidgets.size(); i++) {
            maxBottom = Math.max(maxBottom, baseY.get(i) + contentWidgets.get(i).getHeight());
        }
        contentExtent = maxBottom - contentY();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        reflow();
    }

    private int contentBottom() {
        return this.height - FOOTER_H;
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
        return MARGIN + 8;
    }

    public int contentY() {
        return tabsBottomY + 16;
    }

    public int contentWidth() {
        return contentColW;
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
        if (mouseY >= contentY() && mouseY <= contentBottom()) {
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

        // Fondo oscuro a pantalla completa (el mundo se ve apenas detras).
        g.fill(0, 0, this.width, this.height, 0xD6101018);

        // Titulo arriba a la izquierda.
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Terraform \u00a7d\u2726 \u00a78\u2014 \u00a7f"
                + panels.get(active).title(), MARGIN, 7, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a78[G] cerrar", this.width - MARGIN - 56, 7, 0xFFFFFF, false);

        // Linea separadora bajo las pestanas.
        g.fill(MARGIN, tabsBottomY + 2, this.width - MARGIN, tabsBottomY + 3, 0xFF000000);
        g.fill(MARGIN, tabsBottomY + 3, this.width - MARGIN, tabsBottomY + 4, 0x40FFFFFF);

        // Resaltado de la pestana activa (barra de acento bajo el boton).
        if (active >= 0 && active < tabBounds.size()) {
            int[] b = tabBounds.get(active);
            g.fill(b[0], b[1] + b[3], b[0] + b[2], b[1] + b[3] + 2, 0xFFB07CFF);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Descripcion / estado de la seccion, en gris, justo bajo el separador.
        String status = panels.get(active).status();
        if (status != null) {
            drawWrapped(g, status, contentX(), tabsBottomY + 6, this.width - 2 * MARGIN - 12, 1);
        }

        // Barra inferior.
        int footerTop = this.height - FOOTER_H;
        g.fill(0, footerTop, this.width, footerTop + 1, 0xFF000000);
        g.fill(0, footerTop + 1, this.width, this.height, 0xF20E0E16);

        // Barra de scroll del contenido.
        if (maxScroll() > 0) {
            int trackTop = contentY();
            int trackBottom = contentBottom();
            int trackH = trackBottom - trackTop;
            int knobH = Math.max(16, trackH * visibleHeight() / Math.max(1, contentExtent));
            int knobY = trackTop + (trackH - knobH) * scroll / maxScroll();
            int barX = this.width - MARGIN + 2;
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
