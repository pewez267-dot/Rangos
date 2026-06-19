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

import java.util.List;

/** Panel de Schematics: guardar la seleccion, listar, cargar y pegar en .schem/.litematic/.nbt. */
public final class SchematicsPanel implements HudPanel {

    private static String loadFileName = "";

    @Override
    public String title() {
        return "Schematics";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int third = (width - 8) / 3;
        int row = y;

        screen.addEditBox(x, row, width, 16, ClientToolState.schematicName, s -> ClientToolState.schematicName = s);
        row += 20;
        screen.addButton(x, row, third, 18, "Sponge", () -> ClientToolState.schematicFormat = 0);
        screen.addButton(x + third + 4, row, third, 18, "Litematic", () -> ClientToolState.schematicFormat = 1);
        screen.addButton(x + 2 * (third + 4), row, third, 18, "Vanilla", () -> ClientToolState.schematicFormat = 2);
        row += 22;
        screen.addButton(x, row, width, 18, "Guardar seleccion", () -> PacketHandler.sendToServer(
                new SaveSchematicPacket(format(), ClientToolState.schematicName)));
        row += 22;
        screen.addButton(x, row, width, 18, "Refrescar lista", () -> PacketHandler.sendToServer(
                new SchematicListRequestPacket(-1)));
        row += 22;

        screen.addEditBox(x, row, width, 16, loadFileName, s -> loadFileName = s);
        row += 20;
        int half = (width - 4) / 2;
        screen.addButton(x, row, half, 18, "Cargar", () -> PacketHandler.sendToServer(
                new LoadSchematicPacket(loadFileName)));
        screen.addButton(x + half + 4, row, half, 18, "Pegar (rot " + ClientToolState.pasteRotation * 90 + ")", () -> {
            BlockPos pos = playerPos();
            PacketHandler.sendToServer(new PasteSchematicPacket(loadFileName, pos, ClientToolState.pasteRotation));
        });
        row += 22;
        screen.addButton(x, row, width, 18, "Rotar pegado 90", () ->
                ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4);
    }

    private static SchematicFormat format() {
        SchematicFormat[] values = SchematicFormat.values();
        int f = ClientToolState.schematicFormat;
        return f >= 0 && f < values.length ? values[f] : SchematicFormat.SPONGE;
    }

    private static BlockPos playerPos() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return BlockPos.ZERO;
        }
        return mc.player.blockPosition();
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Formato: \u00a7f" + format().displayName(), x, y - 12);
        List<String> files = ClientSchematicList.files();
        int ly = y + 9 * 22 + 4;
        screen.drawLabel(g, "Disponibles (" + files.size() + "):", x, ly);
        int shown = Math.min(files.size(), 8);
        for (int i = 0; i < shown; i++) {
            screen.drawLabel(g, "\u00a77- \u00a7f" + files.get(i), x, ly + 11 + i * 10);
        }
    }
}
