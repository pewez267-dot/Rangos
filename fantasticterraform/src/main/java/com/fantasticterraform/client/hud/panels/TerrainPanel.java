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
    private static final String[] MOUNTAIN_MODES = {"Colinas", "Crestas", "Lomas"};

    @Override
    public String title() {
        return "Terreno";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addEditBox(x, row, half, 16, String.valueOf(ClientToolState.seed),
                "Semilla del ruido (número). Misma semilla = mismo resultado.", s -> {
                    try {
                        ClientToolState.seed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.seed = s.hashCode();
                    }
                });
        screen.addButton(x + half + 4, row, half, 18, "Suavizar", TerrainPanel::sendSmooth,
                "Promedia las alturas de la superficie para suavizar el relieve.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Area", 1, 2, ClientToolState.smoothKernel, true,
                "Tamaño del area que se promedia: 1 = 3x3, 2 = 5x5 (mas grande = mas suave).", v -> ClientToolState.smoothKernel = v.intValue());
        screen.addSlider(x + half + 4, row, half, 16, "Veces", 1, 10, ClientToolState.smoothPasses, true,
                "Cuantas veces se repite el suavizado (mas = mas liso).", v -> ClientToolState.smoothPasses = v.intValue());
        row += 18;
        screen.addSlider(x, row, width, 16, "Fuerza", 0, 1, ClientToolState.smoothIntensity, false,
                "Que tan fuerte suaviza en cada pasada (0 = nada, 1 = máximo).", v -> ClientToolState.smoothIntensity = v);
        row += 20;

        screen.addSlider(x, row, half, 16, "Altura", 1, 64, ClientToolState.deformAmplitude, false,
                "Cuanto sube o baja el terreno (altura de la colina/pendiente).", v -> ClientToolState.deformAmplitude = v);
        screen.addButton(x + half + 4, row, half, 18, "Elevar (" + CURVES[ClientToolState.deformCurve] + ")", () -> {
            ClientToolState.deformCurve = (ClientToolState.deformCurve + 1) % 3;
            sendDeform();
        }, "Sube/baja la superficie formando relieve. El boton alterna la forma (Lineal/Suave/Ruido) y aplica.");
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

        screen.addSlider(x, row, half, 16, "Cuevas", -1, 1, ClientToolState.caveThreshold, false,
                "Cantidad de cuevas: mas a la IZQUIERDA = mas huecos, mas a la derecha = menos.", v -> ClientToolState.caveThreshold = v);
        screen.addButton(x + half + 4, row, half, 18, "Cuevas", TerrainPanel::sendCave,
                "Excava cuevas con ruido dentro del sólido de la selección.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Altura", 1, 96, ClientToolState.mountainAmplitude, false,
                "Altura máxima de las montanas que se generan.", v -> ClientToolState.mountainAmplitude = v);
        screen.addButton(x + half + 4, row, half, 18, "Montañas (" + MOUNTAIN_MODES[mode()] + ")", () -> {
            ClientToolState.mountainNoiseMode = (ClientToolState.mountainNoiseMode + 1) % 3;
            sendMountain();
        }, "Genera montanas. El boton alterna el estilo (Colinas/Crestas/Lomas) y aplica.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Veces", 1, 10, ClientToolState.erosionPasses, true,
                "Cuantas veces se aplica la erosion termica (mas = mas desgastado).", v -> ClientToolState.erosionPasses = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Erosionar", TerrainPanel::sendErosion,
                "Erosion termica: mueve material de las zonas altas a las bajas.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Fuerza H.", 0, 2, ClientToolState.hydraulicStrength, false,
                "Intensidad de la erosion hidraulica (gotas de lluvia que tallan valles).", v -> ClientToolState.hydraulicStrength = v);
        screen.addButton(x + half + 4, row, half, 18, "Erosion hidraulica", TerrainPanel::sendHydraulic,
                "Simula lluvia: las gotas descienden tallando valles y crestas naturales (lo mas realista).");
        row += 20;
        screen.addSlider(x, row, half, 16, "Escalon", 2, 16, ClientToolState.terraceStep, true,
                "Altura de cada escalon de las terrazas.", v -> ClientToolState.terraceStep = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Terrazas", TerrainPanel::sendTerrace,
                "Convierte el relieve en mesetas escalonadas (tipo arrozal/cantera).");
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
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
