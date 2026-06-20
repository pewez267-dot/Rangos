package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.intelligent.biome.BiomeType;
import com.fantasticterraform.network.GenerateBiomeTerrainPacket;
import com.fantasticterraform.network.PacketHandler;

/**
 * Pestaña de Biomas: generar o repintar terreno por clima. Diseño limpio con secciones
 * claras (Estilo, Relieve, Acabado) y una única acción principal destacada.
 */
public final class BiomePanel implements HudPanel {

    private static final String[] STYLES = {"Llano", "Colinas", "Montañas", "Cañón", "Islas", "Meseta", "Dunas", "Volcánico"};

    @Override
    public String title() {
        return "Biomas";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 6) / 2;
        int row = y;

        // ---- Acción principal arriba, destacada ----
        screen.addButton(x, row, width, 20, "\u00a7a\u00a7l\u25b6 GENERAR / APLICAR BIOMA", BiomePanel::generate,
                "Aplica el bioma a tu selección con los ajustes de abajo. Semilla 0 = distinto cada vez.");
        row += 26;

        screen.addHeader(x, row, width, "ESTILO");
        row += 13;
        screen.addButton(x, row, width, 18, "Relieve: \u00a7f" + STYLES[clamp(ClientToolState.biomeStyle, STYLES.length)],
                () -> ClientToolState.biomeStyle = (ClientToolState.biomeStyle + 1) % STYLES.length,
                "Forma del terreno: llano, colinas, montañas, cañón, islas, meseta, dunas o volcánico.");
        row += 20;
        screen.addButton(x, row, width, 18, "Bioma: \u00a7f" + biomeName(),
                () -> ClientToolState.biomeForced = ClientToolState.biomeForced + 1 >= BiomeType.values().length
                        ? -1 : ClientToolState.biomeForced + 1,
                "Auto (multi) mezcla biomas por clima; o fija uno concreto (define su suelo y vegetación).");
        row += 20;
        screen.addButton(x, row, width, 18, "Modo: \u00a7f" + (ClientToolState.biomeMode == 1 ? "Sobrescribir terreno" : "Generar relieve nuevo"),
                () -> ClientToolState.biomeMode = ClientToolState.biomeMode == 1 ? 0 : 1,
                "Generar = crea relieve nuevo y lo funde con los bordes. Sobrescribir = conserva el relieve actual y solo repinta/puebla el bioma.");
        row += 24;

        screen.addHeader(x, row, width, "RELIEVE");
        row += 13;
        screen.addSlider(x, row, half, 16, "Altura", 0, 1, ClientToolState.biomeAmplitude, false,
                "Fuerza del relieve (0 = casi plano, 1 = muy montañoso).", v -> ClientToolState.biomeAmplitude = v);
        screen.addSlider(x + half + 6, row, half, 16, "Mar", 0.05, 0.9, ClientToolState.biomeSea, false,
                "Altura del nivel del mar (fracción de la selección).", v -> ClientToolState.biomeSea = v);
        row += 18;
        screen.addSlider(x, row, width, 16, "Tamaño de formas", 0.001, 0.02, ClientToolState.biomeFeatureScale, false,
                "Menor = montañas/colinas más grandes y separadas.", v -> ClientToolState.biomeFeatureScale = v);
        row += 24;

        screen.addHeader(x, row, width, "ACABADO");
        row += 13;
        screen.addButton(x, row, half, 18, "Ríos: " + onOff(ClientToolState.biomeRivers),
                () -> ClientToolState.biomeRivers = !ClientToolState.biomeRivers,
                "Talla ríos reales con cauce en U, agua y orillas.");
        screen.addButton(x + half + 6, row, half, 18, "Auto-poblar: " + onOff(ClientToolState.biomeAutoPopulate),
                () -> ClientToolState.biomeAutoPopulate = !ClientToolState.biomeAutoPopulate,
                "Al generar, añade vegetación según el bioma.");
        row += 20;
        screen.addButton(x, row, width, 18, "Suelo: \u00a7f" + (ClientToolState.biomeUseCustom ? "Personalizado" : "Automático (clima)"),
                () -> ClientToolState.biomeUseCustom = !ClientToolState.biomeUseCustom,
                "Automático = el suelo lo decide el clima. Personalizado = usa los 3 bloques de abajo.");
        row += 20;
        screen.addPicker(x, row, width, 18, "Superficie", () -> ClientToolState.biomeSurface,
                RegistryLists.blocks(), true, "Bloque de superficie (modo Personalizado).", s -> ClientToolState.biomeSurface = s);
        row += 20;
        screen.addPicker(x, row, half, 18, "Subsuelo", () -> ClientToolState.biomeSub,
                RegistryLists.blocks(), true, "Bloque bajo la superficie.", s -> ClientToolState.biomeSub = s);
        screen.addPicker(x + half + 6, row, half, 18, "Roca", () -> ClientToolState.biomeStone,
                RegistryLists.blocks(), true, "Relleno profundo.", s -> ClientToolState.biomeStone = s);
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
        return b ? "\u00a7aSí" : "\u00a77No";
    }

    @Override
    public String status() {
        return "Selecciona una región y pulsa GENERAR / APLICAR BIOMA.";
    }
}
