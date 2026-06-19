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
 * Panel del HUD: un unico bloque compacto anclado a la IZQUIERDA (pestanas + controles),
 * con fondo semitransparente para no tapar la vista. No es modal: no pausa el mundo
 * ({@link #isPauseScreen()} = false) ni oscurece toda la pantalla. Cada control lleva
 * un tooltip explicativo.
 */
public class TerraformPanelScreen extends Screen {

    private static final int BOX_X = 2;
    private static final int TAB_W = 64;
    private static final int GAP = 8;
    private static final int CONTENT_W = 172;
    private static final int TOP = 30;

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
        panels.add(new com.fantasticterraform.client.hud.panels.IntelligentGenerationPanel());
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
        int ty = TOP;
        for (int i = 0; i < panels.size(); i++) {
            final int index = i;
            HudPanel panel = panels.get(i);
            Button tab = Button.builder(Component.literal(panel.title()), b -> selectTab(index))
                    .bounds(BOX_X + 4, ty, TAB_W - 4, 17)
                    .build();
            addRenderableWidget(tab);
            ty += 19;
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
        panels.get(active).build(this, contentX(), contentY(), contentWidth(), this.height - TOP - 16);
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

    // ----- fabricas de widgets con tooltip -----

    public <T extends AbstractWidget> T addContent(T widget) {
        contentWidgets.add(widget);
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int boxX1 = contentX() + CONTENT_W;
        int boxTop = 14;
        int boxBottom = this.height - 14;
        // Fondo opaco y compacto (solo lado izquierdo, el resto de la vista queda libre).
        g.fill(BOX_X, boxTop, boxX1, boxBottom, 0xF7101018);
        g.fill(BOX_X, boxTop, boxX1, boxTop + 14, 0xFF2B2B3A);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + panels.get(active).title(), BOX_X + 5, boxTop + 4, 0xFFFFFF, false);

        int ty = TOP + active * 19;
        g.fill(BOX_X + 2, ty - 1, BOX_X + TAB_W, ty + 17, 0x553AA0FF);

        super.render(g, mouseX, mouseY, partialTick);
        panels.get(active).renderExtra(this, g, contentX(), contentY(), contentWidth(), this.height);
    }

    public void drawLabel(GuiGraphics g, String text, int x, int y) {
        g.drawString(this.font, text, x, y, 0xC8C8D8, false);
    }
}
