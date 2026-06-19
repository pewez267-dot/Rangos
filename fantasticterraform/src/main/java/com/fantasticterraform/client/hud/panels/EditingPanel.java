package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.EditOperationPacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Edicion: rellenar, vaciar, reemplazar, formas, copiar/pegar/mover. */
public final class EditingPanel implements HudPanel {

    @Override
    public String title() {
        return "Edicion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addEditBox(x, row, width, 16, ClientToolState.primaryBlock, s -> ClientToolState.primaryBlock = s);
        row += 20;
        screen.addButton(x, row, half, 18, "Rellenar", () -> send(EditOperationPacket.Op.FILL));
        screen.addButton(x + half + 4, row, half, 18, "Vaciar", () -> send(EditOperationPacket.Op.CLEAR));
        row += 22;

        screen.addEditBox(x, row, half, 16, ClientToolState.replaceFrom, s -> ClientToolState.replaceFrom = s);
        screen.addEditBox(x + half + 4, row, half, 16, ClientToolState.replaceTo, s -> ClientToolState.replaceTo = s);
        row += 18;
        screen.addButton(x, row, width, 18, "Reemplazar A -> B", () -> PacketHandler.sendToServer(
                new EditOperationPacket(EditOperationPacket.Op.REPLACE,
                        ClientToolState.replaceFrom, ClientToolState.replaceTo, 0, 0, 0, 0)));
        row += 22;

        screen.addSlider(x, row, width, 16, "Radio forma", 1, 64, ClientToolState.shapeRadius, true,
                v -> ClientToolState.shapeRadius = v.intValue());
        row += 18;
        screen.addSlider(x, row, half, 16, "Altura", 1, 128, ClientToolState.shapeHeight, true,
                v -> ClientToolState.shapeHeight = v.intValue());
        screen.addSlider(x + half + 4, row, half, 16, "Tam piram.", 1, 64, ClientToolState.shapeSize, true,
                v -> ClientToolState.shapeSize = v.intValue());
        row += 20;
        screen.addButton(x, row, width / 3 - 2, 18, "Esfera", () -> sendShape(EditOperationPacket.Op.SHAPE_SPHERE));
        screen.addButton(x + width / 3, row, width / 3 - 2, 18, "Cilindro", () -> sendShape(EditOperationPacket.Op.SHAPE_CYLINDER));
        screen.addButton(x + 2 * width / 3, row, width / 3, 18, "Piramide", () -> sendShape(EditOperationPacket.Op.SHAPE_PYRAMID));
        row += 22;

        screen.addButton(x, row, half, 18, "Copiar", () -> send(EditOperationPacket.Op.COPY));
        screen.addButton(x + half + 4, row, half, 18, "Pegar (rot " + ClientToolState.pasteRotation * 90 + ")", () ->
                PacketHandler.sendToServer(new EditOperationPacket(EditOperationPacket.Op.PASTE, "", "",
                        playerX(), playerY(), playerZ(), ClientToolState.pasteRotation)));
        row += 22;
        screen.addButton(x, row, half, 18, "Rotar pegado 90", () ->
                ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4);
        screen.addButton(x + half + 4, row, half, 18, "Mover seleccion", () ->
                PacketHandler.sendToServer(new EditOperationPacket(EditOperationPacket.Op.MOVE, "", "",
                        ClientToolState.moveX, ClientToolState.moveY, ClientToolState.moveZ, 0)));
    }

    private static void send(EditOperationPacket.Op op) {
        PacketHandler.sendToServer(new EditOperationPacket(op, ClientToolState.primaryBlock, "", 0, 0, 0, 0));
    }

    private static void sendShape(EditOperationPacket.Op op) {
        if (op == EditOperationPacket.Op.SHAPE_PYRAMID) {
            PacketHandler.sendToServer(new EditOperationPacket(op, ClientToolState.primaryBlock, "",
                    ClientToolState.shapeSize, ClientToolState.shapeHeight, ClientToolState.pyramidInverted ? 1 : 0, 0));
        } else {
            PacketHandler.sendToServer(new EditOperationPacket(op, ClientToolState.primaryBlock, "",
                    ClientToolState.shapeRadius, ClientToolState.shapeHeight, 0, 0));
        }
    }

    private static int playerX() {
        return net.minecraft.client.Minecraft.getInstance().player == null ? 0
                : (int) Math.floor(net.minecraft.client.Minecraft.getInstance().player.getX());
    }

    private static int playerY() {
        return net.minecraft.client.Minecraft.getInstance().player == null ? 0
                : (int) Math.floor(net.minecraft.client.Minecraft.getInstance().player.getY());
    }

    private static int playerZ() {
        return net.minecraft.client.Minecraft.getInstance().player == null ? 0
                : (int) Math.floor(net.minecraft.client.Minecraft.getInstance().player.getZ());
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Bloque (fill/forma) arriba; A->B para reemplazar.", x, y - 12);
    }
}
