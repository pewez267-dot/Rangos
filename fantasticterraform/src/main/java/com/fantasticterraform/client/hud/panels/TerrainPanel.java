package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.TerrainOperationPacket;

/** Panel de Terreno: suavizado, deformacion, naturalizacion, generacion y erosion. Layout denso 14px. */
public final class TerrainPanel implements HudPanel {

    private static final String[] CURVES = {"Lineal", "Suave", "Ruido"};
    private static final String[] MOUNTAIN_MODES = {"Colinas", "Crestas", "Lomas"};

    @Override
    public String title() {
        return "Terreno";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        // --- Suavizado ---
        screen.section(x, row, "SUAVIZADO");
        row += 11;
        screen.addRow(x, row, width, "Semilla", screen.addEditBox(x, row, 200, TerraformPanelScreen.RH, String.valueOf(ClientToolState.seed),
                "Semilla del ruido (numero). Misma semilla = mismo resultado.", s -> {
                    try {
                        ClientToolState.seed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.seed = s.hashCode();
                    }
                }));
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Area", 1, 2, ClientToolState.smoothKernel, true,
                "Tamano del area que se promedia: 1 = 3x3, 2 = 5x5.", v -> ClientToolState.smoothKernel = v.intValue());
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Veces", 1, 10, ClientToolState.smoothPasses, true,
                "Cuantas veces se repite el suavizado.", v -> ClientToolState.smoothPasses = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Fuerza", 0, 1, ClientToolState.smoothIntensity, false,
                "Que tan fuerte suaviza en cada pasada.", v -> ClientToolState.smoothIntensity = v);
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aSuavizar", TerrainPanel::sendSmooth,
                "Promedia las alturas de la superficie para suavizar el relieve.");
        row += TerraformPanelScreen.RS + 2;

        // --- Elevar / deformar ---
        screen.section(x, row, "ELEVAR / DEFORMAR");
        row += 11;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Altura", 1, 64, ClientToolState.deformAmplitude, false,
                "Cuanto sube o baja el terreno.", v -> ClientToolState.deformAmplitude = v);
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aElevar (" + CURVES[ClientToolState.deformCurve] + ")", () -> {
            ClientToolState.deformCurve = (ClientToolState.deformCurve + 1) % 3;
            sendDeform();
        }, "Sube/baja la superficie. El boton alterna la forma (Lineal/Suave/Ruido) y aplica.");
        row += TerraformPanelScreen.RS + 2;

        // --- Materiales / naturalizar ---
        screen.section(x, row, "MATERIALES / NATURALIZAR");
        row += 11;
        screen.addRow(x, row, width, "Superficie", screen.addPicker(x, row, 200, TerraformPanelScreen.RH,
                () -> ClientToolState.surfaceBlock, RegistryLists.blocks(), true,
                "Bloque de la capa superior.", s -> ClientToolState.surfaceBlock = s));
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, half, "Tierra", screen.addPicker(x, row, half - 50, TerraformPanelScreen.RH,
                () -> ClientToolState.dirtBlock, RegistryLists.blocks(), true,
                "Bloque de las capas intermedias.", s -> ClientToolState.dirtBlock = s));
        screen.addRow(x + half + 4, row, half, "Piedra", screen.addPicker(x + half + 4, row, half - 50, TerraformPanelScreen.RH,
                () -> ClientToolState.stoneBlock, RegistryLists.blocks(), true,
                "Bloque del relleno profundo.", s -> ClientToolState.stoneBlock = s));
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Capas tierra", 0, 8, ClientToolState.naturalizeLayers, true,
                "Cuantas capas de tierra bajo la superficie.", v -> ClientToolState.naturalizeLayers = v.intValue());
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aNaturalizar", TerrainPanel::sendNaturalize,
                "Re-texturiza la superficie: cesped/tierra/piedra.");
        row += TerraformPanelScreen.RS + 2;

        // --- Generar ---
        screen.section(x, row, "GENERAR");
        row += 11;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Cuevas", -1, 1, ClientToolState.caveThreshold, false,
                "Mas a la IZQUIERDA = mas huecos.", v -> ClientToolState.caveThreshold = v);
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aCuevas", TerrainPanel::sendCave,
                "Excava cuevas con ruido dentro del solido de la seleccion.");
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Altura", 1, 96, ClientToolState.mountainAmplitude, false,
                "Altura maxima de las montanas.", v -> ClientToolState.mountainAmplitude = v);
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aMontanas (" + MOUNTAIN_MODES[mode()] + ")", () -> {
            ClientToolState.mountainNoiseMode = (ClientToolState.mountainNoiseMode + 1) % 3;
            sendMountain();
        }, "Genera montanas. El boton alterna el estilo (Colinas/Crestas/Lomas) y aplica.");
        row += TerraformPanelScreen.RS + 2;

        // --- Erosion ---
        screen.section(x, row, "EROSION");
        row += 11;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Veces", 1, 10, ClientToolState.erosionPasses, true,
                "Cuantas veces se aplica la erosion termica.", v -> ClientToolState.erosionPasses = v.intValue());
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aErosionar", TerrainPanel::sendErosion,
                "Erosion termica: mueve material de las zonas altas a las bajas.");
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Fuerza H.", 0, 2, ClientToolState.hydraulicStrength, false,
                "Intensidad de la erosion hidraulica.", v -> ClientToolState.hydraulicStrength = v);
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aErosion hidraulica", TerrainPanel::sendHydraulic,
                "Simula lluvia: las gotas descienden tallando valles y crestas naturales.");
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Escalon", 2, 16, ClientToolState.terraceStep, true,
                "Altura de cada escalon de las terrazas.", v -> ClientToolState.terraceStep = v.intValue());
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aTerrazas", TerrainPanel::sendTerrace,
                "Convierte el relieve en mesetas escalonadas.");
    }

    private static int mode() {
        int m = ClientToolState.mountainNoiseMode;
        return (m >= 0 && m < MOUNTAIN_MODES.length) ? m : 0;
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
                ClientToolState.mountainOctaves, ClientToolState.mountainNoiseMode, 0, ClientToolState.mountainAmplitude,
                ClientToolState.mountainFrequency, ClientToolState.seed,
                ClientToolState.surfaceBlock, ClientToolState.dirtBlock, ClientToolState.stoneBlock));
    }

    private static void sendHydraulic() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.HYDRAULIC,
                ClientToolState.hydraulicDroplets, 0, 0, ClientToolState.hydraulicStrength, 0,
                ClientToolState.seed, "", "", ""));
    }

    private static void sendTerrace() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.TERRACE,
                ClientToolState.terraceStep, 0, 0, 0, 0, ClientToolState.seed, "", "", ""));
    }

    private static void sendErosion() {
        PacketHandler.sendToServer(new TerrainOperationPacket(TerrainOperationPacket.Op.EROSION,
                ClientToolState.erosionPasses, 0, 0, ClientToolState.erosionTalus, ClientToolState.erosionFactor,
                ClientToolState.seed, "", "", ""));
    }

    @Override
    public String status() {
        return "Configura y aplica suavizado, relieve, materiales, generacion y erosion.";
    }
}
