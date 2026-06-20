package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Panel de Brushes: 10 pinceles de escultura con curva de borde (falloff), bloque
 * secundario, mezcla, profundidad y modo hueco. El brush se aplica con la varita en
 * modo BRUSH haciendo click en el mundo.
 */
public final class BrushesPanel implements HudPanel {

    private static final String[] FALLOFFS = {"Duro", "Lineal", "Suave", "Gaussiano"};
    private static final String[] MASKS = {"Todo", "Solo aire", "Solo sólido"};

    private static final String[][] BRUSHES = {
            {"sphere", "Esfera", "Coloca el bloque en una esfera con borde según el falloff."},
            {"cylinder", "Cilindro", "Coloca el bloque en un cilindro (usa Altura)."},
            {"smooth", "Suavizar", "Suaviza el relieve (kernel gaussiano). Usa Intensidad."},
            {"erode", "Erosion", "Desgasta el relieve dentro del radio. Usa Intensidad."},
            {"overlay", "Superficie", "Pinta la capa superior siguiendo el relieve (usa Prof.)."},
            {"sphere_clear", "Vaciar", "Vacia (aire) una esfera del radio dado."},
            {"noise", "Ruido", "Pinta 2 bloques en parches naturales por ruido (usa Mezcla)."},
            {"blend", "Fundir", "Funde los limites entre materiales (acabado orgánico)."},
            {"flatten", "Aplanar", "Lleva la superficie a la altura del click (usa Intensidad)."},
            {"melt", "Derretir", "Quita picos y restos flotantes. Usa Intensidad."}
    };

    @Override
    public String title() {
        return "Brushes";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int rows = (BRUSHES.length + 1) / 2;
        for (int i = 0; i < BRUSHES.length; i++) {
            final String id = BRUSHES[i][0];
            int col = i % 2;
            int r = i / 2;
            boolean active = ClientToolState.brushId.equals(id);
            String label = (active ? "\u00a7a\u25b6 " : "") + BRUSHES[i][1];
            screen.addButton(x + col * (half + 4), y + r * 20, half, 18, label,
                    () -> ClientToolState.brushId = id, BRUSHES[i][2]);
        }
        int row = y + rows * 20 + 6;

        screen.addSlider(x, row, width, 16, "Radio", 1, 50, ClientToolState.brushRadius, true,
                "Radio del brush (bloques). Debe caber dentro de la selección.",
                v -> ClientToolState.brushRadius = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Intensidad", 0, 1, ClientToolState.brushIntensity, false,
                "Fuerza de Suavizar/Erosion/Aplanar/Derretir (0 = nada, 1 = máximo).",
                v -> ClientToolState.brushIntensity = v);
        row += 18;
        screen.addButton(x, row, half, 18, "Borde: " + FALLOFFS[clamp(ClientToolState.brushFalloff, FALLOFFS.length)],
                () -> ClientToolState.brushFalloff = (ClientToolState.brushFalloff + 1) % FALLOFFS.length,
                "Curva de borde del brush: Duro = corte limpio, Suave/Gaussiano = bordes difuminados naturales.");
        screen.addButton(x + half + 4, row, half, 18, "Hueco: " + (ClientToolState.brushHollow ? "\u00a7aSI" : "\u00a77NO"),
                () -> ClientToolState.brushHollow = !ClientToolState.brushHollow,
                "Esfera/Cilindro: solo la cáscara exterior (hueco por dentro).");
        row += 20;
        screen.addButton(x, row, width, 18, "Máscara: " + MASKS[clamp(ClientToolState.brushMaskMode, MASKS.length)],
                () -> ClientToolState.brushMaskMode = (ClientToolState.brushMaskMode + 1) % MASKS.length,
                "Restringe dónde actúa el brush: Todo, Solo aire (construir sin tapar bloques) o Solo sólido (esculpir lo existente).");
        row += 20;
        screen.addSlider(x, row, half, 16, "Mezcla", 0, 1, ClientToolState.brushMix, false,
                "Proporcion del bloque secundario (0 = solo primario). Para Esfera/Ruido/Superficie.",
                v -> ClientToolState.brushMix = v);
        screen.addSlider(x + half + 4, row, half, 16, "Prof.", 1, 8, ClientToolState.brushDepth, true,
                "Capas hacia abajo en Superficie/Ruido.", v -> ClientToolState.brushDepth = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Altura cil.", 1, 64, ClientToolState.brushHeight, true,
                "Altura del brush Cilindro.", v -> ClientToolState.brushHeight = v.intValue());
        row += 20;
        screen.addPicker(x, row, width, 18, "Bloque", () -> ClientToolState.brushBlock,
                RegistryLists.blocks(), true, "Bloque primario (Esfera/Cilindro/Superficie/Ruido).",
                s -> ClientToolState.brushBlock = s);
        row += 20;
        screen.addPicker(x, row, width, 18, "Bloque 2", () -> ClientToolState.brushSecondaryBlock,
                RegistryLists.blocks(), true, "Bloque secundario para la Mezcla/Ruido.",
                s -> ClientToolState.brushSecondaryBlock = s);
        row += 22;

        // --- Presets de brush (guardables) ---
        screen.addEditBox(x, row, width, 16, presetName,
                "Nombre del preset a guardar.", s -> presetName = s);
        row += 20;
        int third = (width - 8) / 3;
        screen.addButton(x, row, third, 18, "Guardar", () -> {
            com.fantasticterraform.client.ClientBrushPresets.save(presetName);
        }, "Guarda la configuracion actual del brush con el nombre escrito.");
        screen.addButton(x + third + 4, row, third, 18, "Cargar", () ->
                        screen.openPicker("Presets de brush", com.fantasticterraform.client.ClientBrushPresets.names(),
                                presetName, false, name -> {
                                    if (com.fantasticterraform.client.ClientBrushPresets.apply(name)) {
                                        presetName = name;
                                    }
                                }),
                "Elige un preset guardado y aplica toda su configuracion.");
        screen.addButton(x + 2 * (third + 4), row, third, 18, "Borrar", () ->
                        com.fantasticterraform.client.ClientBrushPresets.delete(presetName),
                "Borra el preset con el nombre escrito.");
    }

    private static String presetName = "mi_brush";

    private static int clamp(int v, int len) {
        return (v >= 0 && v < len) ? v : 0;
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }

    @Override
    public String status() {
        return "Brush: \u00a7f" + ClientToolState.brushId + "\u00a77 | Borde: \u00a7f"
                + FALLOFFS[clamp(ClientToolState.brushFalloff, FALLOFFS.length)]
                + "\u00a77 | Pon la varita en modo BRUSH y haz click.";
    }
}
