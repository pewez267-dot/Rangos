package com.fantasticterraform.client.hud.panels;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

/** Panel de Schematics: guardar, listar, cargar y pegar en .schem/.litematic/.nbt. */
public final class SchematicsPanel implements HudPanel {

    private static final String[] FORMATS = {"Sponge", "Litematica", "Vanilla"};
    private static String loadFileName = "";

    @Override
    public String title() {
        return "Schematics";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addEditBox(x, row, width, 16, ClientToolState.schematicName,
                "Nombre del archivo al guardar (solo letras/numeros).", s -> ClientToolState.schematicName = s);
        row += 20;
        screen.addButton(x, row, width, 18, "Formato: " + FORMATS[ClientToolState.schematicFormat], () ->
                        ClientToolState.schematicFormat = (ClientToolState.schematicFormat + 1) % 3,
                "Alterna el formato de guardado: Sponge (.schem), Litematica (.litematic) o Vanilla (.nbt).");
        row += 22;
        screen.addButton(x, row, width, 18, "Guardar seleccion", () -> PacketHandler.sendToServer(
                new SaveSchematicPacket(format(), ClientToolState.schematicName)),
                "Guarda la forma real de la seleccion como schematic.");
        row += 22;

        screen.addButton(x, row, width, 18, "Refrescar lista", () -> PacketHandler.sendToServer(
                new SchematicListRequestPacket(-1)),
                "Pide al servidor la lista de schematics disponibles.");
        row += 22;
        screen.addButton(x, row, width, 18, "Elegir archivo: " + TerraformPanelScreen.shorten(loadFileName), () ->
                        screen.openPicker("Schematics disponibles", ClientSchematicList.files(), loadFileName, false,
                                s -> loadFileName = s),
                "Elige de la lista el schematic a cargar/pegar (pulsa Refrescar primero).");
        row += 22;
        screen.addButton(x, row, half, 18, "Cargar", () -> PacketHandler.sendToServer(
                new LoadSchematicPacket(loadFileName)), "Carga el archivo elegido al portapapeles (muestra el fantasma).");
        screen.addButton(x + half + 4, row, half, 18, "Pegar (rot " + ClientToolState.pasteRotation * 90 + ")", () -> {
            BlockPos pos = playerPos();
            PacketHandler.sendToServer(new PasteSchematicPacket(loadFileName, pos, ClientToolState.pasteRotation,
                    ClientToolState.mirrorX, ClientToolState.mirrorY, ClientToolState.mirrorZ, ClientToolState.pasteScale));
        }, "Carga y pega el archivo en tu posicion con la transformacion actual.");
        row += 22;
        screen.addButton(x, row, width, 18, "Rotar pegado 90", () ->
                ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4,
                "Gira el pegado en incrementos de 90 grados (eje Y).");
        row += 22;

        // --- Transformacion del pegado ---
        int third = (width - 8) / 3;
        screen.addButton(x, row, third, 18, "Espejo X: " + (ClientToolState.mirrorX ? "\u00a7aSI" : "\u00a77NO"),
                () -> ClientToolState.mirrorX = !ClientToolState.mirrorX, "Refleja el pegado en el eje X.");
        screen.addButton(x + third + 4, row, third, 18, "Espejo Y: " + (ClientToolState.mirrorY ? "\u00a7aSI" : "\u00a77NO"),
                () -> ClientToolState.mirrorY = !ClientToolState.mirrorY, "Refleja el pegado verticalmente (eje Y).");
        screen.addButton(x + 2 * (third + 4), row, third, 18, "Espejo Z: " + (ClientToolState.mirrorZ ? "\u00a7aSI" : "\u00a77NO"),
                () -> ClientToolState.mirrorZ = !ClientToolState.mirrorZ, "Refleja el pegado en el eje Z.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Escala", 1, 8, ClientToolState.pasteScale, true,
                "Escala entera del pegado (cada bloque -> cubo NxNxN).", v -> ClientToolState.pasteScale = v.intValue());
        screen.addButton(x + half + 4, row, half, 18, "Fantasma: " + (ClientToolState.ghostEnabled ? "\u00a7aON" : "\u00a77OFF"),
                () -> ClientToolState.ghostEnabled = !ClientToolState.ghostEnabled,
                "Muestra/oculta la vista previa translucida del pegado en tu posicion.");
    }

    private static SchematicFormat format() {
        SchematicFormat[] values = SchematicFormat.values();
        int f = ClientToolState.schematicFormat;
        return f >= 0 && f < values.length ? values[f] : SchematicFormat.SPONGE;
    }

    private static BlockPos playerPos() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
    }

    @Override
    public String status() {
        return "Schematics disponibles: " + ClientSchematicList.files().size()
                + " | Formato: " + format().displayName();
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
