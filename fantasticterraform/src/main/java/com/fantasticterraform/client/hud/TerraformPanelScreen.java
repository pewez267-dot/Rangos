package com.fantasticterraform.client.hud;

import com.fantasticterraform.client.hud.panels.AmbiencePanel;
import com.fantasticterraform.client.hud.panels.BrushesPanel;
import com.fantasticterraform.client.hud.panels.EditingPanel;
import com.fantasticterraform.client.hud.panels.HistoryPanel;
import com.fantasticterraform.client.hud.panels.IntelligentGenerationPanel;
import com.fantasticterraform.client.hud.panels.MasksPanel;
import com.fantasticterraform.client.hud.panels.ParticlesPanel;
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
 * Panel del HUD: un bloque compacto anclado a la IZQUIERDA (pestanas + controles). El
 * area de controles es DESPLAZABLE (rueda del raton o barra) para que ningun control
 * quede fuera de pantalla. No es modal: no pausa el mundo ni lo oscurece por completo.
 */
public class TerraformPanelScreen extends Screen {

    private static final int BOX_X = 2;
    private static final int TAB_W = 64;
    private static final int GAP = 8;
    private static final int CONTENT_W = 178;
    private static final int TOP = 30;
    private static final int FOOTER_H = 22;

    private static int lastTab = 0;

    private final List<HudPanel> panels = new ArrayList<>();
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private int active;
    private int scroll;
    private int contentExtent;

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
        panels.add(new IntelligentGenerationPanel());
        active = Math.min(lastTab, panels.size() - 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.fantasticterraform.client.Keybinds.OPEN_PANELS.matches(keyCode, scanCode)
                && (getFocused() == null || !(getFocused() instanceof EditBox))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        int ty = TOP;
        for (int i = 0; i < panels.size(); i++) {
            final int index = i;
            HudPanel panel = panels.get(i);
            Button tab = Button.builder(Component.literal(panel.title()), b -> selectTab(index))
                    .bounds(BOX_X + 4, ty, TAB_W - 4, 16)
                    .build();
            addRenderableWidget(tab);
            ty += 18;
        }
        rebuildContent();
    }

    private void selectTab(int index) {
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
        panels.get(active).build(this, contentX(), contentY(), contentWidth(), this.height - TOP - 16);
        recomputeExtent();
        reflow();
    }

    private void recomputeExtent() {
        int maxBottom = contentY();
        for (int i = 0; i < contentWidgets.size(); i++) {
            maxBottom = Math.max(maxBottom, baseY.get(i) + contentWidgets.get(i).getHeight());
        }
        contentExtent = maxBottom - contentY();
    }

    private int visibleHeight() {
        return (this.height - 16 - FOOTER_H) - contentY();
    }

    private int maxScroll() {
        return Math.max(0, contentExtent - visibleHeight());
    }

    private void reflow() {
        int top = contentY();
        int bottom = this.height - 16 - FOOTER_H;
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
        return BOX_X + TAB_W + GAP;
    }

    public int contentY() {
        return TOP;
    }

    public int contentWidth() {
        return CONTENT_W - 6;
    }

    // ----- fabricas de widgets (registran su Y base para el scroll) -----

    public <T extends AbstractWidget> T addContent(T widget) {
        contentWidgets.add(widget);
        baseY.add(widget.getY());
        return addRenderableWidget(widget);
    }

    public Button addButton(int x, int y, int w, int h, String label, Runnable action, String tooltip) {
        Button b = Button.builder(Component.literal(label), btn -> action.run()).bounds(x, y, w, h).build();
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
        return s.length() > 16 ? s.substring(0, 15) + "\u2026" : s;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int boxX1 = contentX() + CONTENT_W;
        if (mouseX >= BOX_X && mouseX <= boxX1 && mouseY >= contentY() && mouseY <= this.height - 16) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * 14)));
            reflow();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int boxX1 = contentX() + CONTENT_W;
        int boxTop = 14;
        int boxBottom = this.height - 14;
        g.fill(BOX_X, boxTop, boxX1, boxBottom, 0xF7101018);
        g.fill(BOX_X, boxTop, boxX1, boxTop + 14, 0xFF2B2B3A);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + panels.get(active).title(), BOX_X + 5, boxTop + 4, 0xFFFFFF, false);

        int ty = TOP + active * 18;
        g.fill(BOX_X + 2, ty - 1, BOX_X + TAB_W, ty + 16, 0x553AA0FF);

        super.render(g, mouseX, mouseY, partialTick);

        // Pie fijo con la linea de estado del panel (no se desplaza, no lo tapa el contenido).
        int footerTop = boxBottom - FOOTER_H;
        g.fill(BOX_X, footerTop, boxX1, boxBottom, 0xFF1B1B26);
        String status = panels.get(active).status();
        if (status != null) {
            for (int li = 0; li < 2; li++) {
                int s = li * 34;
                if (s >= status.length()) {
                    break;
                }
                String line = status.substring(s, Math.min(status.length(), s + 34));
                g.drawString(this.font, line, BOX_X + 4, footerTop + 2 + li * 10, 0xC8C8D8, false);
            }
        }

        // Barra de scroll del area de contenido.
        if (maxScroll() > 0) {
            int trackTop = contentY();
            int trackBottom = this.height - 16 - FOOTER_H;
            int trackH = trackBottom - trackTop;
            int knobH = Math.max(16, trackH * visibleHeight() / Math.max(1, contentExtent));
            int knobY = trackTop + (trackH - knobH) * scroll / maxScroll();
            int barX = boxX1 - 3;
            g.fill(barX, trackTop, barX + 2, trackBottom, 0xFF303040);
            g.fill(barX, knobY, barX + 2, knobY + knobH, 0xFF6090FF);
        }
    }

    public void drawLabel(GuiGraphics g, String text, int x, int y) {
        g.drawString(this.font, text, x, y, 0xC8C8D8, false);
    }
}
