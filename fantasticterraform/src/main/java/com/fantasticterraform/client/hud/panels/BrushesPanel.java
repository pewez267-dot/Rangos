package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Panel de Brushes: tipo, radio, intensidad, altura y bloque. El brush se aplica con
 * la varita en modo BRUSH haciendo click en el mundo.
 */
public final class BrushesPanel implements HudPanel {

    private static final String[][] BRUSHES = {
            {"sphere", "Esfera", "Coloca el bloque en una esfera del radio dado."},
            {"cylinder", "Cilindro", "Coloca el bloque en un cilindro centrado en el click."},
            {"smooth", "Suavizado", "Suaviza el relieve dentro del radio (usa Intensidad)."},
            {"erode", "Erosion", "Erosiona/desgasta el relieve dentro del radio (usa Intensidad)."},
            {"overlay", "Superficie", "Pinta solo la capa superior expuesta al cielo."},
            {"sphere_clear", "Vaciado", "Vacia (aire) una esfera del radio dado."}
    };

    @Override
    public String title() {
        return "Brushes";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        for (int i = 0; i < BRUSHES.length; i++) {
            final String id = BRUSHES[i][0];
            int col = i % 2;
            int r = i / 2;
            screen.addButton(x + col * (half + 4), y + r * 22, half, 18, BRUSHES[i][1],
                    () -> ClientToolState.brushId = id, BRUSHES[i][2]);
        }
        int row = y + 3 * 22 + 8;
        screen.addSlider(x, row, width, 16, "Radio", 1, 50, ClientToolState.brushRadius, true,
                "Radio del brush (bloques). Debe caber dentro de la seleccion.",
                v -> ClientToolState.brushRadius = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Intensidad", 0, 1, ClientToolState.brushIntensity, false,
                "Fuerza del suavizado/erosion (0 = nada, 1 = maximo).",
                v -> ClientToolState.brushIntensity = v);
        row += 18;
        screen.addSlider(x, row, width, 16, "Altura cil.", 1, 64, ClientToolState.brushHeight, true,
                "Altura del brush Cilindro.", v -> ClientToolState.brushHeight = v.intValue());
        row += 20;
        screen.addPicker(x, row, width, 18, "Bloque", () -> ClientToolState.brushBlock,
                RegistryLists.blocks(), true, "Bloque que colocan los brushes Esfera y Cilindro.",
                s -> ClientToolState.brushBlock = s);
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        int infoY = y + 3 * 22 + 8 + 60;
        screen.drawLabel(g, "Brush activo: \u00a7f" + ClientToolState.brushId, x, infoY);
        screen.drawLabel(g, "Pon la varita en modo BRUSH y haz click.", x, infoY + 11);
    }
}
