package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.CreateAmbienceZonePacket;
import com.fantasticterraform.network.PacketHandler;

/**
 * Panel de Ambiente: crea una zona de sonido usando el bounding box de la seleccion.
 * Configuracion primero, accion principal al final. Layout denso de 14px.
 */
public final class AmbiencePanel implements HudPanel {

    @Override
    public String title() {
        return "Ambiente";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        // --- Sonido principal ---
        screen.section(x, row, "SONIDO PRINCIPAL");
        row += 11;
        screen.addRow(x, row, width, "Sonido", screen.addPicker(x, row, 240, TerraformPanelScreen.RH,
                () -> ClientToolState.ambienceSound, RegistryLists.sounds(), false,
                "Sonido a reproducir (discos de musica y sonidos de mods incluidos).", s -> ClientToolState.ambienceSound = s));
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Volumen", 0, 2, ClientToolState.ambienceVolume, false,
                "Volumen de reproduccion.", v -> ClientToolState.ambienceVolume = v.floatValue());
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Pitch", 0.5, 2, ClientToolState.ambiencePitch, false,
                "Tono/velocidad del sonido.", v -> ClientToolState.ambiencePitch = v.floatValue());
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Fade s", 0, 10, ClientToolState.ambienceFade, false,
                "Segundos de fundido al entrar/salir.", v -> ClientToolState.ambienceFade = v);
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Loop: " + (ClientToolState.ambienceLoop ? "ON" : "OFF"),
                () -> ClientToolState.ambienceLoop = !ClientToolState.ambienceLoop,
                "Si el sonido se repite en bucle mientras estes en la zona.");
        row += TerraformPanelScreen.RS + 2;

        // --- Mezcla (capas opcionales) ---
        screen.section(x, row, "MEZCLA (capas opcionales)");
        row += 11;
        screen.addRow(x, row, half, "Capa 2", screen.addPicker(x, row, half - 55, TerraformPanelScreen.RH,
                () -> blank(ClientToolState.ambienceSound2), RegistryLists.sounds(), false,
                "Segundo sonido a mezclar (vacio = sin capa).", s -> ClientToolState.ambienceSound2 = s));
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Vol 2", 0, 2, ClientToolState.ambienceVolume2, false,
                "Volumen de la capa 2.", v -> ClientToolState.ambienceVolume2 = v.floatValue());
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, half, "Capa 3", screen.addPicker(x, row, half - 55, TerraformPanelScreen.RH,
                () -> blank(ClientToolState.ambienceSound3), RegistryLists.sounds(), false,
                "Tercer sonido a mezclar (vacio = sin capa).", s -> ClientToolState.ambienceSound3 = s));
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Vol 3", 0, 2, ClientToolState.ambienceVolume3, false,
                "Volumen de la capa 3.", v -> ClientToolState.ambienceVolume3 = v.floatValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "\u00a7cQuitar capas 2 y 3", () -> {
            ClientToolState.ambienceSound2 = "";
            ClientToolState.ambienceSound3 = "";
        }, "Deja solo el sonido principal.");
        row += TerraformPanelScreen.RS + 2;

        // --- Accion principal (al final, ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7a\u00a7l\u25b6 CREAR ZONA (usa seleccion)",
                () -> PacketHandler.sendToServer(
                        new CreateAmbienceZonePacket(ClientToolState.ambienceSound, ClientToolState.ambienceVolume,
                                ClientToolState.ambiencePitch, ClientToolState.ambienceLoop, ClientToolState.ambienceFade,
                                ClientToolState.ambienceSound2, ClientToolState.ambienceVolume2,
                                ClientToolState.ambienceSound3, ClientToolState.ambienceVolume3)),
                "Crea una zona de ambiente con el bounding box de la seleccion (mezcla hasta 3 sonidos).");
    }

    private static String blank(String s) {
        return (s == null || s.isEmpty()) ? "(ninguno)" : s;
    }

    @Override
    public String status() {
        return "Selecciona una region, elige sonidos y pulsa CREAR ZONA.";
    }
}
