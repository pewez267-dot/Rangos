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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ventana de control CENTRADA, calcada en densidad y estilo de FantasticCrates
 * (com.fscrates.client.screen.CrateEditorScreen): barra de titulo, fila de pestanas
 * planas dibujadas con drawString, linea de acento, area de contenido densa con filas
 * de 14px y pie con boton Cerrar.
 *
 * <p>Reglas de diseno (identicas a la referencia):
 * <ul>
 *   <li>Filas de {@value #RH}px de alto, paso de {@value #RS}px (gap de 2px).</li>
 *   <li>Etiquetas en gris {@code 0xE0E0E0} a la izquierda, widget compacto a la derecha.</li>
 *   <li>Subtitulos de seccion en blanco negrita (\u00a7l\u00a7f). Nada mas lleva color salvo
 *       las acciones (verde) y las acciones destructivas (rojo).</li>
 *   <li>Pestanas: inactiva = texto gris plano; activa = texto blanco + barra de acento.</li>
 * </ul>
 */
public class TerraformPanelScreen extends Screen {

    /** Alto de una fila/control compacto. */
    public static final int RH = 14;
    /** Paso entre filas (alto + 2px de gap). */
    public static final int RS = 16;
    /** Alto del unico boton de accion principal por pestana. */
    public static final int ACTION_H = 16;

    // Colores EXACTOS del mod de referencia (FantasticCrates / CrateEditorScreen).
    private static final int COLOR_PANEL = -535291870;
    private static final int COLOR_TITLEBAR = -14408646;
    private static final int COLOR_ACCENT = -12961206;
    private static final int COLOR_HELP = 10133680;

    // Texto: etiquetas y pestanas.
    private static final int COLOR_LABEL = 0xE0E0E0;
    private static final int COLOR_TAB_OFF = 0x888888;
    private static final int COLOR_TAB_ON = 0xFFFFFF;
    private static final int COLOR_TAB_BAR = 0xFFB07CFF; // barra de acento morada bajo la pestana activa

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int tabsBottomY;

    private static int lastTab = 0;

    private final List<HudPanel> panels = new ArrayList<>();
    private final String[] tabLabels;
    private final List<int[]> tabRects = new ArrayList<>(); // [x,y,w,h] por pestana

    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private final List<TextLabel> labels = new ArrayList<>();

    private int active;
    private int scroll;
    private int contentExtent;

    private int pendingTab = -1;
    private boolean pendingRebuild;

    public TerraformPanelScreen() {
        super(Component.literal("Fantastic Terraform"));
        // ORDEN DE PESTANAS (obligatorio):
        // Seleccion, Edicion, Terreno, Brushes, Mascaras, Biomas, Poblacion,
        // Dungeons, Schematics, Particulas, Ambiente, Historial.
        panels.add(new SelectionPanel());
        panels.add(new EditingPanel());
        panels.add(new TerrainPanel());
        panels.add(new BrushesPanel());
        panels.add(new MasksPanel());
        panels.add(new BiomePanel());
        panels.add(new PopulationPanel());
        panels.add(new DungeonPanel());
        panels.add(new SchematicsPanel());
        panels.add(new ParticlesPanel());
        panels.add(new AmbiencePanel());
        panels.add(new HistoryPanel());
        // Etiquetas cortas para que quepan 12 pestanas en una fila.
        tabLabels = new String[] {
                "Selec", "Edita", "Terr", "Brush", "Masc", "Bioma",
                "Pobla", "Dung", "Esquem", "Part", "Amb", "Hist"
        };
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
        panelWidth = Math.min(this.width - 16, 540);
        panelHeight = Math.min(this.height - 16, 320);
        leftPos = (this.width - panelWidth) / 2;
        topPos = (this.height - panelHeight) / 2;

        // Pestanas: planas, una fila, alto 14px, dibujadas con drawString en render().
        tabRects.clear();
        int gap = 2;
        int n = panels.size();
        int availW = panelWidth - 16;
        int tabW = (availW - (n - 1) * gap) / n;
        int startX = leftPos + 8;
        int tabsY = topPos + 20;
        for (int i = 0; i < n; i++) {
            int bx = startX + i * (tabW + gap);
            tabRects.add(new int[] {bx, tabsY, tabW, RH});
        }
        tabsBottomY = tabsY + RH;

        // Pie: boton Cerrar a la izquierda (compacto).
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(leftPos + 8, topPos + panelHeight - 17, 60, RH).build());

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
        labels.clear();
        panels.get(active).build(this, contentX(), contentY(), contentWidth(), visibleHeight());
        int maxBottom = contentY();
        for (int i = 0; i < contentWidgets.size(); i++) {
            maxBottom = Math.max(maxBottom, baseY.get(i) + contentWidgets.get(i).getHeight());
        }
        for (TextLabel l : labels) {
            maxBottom = Math.max(maxBottom, l.baseY + 9);
        }
        contentExtent = maxBottom - contentY();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        reflow();
    }

    private int contentBottom() {
        return topPos + panelHeight - 20;
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
        return leftPos + 8;
    }

    public int contentY() {
        return tabsBottomY + 16;
    }

    public int contentWidth() {
        return panelWidth - 16;
    }

    // ---------------------------------------------------------------------
    //  API densa para los paneles (registra Y base; los botones refrescan etiquetas)
    // ---------------------------------------------------------------------

    public <T extends AbstractWidget> T add(T widget) {
        contentWidgets.add(widget);
        baseY.add(widget.getY());
        return addRenderableWidget(widget);
    }

    /** Etiqueta gris (sin color) en la posicion dada. Se desplaza con el scroll. */
    public void label(int x, int y, String text) {
        labels.add(new TextLabel(x, y, text, COLOR_LABEL));
    }

    /** Subtitulo de seccion: blanco negrita (\u00a7l\u00a7f). Sin ningun otro color. */
    public void section(int x, int y, String text) {
        labels.add(new TextLabel(x, y, "\u00a7l\u00a7f" + text, COLOR_TAB_ON));
    }

    /**
     * Fila label+widget en una sola linea: dibuja {@code label} en gris a la izquierda y
     * coloca {@code control} (con su ancho ya fijado) alineado a la derecha del ancho dado.
     * Calcado del patron de FantasticCrates (label a la izq, EditBox/boton a la der).
     */
    public AbstractWidget addRow(int x, int y, int width, String label, AbstractWidget control) {
        int cw = control.getWidth();
        control.setX(x + width - cw);
        control.setY(y);
        label(x, y + 3, label);
        return control;
    }

    public Button addButton(int x, int y, int w, int h, String label, Runnable action, String tooltip) {
        Button b = Button.builder(Component.literal(label), btn -> {
            action.run();
            pendingRebuild = true;
        }).bounds(x, y, w, h).build();
        if (tooltip != null) {
            b.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return add(b);
    }

    public EditBox addEditBox(int x, int y, int w, int h, String initial, String tooltip, Consumer<String> onChange) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.empty());
        box.setMaxLength(256);
        box.setValue(initial == null ? "" : initial);
        box.setResponder(onChange);
        if (tooltip != null) {
            box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return add(box);
    }

    public SliderWidget addSlider(int x, int y, int w, int h, String label, double min, double max,
                                  double initial, boolean integer, String tooltip, Consumer<Double> onChange) {
        SliderWidget s = new SliderWidget(x, y, w, h, label, min, max, initial, integer, onChange);
        if (tooltip != null) {
            s.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return add(s);
    }

    /** Boton selector que abre el desplegable; muestra solo el valor (corto), sin color. */
    public Button addPicker(int x, int y, int w, int h, Supplier<String> current,
                            List<String> options, boolean blockIcons, String tooltip, Consumer<String> onSelect) {
        return addButton(x, y, w, h, shorten(current.get()),
                () -> openPicker(shorten(current.get()), options, current.get(), blockIcons, onSelect), tooltip);
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

    // ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < tabRects.size(); i++) {
                int[] r = tabRects.get(i);
                if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                    pendingTab = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

        // Panel centrado con los colores exactos de la referencia.
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, COLOR_PANEL);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 18, COLOR_TITLEBAR);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, COLOR_ACCENT);
        // Linea de acento bajo la fila de pestanas.
        g.fill(leftPos + 6, tabsBottomY + 1, leftPos + panelWidth - 6, tabsBottomY + 2, COLOR_ACCENT);

        // Titulo (blanco, sin codigos de color decorativos).
        g.drawString(this.font, "Fantastic Terraform  -  " + panels.get(active).title(),
                leftPos + 8, topPos + 5, COLOR_TAB_ON, false);
        g.drawString(this.font, "[G]", leftPos + panelWidth - 20, topPos + 5, COLOR_HELP, false);

        // Pestanas planas: color por estado, barra de acento bajo la activa.
        for (int i = 0; i < tabRects.size(); i++) {
            int[] r = tabRects.get(i);
            boolean on = i == active;
            String text = tabLabels[i];
            int tx = r[0] + (r[2] - this.font.width(text)) / 2;
            int ty = r[1] + (r[3] - 8) / 2;
            g.drawString(this.font, text, tx, ty, on ? COLOR_TAB_ON : COLOR_TAB_OFF, false);
            if (on) {
                g.fill(r[0], r[1] + r[3], r[0] + r[2], r[1] + r[3] + 1, COLOR_TAB_BAR);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Etiquetas y subtitulos por encima de los widgets, recortados al area de contenido.
        int top = contentY();
        int bottom = contentBottom();
        for (TextLabel l : labels) {
            int ly = l.baseY - scroll;
            if (ly >= top - 2 && ly + 9 <= bottom + 2) {
                g.drawString(this.font, l.text, l.x, ly, l.color, false);
            }
        }

        // Descripcion / estado de la seccion (gris), bajo el separador.
        String status = panels.get(active).status();
        if (status != null && !status.isEmpty()) {
            String trimmed = this.font.plainSubstrByWidth(status, panelWidth - 90);
            g.drawString(this.font, trimmed, leftPos + 8, tabsBottomY + 4, COLOR_HELP, false);
        }

        // Barra de scroll del contenido.
        if (maxScroll() > 0) {
            int trackTop = contentY();
            int trackBottom = contentBottom();
            int trackH = trackBottom - trackTop;
            int knobH = Math.max(16, trackH * visibleHeight() / Math.max(1, contentExtent));
            int knobY = trackTop + (trackH - knobH) * scroll / maxScroll();
            int barX = leftPos + panelWidth - 4;
            g.fill(barX, trackTop, barX + 2, trackBottom, 0xFF303040);
            g.fill(barX, knobY, barX + 2, knobY + knobH, COLOR_TAB_BAR);
        }
    }

    /** Etiqueta de texto diferida (se dibuja tras los widgets, recortada y con scroll). */
    private record TextLabel(int x, int baseY, String text, int color) {
    }
}
