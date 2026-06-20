package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientPlacement;
import com.fantasticterraform.client.ClientSchematicList;
import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.LoadSchematicPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.PasteSchematicPacket;
import com.fantasticterraform.network.SaveSchematicPacket;
import com.fantasticterraform.network.SchematicListRequestPacket;
import com.fantasticterraform.schematics.SchematicFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

/**
 * Pestaña de Schematics con COLOCACIÓN INTERACTIVA: cargas un archivo, ves su fantasma
 * translúcido y lo colocas con precisión (rotar, espejar, escalar, mover en X/Y/Z y
 * altura, o anclarlo al bloque que miras). El pegado cae EXACTAMENTE donde se ve el
 * fantasma. Diseño limpio en secciones.
 */
public final class SchematicsPanel implements HudPanel {

    private static final String[] FORMATS = {"Sponge", "Litematica", "Vanilla"};
    private static final int[] ROTS = {0, 90, 180, 270};
    private static String loadFileName = "";

    @Override
    public String title() {
        return "Schematics";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 6) / 2;
        int third = (width - 10) / 3;
        int row = y;

        // ===== ACCIÓN PRINCIPAL =====
        screen.addButton(x, row, width, 20, "\u00a7a\u00a7l\u25b6 PEGAR DONDE EL FANTASMA", SchematicsPanel::paste,
                "Pega el schematic elegido en la posición/orientación del fantasma. Carga uno y muévelo antes.");
        row += 26;

        // ===== ARCHIVO =====
        screen.addHeader(x, row, width, "ARCHIVO");
        row += 13;
        screen.addButton(x, row, width, 18, "Elegir: \u00a7f" + (loadFileName.isEmpty() ? "(ninguno)" : TerraformPanelScreen.shorten(loadFileName)),
                () -> screen.openPicker("Schematics disponibles", ClientSchematicList.files(), loadFileName, false,
                        s -> loadFileName = s),
                "Elige el schematic a colocar (pulsa Refrescar primero si la lista está vacía).");
        row += 20;
        screen.addButton(x, row, half, 18, "Cargar fantasma", () -> PacketHandler.sendToServer(
                new LoadSchematicPacket(loadFileName)), "Carga el archivo y muestra su fantasma para colocarlo.");
        screen.addButton(x + half + 6, row, half, 18, "Refrescar lista", () -> PacketHandler.sendToServer(
                new SchematicListRequestPacket(-1)), "Pide al servidor la lista de schematics.");
        row += 20;
        screen.addButton(x, row, width, 18, "Fantasma: " + (ClientToolState.ghostEnabled ? "\u00a7aON" : "\u00a77OFF"),
                () -> ClientToolState.ghostEnabled = !ClientToolState.ghostEnabled,
                "Muestra/oculta la previsualización translúcida.");
        row += 24;

        // ===== TRANSFORMAR =====
        screen.addHeader(x, row, width, "TRANSFORMAR");
        row += 13;
        screen.addButton(x, row, half, 18, "Rotar: \u00a7f" + ROTS[ClientToolState.pasteRotation % 4] + "\u00b0",
                () -> ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4,
                "Gira el pegado 90° en el eje Y.");
        screen.addSlider(x + half + 6, row, half, 16, "Escala", 1, 8, ClientToolState.pasteScale, true,
                "Cada bloque se expande a un cubo NxNxN.", v -> ClientToolState.pasteScale = v.intValue());
        row += 20;
        screen.addButton(x, row, third, 18, "Espejo X: " + onOff(ClientToolState.mirrorX),
                () -> ClientToolState.mirrorX = !ClientToolState.mirrorX, "Refleja en X.");
        screen.addButton(x + third + 4, row, third, 18, "Espejo Y: " + onOff(ClientToolState.mirrorY),
                () -> ClientToolState.mirrorY = !ClientToolState.mirrorY, "Refleja verticalmente.");
        screen.addButton(x + 2 * (third + 4), row, third, 18, "Espejo Z: " + onOff(ClientToolState.mirrorZ),
                () -> ClientToolState.mirrorZ = !ClientToolState.mirrorZ, "Refleja en Z.");
        row += 24;

        // ===== POSICIÓN =====
        screen.addHeader(x, row, width, "POSICIÓN");
        row += 13;
        screen.addButton(x, row, width, 18, "Anclar a: \u00a7f" + (ClientToolState.pasteAtLook ? "el bloque que miras" : "tu posición"),
                () -> ClientToolState.pasteAtLook = !ClientToolState.pasteAtLook,
                "Punto base del fantasma: el bloque al que apuntas o donde estás parado.");
        row += 20;
        screen.addSlider(x, row, third, 16, "X", -64, 64, ClientToolState.pasteOffsetX, true,
                "Desplazamiento Este/Oeste.", v -> ClientToolState.pasteOffsetX = v.intValue());
        screen.addSlider(x + third + 4, row, third, 16, "Altura Y", -64, 64, ClientToolState.pasteOffsetY, true,
                "Sube/baja el pegado.", v -> ClientToolState.pasteOffsetY = v.intValue());
        screen.addSlider(x + 2 * (third + 4), row, third, 16, "Z", -64, 64, ClientToolState.pasteOffsetZ, true,
                "Desplazamiento Norte/Sur.", v -> ClientToolState.pasteOffsetZ = v.intValue());
        row += 18;
        screen.addButton(x, row, width, 18, "Centrar (offset 0,0,0)", () -> {
            ClientToolState.pasteOffsetX = 0;
            ClientToolState.pasteOffsetY = 0;
            ClientToolState.pasteOffsetZ = 0;
        }, "Reinicia el desplazamiento del fantasma.");
        row += 24;

        // ===== GUARDAR =====
        screen.addHeader(x, row, width, "GUARDAR SELECCIÓN");
        row += 13;
        screen.addEditBox(x, row, half, 16, ClientToolState.schematicName,
                "Nombre del archivo (solo letras/números).", s -> ClientToolState.schematicName = s);
        screen.addButton(x + half + 6, row, half, 18, "Formato: \u00a7f" + FORMATS[ClientToolState.schematicFormat],
                () -> ClientToolState.schematicFormat = (ClientToolState.schematicFormat + 1) % 3,
                "Sponge (.schem), Litematica (.litematic) o Vanilla (.nbt).");
        row += 20;
        screen.addButton(x, row, width, 18, "Guardar selección como schematic", () -> PacketHandler.sendToServer(
                new SaveSchematicPacket(format(), ClientToolState.schematicName)),
                "Guarda la forma real de la selección en disco.");
    }

    private static void paste() {
        if (loadFileName.isEmpty()) {
            return;
        }
        BlockPos pos = ClientPlacement.origin();
        PacketHandler.sendToServer(new PasteSchematicPacket(loadFileName, pos, ClientToolState.pasteRotation,
                ClientToolState.mirrorX, ClientToolState.mirrorY, ClientToolState.mirrorZ, ClientToolState.pasteScale));
    }

    private static SchematicFormat format() {
        SchematicFormat[] values = SchematicFormat.values();
        int f = ClientToolState.schematicFormat;
        return f >= 0 && f < values.length ? values[f] : SchematicFormat.SPONGE;
    }

    private static String onOff(boolean b) {
        return b ? "\u00a7aSí" : "\u00a77No";
    }

    @Override
    public String status() {
        return "Archivos: " + ClientSchematicList.files().size()
                + " | El pegado cae donde ves el fantasma. Carga, mueve y pega.";
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
