package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.CreateAmbienceZonePacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Ambiente: crear una zona de sonido usando el bounding box de la seleccion. */
public final class AmbiencePanel implements HudPanel {

    @Override
    public String title() {
        return "Ambiente";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;
        screen.addEditBox(x, row, width, 16, ClientToolState.ambienceSound, s -> ClientToolState.ambienceSound = s);
        row += 20;
        screen.addSlider(x, row, half, 16, "Volumen", 0, 2, ClientToolState.ambienceVolume, false,
                v -> ClientToolState.ambienceVolume = v.floatValue());
        screen.addSlider(x + half + 4, row, half, 16, "Pitch", 0.5, 2, ClientToolState.ambiencePitch, false,
                v -> ClientToolState.ambiencePitch = v.floatValue());
        row += 18;
        screen.addSlider(x, row, half, 16, "Fade s", 0, 10, ClientToolState.ambienceFade, false,
                v -> ClientToolState.ambienceFade = v);
        screen.addButton(x + half + 4, row, half, 18, "Loop: " + (ClientToolState.ambienceLoop ? "ON" : "OFF"),
                () -> ClientToolState.ambienceLoop = !ClientToolState.ambienceLoop);
        row += 22;
        screen.addButton(x, row, width, 18, "Crear zona (usa seleccion)", () -> PacketHandler.sendToServer(
                new CreateAmbienceZonePacket(ClientToolState.ambienceSound, ClientToolState.ambienceVolume,
                        ClientToolState.ambiencePitch, ClientToolState.ambienceLoop, ClientToolState.ambienceFade)));
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Sonido: ej. minecraft:ambient.cave o uno de resourcepack.", x, y - 12);
    }
}
