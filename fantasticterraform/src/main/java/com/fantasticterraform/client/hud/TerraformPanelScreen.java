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
 * Pantalla de paneles del HUD. NO es modal: {@link #isPauseScreen()} es false y no
 * oscurece el mundo. Distribucion: pestanas en el borde IZQUIERDO y controles en el
 * borde DERECHO, dejando el centro de la pantalla libre. Fondos opacos para no
 * estorbar la vision. Cada control lleva un tooltip explicativo.
 */
public class TerraformPanelScreen extends Screen {

    private static final int LEFT_W = 100;
    private static final int CONTENT_W = 212;
    private static final int TAB_TOP = 30;

    private static int lastTab = 0;

    private final List<HudPanel> panels = new ArrayList<>();
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private int active;
    private int rightX;

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
        rightX = this.width - CONTENT_W - 8;
        int ty = TAB_TOP;
        for (int i = 0; i < panels.size(); i++) {
            final int index = i;
            HudPanel panel = panels.get(i);
            Button tab = Button.builder(Component.literal(panel.title()), b -> selectTab(index))
                    .bounds(6, ty, LEFT_W - 12, 18)
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
        panels.get(active).build(this, contentX(), contentY(), CONTENT_W - 8, this.height - TAB_TOP - 16);
    }

    // ----- geometria del area de contenido (borde derecho) -----

    public int contentX() {
        return rightX + 6;
    }

    public int contentY() {
        return TAB_TOP + 4;
    }

    public int contentWidth() {
        return CONTENT_W - 12;
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

    /** Boton que abre un menu desplegable para elegir un valor de una lista. */
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Franja izquierda (pestanas) opaca.
        g.fill(0, 4, LEFT_W, this.height - 4, 0xFF161620);
        g.fill(0, 4, LEFT_W, 22, 0xFF2B2B3A);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fTerraform", 6, 9, 0xFFFFFF, false);

        int ty = TAB_TOP + active * 20;
        g.fill(2, ty - 1, LEFT_W - 2, ty + 19, 0x553AA0FF);

        // Franja derecha (contenido) opaca, centro libre.
        g.fill(rightX, 4, this.width - 2, this.height - 4, 0xFF161620);
        g.fill(rightX, 4, this.width - 2, 22, 0xFF2B2B3A);
        g.drawString(this.font, "\u00a7f" + panels.get(active).title(), rightX + 6, 9, 0xFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick);
        panels.get(active).renderExtra(this, g, contentX(), contentY(), contentWidth(), this.height);
    }

    public void drawLabel(GuiGraphics g, String text, int x, int y) {
        g.drawString(this.font, text, x, y, 0xC8C8D8, false);
    }
}
