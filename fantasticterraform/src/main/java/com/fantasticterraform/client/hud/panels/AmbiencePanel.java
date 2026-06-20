package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.CreateAmbienceZonePacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Panel de Ambiente: crea una zona de sonido (musica de discos o sonidos de mods)
 * usando el bounding box de la selección. El sonido se elige de una lista.
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
        screen.addPicker(x, row, width, 18, "Sonido", () -> ClientToolState.ambienceSound,
                RegistryLists.sounds(), false,
                "Sonido a reproducir. Incluye discos de musica y sonidos de mods instalados.",
                s -> ClientToolState.ambienceSound = s);
        row += 22;
        screen.addSlider(x, row, half, 16, "Volumen", 0, 2, ClientToolState.ambienceVolume, false,
                "Volumen de reproduccion.", v -> ClientToolState.ambienceVolume = v.floatValue());
        screen.addSlider(x + half + 4, row, half, 16, "Pitch", 0.5, 2, ClientToolState.ambiencePitch, false,
                "Tono/velocidad del sonido.", v -> ClientToolState.ambiencePitch = v.floatValue());
        row += 18;
        screen.addSlider(x, row, half, 16, "Fade s", 0, 10, ClientToolState.ambienceFade, false,
                "Segundos de fundido al entrar/salir.", v -> ClientToolState.ambienceFade = v);
        screen.addButton(x + half + 4, row, half, 18, "Loop: " + (ClientToolState.ambienceLoop ? "ON" : "OFF"),
                () -> ClientToolState.ambienceLoop = !ClientToolState.ambienceLoop,
                "Si el sonido se repite en bucle mientras estes en la zona.");
        row += 22;

        // --- Mezcla: capas de sonido adicionales ---
        screen.addPicker(x, row, half, 18, "Capa 2", () -> blank(ClientToolState.ambienceSound2),
                RegistryLists.sounds(), false, "Segundo sonido a mezclar (vacio = sin capa).",
                s -> ClientToolState.ambienceSound2 = s);
        screen.addSlider(x + half + 4, row, half, 16, "Vol 2", 0, 2, ClientToolState.ambienceVolume2, false,
                "Volumen de la capa 2.", v -> ClientToolState.ambienceVolume2 = v.floatValue());
        row += 20;
        screen.addPicker(x, row, half, 18, "Capa 3", () -> blank(ClientToolState.ambienceSound3),
                RegistryLists.sounds(), false, "Tercer sonido a mezclar (vacio = sin capa).",
                s -> ClientToolState.ambienceSound3 = s);
        screen.addSlider(x + half + 4, row, half, 16, "Vol 3", 0, 2, ClientToolState.ambienceVolume3, false,
                "Volumen de la capa 3.", v -> ClientToolState.ambienceVolume3 = v.floatValue());
        row += 20;
        screen.addButton(x, row, half, 18, "Quitar capas", () -> {
            ClientToolState.ambienceSound2 = "";
            ClientToolState.ambienceSound3 = "";
        }, "Vacia las capas 2 y 3 (deja solo el sonido principal).");
        row += 22;

        screen.addButton(x, row, width, 18, "Crear zona (usa selección)", () -> PacketHandler.sendToServer(
                        new CreateAmbienceZonePacket(ClientToolState.ambienceSound, ClientToolState.ambienceVolume,
                                ClientToolState.ambiencePitch, ClientToolState.ambienceLoop, ClientToolState.ambienceFade,
                                ClientToolState.ambienceSound2, ClientToolState.ambienceVolume2,
                                ClientToolState.ambienceSound3, ClientToolState.ambienceVolume3)),
                "Crea una zona de ambiente con el bounding box de la selección activa (mezcla hasta 3 sonidos).");
    }

    private static String blank(String s) {
        return (s == null || s.isEmpty()) ? "(ninguno)" : s;
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
