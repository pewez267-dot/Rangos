package com.fantasticterraform.client.hud;

import com.fantasticterraform.client.hud.panels.AmbiencePanel;
import com.fantasticterraform.client.hud.panels.BrushesPanel;
import com.fantasticterraform.client.hud.panels.EditingPanel;
import com.fantasticterraform.client.hud.panels.HistoryPanel;
import com.fantasticterraform.client.hud.panels.MasksPanel;
import com.fantasticterraform.client.hud.panels.ParticlesPanel;
import com.fantasticterraform.client.hud.panels.SchematicsPanel;
import com.fantasticterraform.client.hud.panels.SelectionPanel;
import com.fantasticterraform.client.hud.panels.TerrainPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla de paneles interactivos del HUD. NO es un AbstractContainerScreen ni una
 * ventana modal: {@link #isPauseScreen()} es false y {@link #renderBackground} no
 * dibuja fondo, asi que el mundo sigue renderizandose (y el wireframe visible) detras
 * de los paneles mientras el OP interactua con ellos.
 */
public class TerraformPanelScreen extends Screen {

    private static final int PANEL_LEFT = 6;
    private static final int PANEL_TOP = 24;
    private static final int TAB_WIDTH = 96;
    private static final int CONTENT_WIDTH = 210;

    private static int lastTab = 0;

    private final List<HudPanel> panels = new ArrayList<>();
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private int active;

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
        active = Math.min(lastTab, panels.size() - 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.fantasticterraform.client.Keybinds.OPEN_PANELS.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        // Pestanas (columna izquierda persistente).
        int ty = PANEL_TOP;
        for (int i = 0; i < panels.size(); i++) {
            final int index = i;
            HudPanel panel = panels.get(i);
            Button tab = Button.builder(Component.literal(panel.title()), b -> selectTab(index))
                    .bounds(PANEL_LEFT, ty, TAB_WIDTH, 18)
                    .build();
            addRenderableWidget(tab);
            ty += 20;
        }
        rebuildContent();
    }

    private void selectTab(int index) {
        active = index;
        lastTab = index;
        rebuildContent();
    }

    private void rebuildContent() {
        for (AbstractWidget w : contentWidgets) {
            removeWidget(w);
        }
        contentWidgets.clear();
        int x = PANEL_LEFT + TAB_WIDTH + 10;
        int y = PANEL_TOP + 14;
        panels.get(active).build(this, x, y, CONTENT_WIDTH, this.height - PANEL_TOP - 30);
    }

    // ----- helpers para los paneles -----

    public int contentX() {
        return PANEL_LEFT + TAB_WIDTH + 10;
    }

    public int contentY() {
        return PANEL_TOP + 14;
    }

    public int contentWidth() {
        return CONTENT_WIDTH;
    }

    public <T extends AbstractWidget> T addContent(T widget) {
        contentWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    public Button addButton(int x, int y, int w, int h, String label, Runnable action) {
        return addContent(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, w, h).build());
    }

    public EditBox addEditBox(int x, int y, int w, int h, String initial, Consumer<String> onChange) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.empty());
        box.setMaxLength(256);
        box.setValue(initial == null ? "" : initial);
        box.setResponder(onChange);
        return addContent(box);
    }

    public SliderWidget addSlider(int x, int y, int w, int h, String label, double min, double max,
                                  double initial, boolean integer, Consumer<Double> onChange) {
        return addContent(new SliderWidget(x, y, w, h, label, min, max, initial, integer, onChange));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Fondo translucido SOLO del rectangulo del panel; el mundo sigue visible alrededor.
        int x0 = PANEL_LEFT - 3;
        int y0 = 4;
        int x1 = PANEL_LEFT + TAB_WIDTH + 10 + CONTENT_WIDTH + 8;
        int y1 = this.height - 4;
        g.fill(x0, y0, x1, y1, 0xC0101018);
        g.fill(x0, y0, x1, y0 + 18, 0xFF2B2B3A);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Terraform \u00a7d\u2726 \u00a77HUD", x0 + 6, y0 + 5, 0xFFFFFF, false);

        // Resaltar pestana activa.
        int ty = PANEL_TOP + active * 20;
        g.fill(PANEL_LEFT - 2, ty - 1, PANEL_LEFT + TAB_WIDTH + 2, ty + 19, 0x553AA0FF);

        super.render(g, mouseX, mouseY, partialTick);

        // Texto/indicadores del panel activo.
        panels.get(active).renderExtra(this, g, contentX(), contentY(), CONTENT_WIDTH, this.height);
    }

    public void drawLabel(GuiGraphics g, String text, int x, int y) {
        g.drawString(this.font, text, x, y, 0xC8C8D8, false);
    }
}
