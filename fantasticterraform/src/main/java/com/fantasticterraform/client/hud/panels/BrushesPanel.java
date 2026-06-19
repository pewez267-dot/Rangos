package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Panel de Brushes: elegir tipo, radio, intensidad, altura y bloque. El brush se
 * aplica con la varita (en modo BRUSH) haciendo click en el mundo.
 */
public final class BrushesPanel implements HudPanel {

    private static final String[][] BRUSHES = {
            {"sphere", "Esfera"},
            {"cylinder", "Cilindro"},
            {"smooth", "Suavizado"},
            {"erode", "Erosion"},
            {"overlay", "Superficie"},
            {"sphere_clear", "Vaciado"}
    };

    @Override
    public String title() {
        return "Brushes";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;
        for (int i = 0; i < BRUSHES.length; i++) {
            final String id = BRUSHES[i][0];
            int col = i % 2;
            int r = i / 2;
            screen.addButton(x + col * (half + 4), y + r * 22, half, 18, BRUSHES[i][1],
                    () -> ClientToolState.brushId = id);
        }
        row = y + 3 * 22 + 6;
        screen.addSlider(x, row, width, 16, "Radio", 1, 50, ClientToolState.brushRadius, true,
                v -> ClientToolState.brushRadius = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Intensidad", 0, 1, ClientToolState.brushIntensity, false,
                v -> ClientToolState.brushIntensity = v);
        row += 18;
        screen.addSlider(x, row, width, 16, "Altura cil.", 1, 64, ClientToolState.brushHeight, true,
                v -> ClientToolState.brushHeight = v.intValue());
        row += 18;
        screen.addEditBox(x, row, width, 16, ClientToolState.brushBlock, s -> ClientToolState.brushBlock = s);
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        int infoY = y + 3 * 22 + 6 + 72;
        screen.drawLabel(g, "Brush activo: \u00a7f" + ClientToolState.brushId, x, infoY);
        screen.drawLabel(g, "Modo varita debe ser BRUSH (pestana Seleccion).", x, infoY + 11);
    }
}
