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
import net.minecraft.core.BlockPos;

/**
 * Pestana de Schematics con colocacion interactiva: cargar archivo, ver fantasma,
 * transformar y pegar donde se ve. Configuracion primero, accion principal al final.
 * Layout denso de 14px.
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
        int half = (width - 4) / 2;
        int third = (width - 8) / 3;
        int row = y;

        // --- Archivo ---
        screen.section(x, row, "ARCHIVO");
        row += 11;
        screen.addRow(x, row, width, "Elegir", screen.addButton(x, row, 280, TerraformPanelScreen.RH,
                loadFileName.isEmpty() ? "(ninguno)" : TerraformPanelScreen.shorten(loadFileName),
                () -> screen.openPicker("Schematics disponibles", ClientSchematicList.files(), loadFileName, false,
                        s -> loadFileName = s),
                "Elige el schematic a colocar (pulsa Refrescar primero si la lista esta vacia)."));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Cargar fantasma",
                () -> PacketHandler.sendToServer(new LoadSchematicPacket(loadFileName)),
                "Carga el archivo y muestra su fantasma para colocarlo.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Refrescar lista",
                () -> PacketHandler.sendToServer(new SchematicListRequestPacket(-1)), "Pide al servidor la lista de schematics.");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "Fantasma: " + (ClientToolState.ghostEnabled ? "ON" : "OFF"),
                () -> ClientToolState.ghostEnabled = !ClientToolState.ghostEnabled,
                "Muestra/oculta la previsualizacion translucida.");
        row += TerraformPanelScreen.RS + 2;

        // --- Transformar ---
        screen.section(x, row, "TRANSFORMAR");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Rotar: " + ROTS[ClientToolState.pasteRotation % 4] + "\u00b0",
                () -> ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4, "Gira el pegado 90 grados en el eje Y.");
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Escala", 1, 8, ClientToolState.pasteScale, true,
                "Cada bloque se expande a un cubo NxNxN.", v -> ClientToolState.pasteScale = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, third, TerraformPanelScreen.RH, "Espejo X: " + onOff(ClientToolState.mirrorX),
                () -> ClientToolState.mirrorX = !ClientToolState.mirrorX, "Refleja en X.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "Espejo Y: " + onOff(ClientToolState.mirrorY),
                () -> ClientToolState.mirrorY = !ClientToolState.mirrorY, "Refleja verticalmente.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "Espejo Z: " + onOff(ClientToolState.mirrorZ),
                () -> ClientToolState.mirrorZ = !ClientToolState.mirrorZ, "Refleja en Z.");
        row += TerraformPanelScreen.RS + 2;

        // --- Posicion ---
        screen.section(x, row, "POSICION");
        row += 11;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "Anclar a: " + (ClientToolState.pasteAtLook ? "el bloque que miras" : "tu posicion"),
                () -> ClientToolState.pasteAtLook = !ClientToolState.pasteAtLook,
                "Punto base del fantasma: el bloque al que apuntas o donde estas parado.");
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, third, TerraformPanelScreen.RH, "X", -64, 64, ClientToolState.pasteOffsetX, true,
                "Desplazamiento Este/Oeste.", v -> ClientToolState.pasteOffsetX = v.intValue());
        screen.addSlider(x + third + 4, row, third, TerraformPanelScreen.RH, "Altura Y", -64, 64, ClientToolState.pasteOffsetY, true,
                "Sube/baja el pegado.", v -> ClientToolState.pasteOffsetY = v.intValue());
        screen.addSlider(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "Z", -64, 64, ClientToolState.pasteOffsetZ, true,
                "Desplazamiento Norte/Sur.", v -> ClientToolState.pasteOffsetZ = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "Centrar (offset 0,0,0)", () -> {
            ClientToolState.pasteOffsetX = 0;
            ClientToolState.pasteOffsetY = 0;
            ClientToolState.pasteOffsetZ = 0;
        }, "Reinicia el desplazamiento del fantasma.");
        row += TerraformPanelScreen.RS + 2;

        // --- Guardar seleccion ---
        screen.section(x, row, "GUARDAR SELECCION");
        row += 11;
        screen.addRow(x, row, half, "Nombre", screen.addEditBox(x, row, half - 55, TerraformPanelScreen.RH,
                ClientToolState.schematicName, "Nombre del archivo (solo letras/numeros).", s -> ClientToolState.schematicName = s));
        screen.addRow(x + half + 4, row, half, "Formato", screen.addButton(x + half + 4, row, half - 65, TerraformPanelScreen.RH,
                FORMATS[ClientToolState.schematicFormat],
                () -> ClientToolState.schematicFormat = (ClientToolState.schematicFormat + 1) % 3,
                "Sponge (.schem), Litematica (.litematic) o Vanilla (.nbt)."));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "\u00a7aGuardar seleccion como schematic",
                () -> PacketHandler.sendToServer(new SaveSchematicPacket(format(), ClientToolState.schematicName)),
                "Guarda la forma real de la seleccion en disco.");
        row += TerraformPanelScreen.RS + 2;

        // --- Accion principal (al final, ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7a\u00a7l\u25b6 PEGAR DONDE EL FANTASMA", SchematicsPanel::paste,
                "Pega el schematic elegido en la posicion/orientacion del fantasma.");
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
        return b ? "Si" : "No";
    }

    @Override
    public String status() {
        return "Archivos: " + ClientSchematicList.files().size()
                + " | El pegado cae donde ves el fantasma. Carga, mueve y pega.";
    }
}
