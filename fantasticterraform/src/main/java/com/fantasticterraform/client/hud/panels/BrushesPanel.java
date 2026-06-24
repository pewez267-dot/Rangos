package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;

/**
 * Panel de Brushes: 10 pinceles de escultura en rejilla compacta + radio, intensidad,
 * borde, mascara, profundidad, mezcla, altura y bloques. Layout denso (filas de 14px)
 * calcado de FantasticCrates: todo cabe sin scroll.
 */
public final class BrushesPanel implements HudPanel {

    private static final String[] FALLOFFS = {"Duro", "Lineal", "Suave", "Gauss"};
    private static final String[] MASKS = {"Todo", "Solo aire", "Solo solido"};

    private static final String[][] BRUSHES = {
            {"sphere", "Esfera", "Coloca el bloque en una esfera con borde segun el falloff."},
            {"cylinder", "Cilindro", "Coloca el bloque en un cilindro (usa Altura)."},
            {"smooth", "Suavizar", "Suaviza el relieve (kernel gaussiano). Usa Intensidad."},
            {"erode", "Erosion", "Desgasta el relieve dentro del radio. Usa Intensidad."},
            {"overlay", "Superficie", "Pinta la capa superior siguiendo el relieve (usa Prof.)."},
            {"sphere_clear", "Vaciar", "Vacia (aire) una esfera del radio dado."},
            {"noise", "Ruido", "Pinta 2 bloques en parches naturales por ruido (usa Mezcla)."},
            {"blend", "Fundir", "Funde los limites entre materiales (acabado organico)."},
            {"flatten", "Aplanar", "Lleva la superficie a la altura del click (usa Intensidad)."},
            {"melt", "Derretir", "Quita picos y restos flotantes. Usa Intensidad."}
    };

    private static String presetName = "mi_brush";

    @Override
    public String title() {
        return "Brushes";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int third = (width - 8) / 3;
        int per = 4;
        int gap = 4;
        int bw = (width - (per - 1) * gap) / per;
        int row = y;

        // --- Tipos de brush en rejilla compacta (4 por fila) ---
        for (int i = 0; i < BRUSHES.length; i++) {
            final String id = BRUSHES[i][0];
            int col = i % per;
            int r = i / per;
            boolean active = ClientToolState.brushId.equals(id);
            String label = (active ? "\u25b6 " : "") + BRUSHES[i][1];
            screen.addButton(x + col * (bw + gap), row + r * TerraformPanelScreen.RS, bw, TerraformPanelScreen.RH,
                    label, () -> ClientToolState.brushId = id, BRUSHES[i][2]);
        }
        row += ((BRUSHES.length + per - 1) / per) * TerraformPanelScreen.RS + 2;

        // --- Parametros (cada uno en su fila de 14px) ---
        screen.addSlider(x, row, width, TerraformPanelScreen.RH, "Radio", 1, 50, ClientToolState.brushRadius, true,
                "Radio del brush (bloques). Debe caber dentro de la seleccion.",
                v -> ClientToolState.brushRadius = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, width, TerraformPanelScreen.RH, "Intensidad", 0, 1, ClientToolState.brushIntensity, false,
                "Fuerza de Suavizar/Erosion/Aplanar/Derretir (0 = nada, 1 = maximo).",
                v -> ClientToolState.brushIntensity = v);
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Borde: " + FALLOFFS[clamp(ClientToolState.brushFalloff, FALLOFFS.length)],
                () -> ClientToolState.brushFalloff = (ClientToolState.brushFalloff + 1) % FALLOFFS.length,
                "Curva de borde: Duro = corte limpio, Suave/Gauss = bordes difuminados.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Hueco: " + (ClientToolState.brushHollow ? "Si" : "No"),
                () -> ClientToolState.brushHollow = !ClientToolState.brushHollow,
                "Esfera/Cilindro: solo la cascara exterior (hueco por dentro).");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Mascara: " + MASKS[clamp(ClientToolState.brushMaskMode, MASKS.length)],
                () -> ClientToolState.brushMaskMode = (ClientToolState.brushMaskMode + 1) % MASKS.length,
                "Restringe donde actua: Todo, Solo aire o Solo solido.");
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Altura cil.", 1, 64, ClientToolState.brushHeight, true,
                "Altura del brush Cilindro.", v -> ClientToolState.brushHeight = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Mezcla", 0, 1, ClientToolState.brushMix, false,
                "Proporcion del bloque secundario (Esfera/Ruido/Superficie).", v -> ClientToolState.brushMix = v);
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Prof.", 1, 8, ClientToolState.brushDepth, true,
                "Capas hacia abajo en Superficie/Ruido.", v -> ClientToolState.brushDepth = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, width, "Bloque", screen.addPicker(x, row, 170, TerraformPanelScreen.RH,
                () -> ClientToolState.brushBlock, RegistryLists.blocks(), true,
                "Bloque primario (Esfera/Cilindro/Superficie/Ruido).", s -> ClientToolState.brushBlock = s));
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, width, "Bloque 2", screen.addPicker(x, row, 170, TerraformPanelScreen.RH,
                () -> ClientToolState.brushSecondaryBlock, RegistryLists.blocks(), true,
                "Bloque secundario para la Mezcla/Ruido.", s -> ClientToolState.brushSecondaryBlock = s));
        row += TerraformPanelScreen.RS + 2;

        // --- Presets ---
        screen.section(x, row, "PRESETS");
        row += 11;
        screen.addRow(x, row, width, "Nombre", screen.addEditBox(x, row, 200, TerraformPanelScreen.RH, presetName,
                "Nombre del preset a guardar.", s -> presetName = s));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, third, TerraformPanelScreen.RH, "Guardar",
                () -> com.fantasticterraform.client.ClientBrushPresets.save(presetName),
                "Guarda la configuracion actual del brush con el nombre escrito.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "Cargar",
                () -> screen.openPicker("Presets de brush", com.fantasticterraform.client.ClientBrushPresets.names(),
                        presetName, false, name -> {
                            if (com.fantasticterraform.client.ClientBrushPresets.apply(name)) {
                                presetName = name;
                            }
                        }),
                "Elige un preset guardado y aplica toda su configuracion.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "\u00a7cBorrar",
                () -> com.fantasticterraform.client.ClientBrushPresets.delete(presetName),
                "Borra el preset con el nombre escrito.");
    }

    private static int clamp(int v, int len) {
        return (v >= 0 && v < len) ? v : 0;
    }

    @Override
    public String status() {
        return "Brush activo: " + ClientToolState.brushId
                + " | Pon la varita en modo BRUSH (tecla V) y haz click en el mundo.";
    }
}
