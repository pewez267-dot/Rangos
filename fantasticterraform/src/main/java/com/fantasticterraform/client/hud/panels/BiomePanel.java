package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.intelligent.biome.BiomeType;
import com.fantasticterraform.network.GenerateBiomeTerrainPacket;
import com.fantasticterraform.network.PacketHandler;

/**
 * Pestana de Biomas: generar o repintar terreno por clima. Primero los controles de
 * configuracion, al final la accion que los ejecuta. Layout denso de 14px.
 */
public final class BiomePanel implements HudPanel {

    private static final String[] STYLES = {"Llano", "Colinas", "Montanas", "Canon", "Islas", "Meseta", "Dunas", "Volcanico"};

    @Override
    public String title() {
        return "Biomas";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        // --- Estilo ---
        screen.section(x, row, "ESTILO");
        row += 11;
        screen.addRow(x, row, half, "Relieve", screen.addButton(x, row, half - 50, TerraformPanelScreen.RH,
                STYLES[clamp(ClientToolState.biomeStyle, STYLES.length)],
                () -> ClientToolState.biomeStyle = (ClientToolState.biomeStyle + 1) % STYLES.length,
                "Forma del terreno: llano, colinas, montanas, canon, islas, meseta, dunas o volcanico."));
        screen.addRow(x + half + 4, row, half, "Bioma", screen.addButton(x + half + 4, row, half - 50, TerraformPanelScreen.RH,
                biomeName(),
                () -> ClientToolState.biomeForced = ClientToolState.biomeForced + 1 >= BiomeType.values().length
                        ? -1 : ClientToolState.biomeForced + 1,
                "Auto (multi) mezcla biomas por clima; o fija uno concreto."));
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, width, "Modo", screen.addButton(x, row, 220, TerraformPanelScreen.RH,
                ClientToolState.biomeMode == 1 ? "Sobrescribir terreno" : "Generar relieve nuevo",
                () -> ClientToolState.biomeMode = ClientToolState.biomeMode == 1 ? 0 : 1,
                "Generar = crea relieve nuevo. Sobrescribir = conserva el relieve y solo repinta/puebla."));
        row += TerraformPanelScreen.RS + 2;

        // --- Relieve ---
        screen.section(x, row, "RELIEVE");
        row += 11;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Altura", 0, 1, ClientToolState.biomeAmplitude, false,
                "Fuerza del relieve (0 = casi plano, 1 = muy montanoso).", v -> ClientToolState.biomeAmplitude = v);
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Mar", 0.05, 0.9, ClientToolState.biomeSea, false,
                "Altura del nivel del mar (fraccion de la seleccion).", v -> ClientToolState.biomeSea = v);
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, width, TerraformPanelScreen.RH, "Tamano de formas", 0.001, 0.02, ClientToolState.biomeFeatureScale, false,
                "Menor = montanas/colinas mas grandes y separadas.", v -> ClientToolState.biomeFeatureScale = v);
        row += TerraformPanelScreen.RS + 2;

        // --- Acabado ---
        screen.section(x, row, "ACABADO");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Rios: " + onOff(ClientToolState.biomeRivers),
                () -> ClientToolState.biomeRivers = !ClientToolState.biomeRivers,
                "Talla rios reales con cauce en U, agua y orillas.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Auto-poblar: " + onOff(ClientToolState.biomeAutoPopulate),
                () -> ClientToolState.biomeAutoPopulate = !ClientToolState.biomeAutoPopulate,
                "Al generar, anade vegetacion segun el bioma.");
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, width, "Suelo", screen.addButton(x, row, 220, TerraformPanelScreen.RH,
                ClientToolState.biomeUseCustom ? "Personalizado" : "Automatico (clima)",
                () -> ClientToolState.biomeUseCustom = !ClientToolState.biomeUseCustom,
                "Automatico = el suelo lo decide el clima. Personalizado = usa los 3 bloques de abajo."));
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, width, "Superficie", screen.addPicker(x, row, 200, TerraformPanelScreen.RH,
                () -> ClientToolState.biomeSurface, RegistryLists.blocks(), true,
                "Bloque de superficie (modo Personalizado).", s -> ClientToolState.biomeSurface = s));
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, half, "Subsuelo", screen.addPicker(x, row, half - 60, TerraformPanelScreen.RH,
                () -> ClientToolState.biomeSub, RegistryLists.blocks(), true,
                "Bloque bajo la superficie.", s -> ClientToolState.biomeSub = s));
        screen.addRow(x + half + 4, row, half, "Roca", screen.addPicker(x + half + 4, row, half - 60, TerraformPanelScreen.RH,
                () -> ClientToolState.biomeStone, RegistryLists.blocks(), true,
                "Relleno profundo.", s -> ClientToolState.biomeStone = s));
        row += TerraformPanelScreen.RS + 2;

        // --- Accion principal (al final, ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7a\u00a7l\u25b6 GENERAR / APLICAR BIOMA", BiomePanel::generate,
                "Aplica el bioma a tu seleccion con los ajustes de arriba.");
    }

    private static void generate() {
        PacketHandler.sendToServer(new GenerateBiomeTerrainPacket(
                ClientToolState.biomeStyle, ClientToolState.biomeFeatureScale, ClientToolState.biomeAmplitude,
                ClientToolState.biomeSea, ClientToolState.biomeUseCustom, ClientToolState.biomeSurface,
                ClientToolState.biomeSub, ClientToolState.biomeStone, ClientToolState.genSeed,
                ClientToolState.biomeForced, ClientToolState.biomeAutoPopulate, ClientToolState.biomeRivers,
                ClientToolState.biomeMode));
    }

    private static String biomeName() {
        if (ClientToolState.biomeForced < 0) {
            return "Auto (multi)";
        }
        BiomeType[] v = BiomeType.values();
        int i = ClientToolState.biomeForced;
        return i < v.length ? v[i].displayName() : "Auto (multi)";
    }

    private static int clamp(int v, int len) {
        return (v >= 0 && v < len) ? v : 0;
    }

    private static String onOff(boolean b) {
        return b ? "Si" : "No";
    }

    @Override
    public String status() {
        return "Selecciona una region, configura y pulsa GENERAR / APLICAR BIOMA.";
    }
}
