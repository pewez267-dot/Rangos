package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.EditOperationPacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Edicion: rellenar, vaciar, reemplazar, formas y copiar/pegar/mover. */
public final class EditingPanel implements HudPanel {

    @Override
    public String title() {
        return "Edicion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addPicker(x, row, width, 18, "Bloque", () -> ClientToolState.primaryBlock,
                RegistryLists.blocks(), true,
                "Bloque que usaran Rellenar y las Formas. Elige de la lista (no se escribe).",
                s -> ClientToolState.primaryBlock = s);
        row += 22;
        screen.addButton(x, row, half, 18, "Rellenar", () -> send(EditOperationPacket.Op.FILL),
                "Rellena toda la seleccion con el bloque elegido.");
        screen.addButton(x + half + 4, row, half, 18, "Vaciar", () -> send(EditOperationPacket.Op.CLEAR),
                "Reemplaza toda la seleccion con aire.");
        row += 22;

        screen.addPicker(x, row, half, 18, "De", () -> ClientToolState.replaceFrom,
                RegistryLists.blocks(), true, "Bloque a buscar dentro de la seleccion.",
                s -> ClientToolState.replaceFrom = s);
        screen.addPicker(x + half + 4, row, half, 18, "A", () -> ClientToolState.replaceTo,
                RegistryLists.blocks(), true, "Bloque por el que se sustituye.",
                s -> ClientToolState.replaceTo = s);
        row += 22;
        screen.addButton(x, row, width, 18, "Reemplazar (De -> A)", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.REPLACE,
                                ClientToolState.replaceFrom, ClientToolState.replaceTo, 0, 0, 0, 0)),
                "Sustituye el bloque 'De' por el bloque 'A' solo dentro de la seleccion.");
        row += 22;

        screen.addSlider(x, row, width, 16, "Radio forma", 1, 64, ClientToolState.shapeRadius, true,
                "Radio de las formas Esfera/Cilindro (en bloques).",
                v -> ClientToolState.shapeRadius = v.intValue());
        row += 18;
        screen.addSlider(x, row, half, 16, "Altura", 1, 128, ClientToolState.shapeHeight, true,
                "Altura del Cilindro y de la Piramide.", v -> ClientToolState.shapeHeight = v.intValue());
        screen.addSlider(x + half + 4, row, half, 16, "Tam piram.", 1, 64, ClientToolState.shapeSize, true,
                "Semi-anchura de la base de la Piramide.", v -> ClientToolState.shapeSize = v.intValue());
        row += 20;
        int third = (width - 8) / 3;
        screen.addButton(x, row, third, 18, "Esfera", () -> sendShape(EditOperationPacket.Op.SHAPE_SPHERE),
                "Rellena una esfera centrada en la seleccion (recortada a ella).");
        screen.addButton(x + third + 4, row, third, 18, "Cilindro", () -> sendShape(EditOperationPacket.Op.SHAPE_CYLINDER),
                "Rellena un cilindro centrado en la seleccion.");
        screen.addButton(x + 2 * (third + 4), row, third, 18, "Piramide", () -> sendShape(EditOperationPacket.Op.SHAPE_PYRAMID),
                "Rellena una piramide de base cuadrada.");
        row += 22;

        screen.addButton(x, row, half, 18, "Copiar", () -> send(EditOperationPacket.Op.COPY),
                "Copia la forma real de la seleccion al portapapeles.");
        screen.addButton(x + half + 4, row, half, 18, "Pegar (rot " + ClientToolState.pasteRotation * 90 + ")",
                () -> PacketHandler.sendToServer(new EditOperationPacket(EditOperationPacket.Op.PASTE, "", "",
                        px(), py(), pz(), ClientToolState.pasteRotation)),
                "Pega el portapapeles en tu posicion con la rotacion actual.");
        row += 22;
        screen.addButton(x, row, half, 18, "Rotar 90", () ->
                        ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4,
                "Gira el pegado en incrementos de 90 grados.");
        screen.addButton(x + half + 4, row, half, 18, "Mover", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.MOVE, "", "",
                                ClientToolState.moveX, ClientToolState.moveY, ClientToolState.moveZ, 0)),
                "Mueve el contenido de la seleccion (offset por defecto: +5 en Y).");
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

    private static int px() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.blockPosition().getX();
    }

    private static int py() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.blockPosition().getY();
    }

    private static int pz() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.blockPosition().getZ();
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
