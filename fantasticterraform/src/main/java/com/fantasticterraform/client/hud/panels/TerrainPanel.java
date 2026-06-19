package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.TerrainOperationPacket;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Terreno: suavizado, deformacion, naturalizacion, cuevas, montanas y erosion. */
public final class TerrainPanel implements HudPanel {

    private static final String[] CURVES = {"Lineal", "Suave", "Ruido"};

    @Override
    public String title() {
        return "Terreno";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addEditBox(x, row, half, 16, String.valueOf(ClientToolState.seed),
                "Semilla del ruido (numero). Misma semilla = mismo resultado.", s -> {
                    try {
                        ClientToolState.seed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.seed = s.hashCode();
                    }
                });
        screen.addButton(x + half + 4, row, half, 18, "Suavizar", TerrainPanel::sendSmooth,
                "Promedia las alturas de la superficie para suavizar el relieve.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Kernel", 1, 2, ClientToolState.smoothKernel, true,
                "Tamano del area de promedio: 1 = 3x3, 2 = 5x5.", v -> ClientToolState.smoothKernel = v.intValue());
        screen.addSlider(x + half + 4, row, half, 16, "Pasadas", 1, 10, ClientToolState.smoothPasses, true,
                "Numero de repeticiones del suavizado.", v -> ClientToolState.smoothPasses = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Intensidad", 0, 1, ClientToolState.smoothIntensity, false,
                "Cuanto se acerca cada columna al promedio (0..1).", v -> ClientToolState.smoothIntensity = v);
        row += 20;

        screen.addSlider(x, row, half, 16, "Amp. def.", 1, 64, ClientToolState.deformAmplitude, false,
                "Amplitud del desplazamiento vertical de la deformacion.", v -> ClientToolState.deformAmplitude = v);
        screen.addButton(x + half + 4, row, half, 18, "Deformar (" + CURVES[ClientToolState.deformCurve] + ")", () -> {
            ClientToolState.deformCurve = (ClientToolState.deformCurve + 1) % 3;
            sendDeform();
        }, "Desplaza la superficie segun una curva. El boton alterna Lineal/Suave/Ruido y aplica.");
        row += 22;

        screen.addPicker(x, row, width, 18, "Superficie", () -> ClientToolState.surfaceBlock,
                RegistryLists.blocks(), true, "Bloque de la capa superior (naturalizar/montanas).",
                s -> ClientToolState.surfaceBlock = s);
        row += 20;
        screen.addPicker(x, row, half, 18, "Tierra", () -> ClientToolState.dirtBlock,
                RegistryLists.blocks(), true, "Bloque de las capas intermedias.", s -> ClientToolState.dirtBlock = s);
        screen.addPicker(x + half + 4, row, half, 18, "Piedra", () -> ClientToolState.stoneBlock,
                RegistryLists.blocks(), true, "Bloque del relleno profundo.", s -> ClientToolState.stoneBlock = s);
        row += 20;
        screen.addSlider(x, row, half, 16, "Capas tierra", 0, 8, ClientToolState.naturalizeLayers, true,
                "Cuantas capas de tierra bajo la superficie.", v -> ClientToolState.naturalizeLayers = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Naturalizar", TerrainPanel::sendNaturalize,
                "Re-texturiza la superficie: cesped/tierra/piedra.");
        row += 22;

        screen.addSlider(x, row, half, 16, "Umbral cueva", -1, 1, ClientToolState.caveThreshold, false,
                "Umbral del ruido 3D. Mas alto = menos cuevas.", v -> ClientToolState.caveThreshold = v);
        screen.addButton(x + half + 4, row, half, 18, "Cuevas", TerrainPanel::sendCave,
                "Talla cuevas con ruido 3D dentro del solido de la seleccion.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Amp. montana", 1, 96, ClientToolState.mountainAmplitude, false,
                "Altura maxima de las montanas generadas.", v -> ClientToolState.mountainAmplitude = v);
        screen.addButton(x + half + 4, row, half, 18, "Montanas", TerrainPanel::sendMountain,
                "Genera montanas con ruido 2D desde la base de la seleccion.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Pasadas eros.", 1, 10, ClientToolState.erosionPasses, true,
                "Iteraciones de erosion termica.", v -> ClientToolState.erosionPasses = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Erosionar", TerrainPanel::sendErosion,
                "Mueve material de zonas altas a bajas (erosion termica).");
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
    }
}
