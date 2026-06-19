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
 * usando el bounding box de la seleccion. El sonido se elige de una lista.
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
        screen.addButton(x, row, width, 18, "Crear zona (usa seleccion)", () -> PacketHandler.sendToServer(
                        new CreateAmbienceZonePacket(ClientToolState.ambienceSound, ClientToolState.ambienceVolume,
                                ClientToolState.ambiencePitch, ClientToolState.ambienceLoop, ClientToolState.ambienceFade)),
                "Crea una zona de ambiente con el bounding box de la seleccion activa.");
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
