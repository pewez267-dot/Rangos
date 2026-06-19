package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.TerrainOperationPacket;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Terreno: suavizado, deformacion, naturalizacion, cuevas, montanas y erosion. */
public final class TerrainPanel implements HudPanel {

    @Override
    public String title() {
        return "Terreno";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addEditBox(x, row, half, 16, String.valueOf(ClientToolState.seed), s -> {
            try {
                ClientToolState.seed = Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                ClientToolState.seed = s.hashCode();
            }
        });
        screen.addButton(x + half + 4, row, half, 18, "Suavizar", TerrainPanel::sendSmooth);
        row += 20;

        screen.addSlider(x, row, half, 16, "Kernel", 1, 2, ClientToolState.smoothKernel, true,
                v -> ClientToolState.smoothKernel = v.intValue());
        screen.addSlider(x + half + 4, row, half, 16, "Pasadas", 1, 10, ClientToolState.smoothPasses, true,
                v -> ClientToolState.smoothPasses = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Intensidad", 0, 1, ClientToolState.smoothIntensity, false,
                v -> ClientToolState.smoothIntensity = v);
        row += 20;

        screen.addSlider(x, row, half, 16, "Amplitud def.", 1, 64, ClientToolState.deformAmplitude, false,
                v -> ClientToolState.deformAmplitude = v);
        screen.addButton(x + half + 4, row, half, 18, "Deformar (curva " + ClientToolState.deformCurve + ")", () -> {
            ClientToolState.deformCurve = (ClientToolState.deformCurve + 1) % 3;
            sendDeform();
        });
        row += 20;

        screen.addEditBox(x, row, width, 16, ClientToolState.surfaceBlock, s -> ClientToolState.surfaceBlock = s);
        row += 18;
        screen.addEditBox(x, row, half, 16, ClientToolState.dirtBlock, s -> ClientToolState.dirtBlock = s);
        screen.addEditBox(x + half + 4, row, half, 16, ClientToolState.stoneBlock, s -> ClientToolState.stoneBlock = s);
        row += 18;
        screen.addSlider(x, row, half, 16, "Capas tierra", 0, 8, ClientToolState.naturalizeLayers, true,
                v -> ClientToolState.naturalizeLayers = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Naturalizar", TerrainPanel::sendNaturalize);
        row += 20;

        screen.addSlider(x, row, half, 16, "Umbral cueva", -1, 1, ClientToolState.caveThreshold, false,
                v -> ClientToolState.caveThreshold = v);
        screen.addButton(x + half + 4, row, half, 18, "Cuevas", TerrainPanel::sendCave);
        row += 20;

        screen.addSlider(x, row, half, 16, "Amp. montana", 1, 96, ClientToolState.mountainAmplitude, false,
                v -> ClientToolState.mountainAmplitude = v);
        screen.addButton(x + half + 4, row, half, 18, "Montanas", TerrainPanel::sendMountain);
        row += 20;

        screen.addSlider(x, row, half, 16, "Pasadas eros.", 1, 10, ClientToolState.erosionPasses, true,
                v -> ClientToolState.erosionPasses = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Erosionar", TerrainPanel::sendErosion);
    }

    private static void sendSmooth() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.SMOOTH,
                ClientToolState.smoothKernel, ClientToolState.smoothPasses, 0,
                ClientToolState.smoothIntensity, 0, ClientToolState.seed, "", "", ""));
    }

    private static void sendDeform() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.DEFORM,
                ClientToolState.deformCurve, 0, 0, ClientToolState.deformAmplitude, 0, ClientToolState.seed, "", "", ""));
    }

    private static void sendNaturalize() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.NATURALIZE,
                ClientToolState.naturalizeLayers, 0, 0, 0, 0, ClientToolState.seed,
                ClientToolState.surfaceBlock, ClientToolState.dirtBlock, ClientToolState.stoneBlock));
    }

    private static void sendCave() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.CAVE,
                0, 0, 0, ClientToolState.caveThreshold, ClientToolState.caveScale, ClientToolState.seed, "", "", ""));
    }

    private static void sendMountain() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.MOUNTAIN,
                ClientToolState.mountainOctaves, 0, 0, ClientToolState.mountainAmplitude,
                ClientToolState.mountainFrequency, ClientToolState.seed,
                ClientToolState.surfaceBlock, ClientToolState.dirtBlock, ClientToolState.stoneBlock));
    }

    private static void sendErosion() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.EROSION,
                ClientToolState.erosionPasses, 0, 0, ClientToolState.erosionTalus, ClientToolState.erosionFactor,
                ClientToolState.seed, "", "", ""));
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Semilla a la izquierda. Bloques: superficie / tierra / piedra.", x, y - 12);
    }
}
