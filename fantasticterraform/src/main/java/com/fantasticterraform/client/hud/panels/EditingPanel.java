package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.EditOperationPacket;
import com.fantasticterraform.network.PacketHandler;

/** Panel de Edicion: rellenar, vaciar, reemplazar, formas, portapapeles, operaciones y apilar. */
public final class EditingPanel implements HudPanel {

    private static final String[] AXES = {"X", "Y", "Z"};

    @Override
    public String title() {
        return "Edicion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int third = (width - 8) / 3;
        int row = y;

        // --- Relleno ---
        screen.section(x, row, "RELLENO");
        row += 11;
        screen.addRow(x, row, width, "Bloque", screen.addPicker(x, row, 200, TerraformPanelScreen.RH,
                () -> ClientToolState.primaryBlock, RegistryLists.blocks(), true,
                "Bloque que usan Rellenar y las Formas.", s -> ClientToolState.primaryBlock = s));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "\u00a7aRellenar", () -> send(EditOperationPacket.Op.FILL),
                "Rellena toda la seleccion con el bloque elegido.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7cVaciar", () -> send(EditOperationPacket.Op.CLEAR),
                "Reemplaza toda la seleccion con aire.");
        row += TerraformPanelScreen.RS + 2;

        // --- Reemplazar ---
        screen.section(x, row, "REEMPLAZAR");
        row += 11;
        screen.addRow(x, row, half, "De", screen.addPicker(x, row, half - 30, TerraformPanelScreen.RH,
                () -> ClientToolState.replaceFrom, RegistryLists.blocks(), true,
                "Bloque a buscar dentro de la seleccion.", s -> ClientToolState.replaceFrom = s));
        screen.addRow(x + half + 4, row, half, "A", screen.addPicker(x + half + 4, row, half - 30, TerraformPanelScreen.RH,
                () -> ClientToolState.replaceTo, RegistryLists.blocks(), true,
                "Bloque por el que se sustituye.", s -> ClientToolState.replaceTo = s));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "\u00a7aReemplazar (De\u2192A)", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.REPLACE,
                                ClientToolState.replaceFrom, ClientToolState.replaceTo, 0, 0, 0, 0)),
                "Sustituye el bloque 'De' por el bloque 'A' solo dentro de la seleccion.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aReemp.\u2192patron", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.REPLACE_PATTERN, ClientToolState.replaceFrom,
                                ClientToolState.editPattern, 0, 0, 0, 0)),
                "Sustituye el bloque 'De' por la mezcla del patron (abajo).");
        row += TerraformPanelScreen.RS + 2;

        // --- Formas ---
        screen.section(x, row, "FORMAS");
        row += 11;
        screen.addSlider(x, row, third, TerraformPanelScreen.RH, "Radio", 1, 64, ClientToolState.shapeRadius, true,
                "Radio de Esfera/Cilindro.", v -> ClientToolState.shapeRadius = v.intValue());
        screen.addSlider(x + third + 4, row, third, TerraformPanelScreen.RH, "Altura", 1, 128, ClientToolState.shapeHeight, true,
                "Altura del Cilindro y la Piramide.", v -> ClientToolState.shapeHeight = v.intValue());
        screen.addSlider(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "Tam.pir", 1, 64, ClientToolState.shapeSize, true,
                "Semi-anchura de la base de la Piramide.", v -> ClientToolState.shapeSize = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, third, TerraformPanelScreen.RH, "\u00a7aEsfera", () -> sendShape(EditOperationPacket.Op.SHAPE_SPHERE),
                "Rellena una esfera centrada en la seleccion.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "\u00a7aCilindro", () -> sendShape(EditOperationPacket.Op.SHAPE_CYLINDER),
                "Rellena un cilindro centrado en la seleccion.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "\u00a7aPiramide", () -> sendShape(EditOperationPacket.Op.SHAPE_PYRAMID),
                "Rellena una piramide de base cuadrada.");
        row += TerraformPanelScreen.RS + 2;

        // --- Portapapeles ---
        screen.section(x, row, "PORTAPAPELES");
        row += 11;
        screen.addButton(x, row, third, TerraformPanelScreen.RH, "\u00a7aCopiar", () -> send(EditOperationPacket.Op.COPY),
                "Copia la forma real de la seleccion al portapapeles.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "Rotar 90",
                () -> ClientToolState.pasteRotation = (ClientToolState.pasteRotation + 1) % 4,
                "Gira el pegado en incrementos de 90 grados.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "\u00a7aPegar (rot " + ClientToolState.pasteRotation * 90 + ")",
                () -> {
                    net.minecraft.core.BlockPos o = com.fantasticterraform.client.ClientPlacement.origin();
                    PacketHandler.sendToServer(new EditOperationPacket(EditOperationPacket.Op.PASTE, "", "",
                            o.getX(), o.getY(), o.getZ(), packedTransform()));
                },
                "Pega el portapapeles donde ves el fantasma.");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "\u00a7aMover", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.MOVE, "", "",
                                ClientToolState.moveX, ClientToolState.moveY, ClientToolState.moveZ, 0)),
                "Mueve el contenido de la seleccion (offset por defecto: +5 en Y).");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7cHuecar", () -> send(EditOperationPacket.Op.HOLLOW),
                "Vacia el interior de la seleccion, dejando solo la cascara.");
        row += TerraformPanelScreen.RS + 2;

        // --- Operaciones ---
        screen.section(x, row, "OPERACIONES");
        row += 11;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Pasadas 3D", 1, 6, ClientToolState.smooth3DPasses, true,
                "Iteraciones del suavizado 3D (mas = mas redondeado).", v -> ClientToolState.smooth3DPasses = v.intValue());
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aSuavizar 3D", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.SMOOTH3D, "", "",
                                ClientToolState.smooth3DPasses, 0, 0, 0)),
                "Suavizado volumetrico real (funde salientes y rellena huecos en 3D).");
        row += TerraformPanelScreen.RS + 2;

        // --- Patrones y contorno ---
        screen.section(x, row, "PATRONES Y CONTORNO");
        row += 11;
        screen.addRow(x, row, width, "Patron", screen.addEditBox(x, row, 320, TerraformPanelScreen.RH, ClientToolState.editPattern,
                "Patron ponderado. Ej: 50%stone,50%cobblestone  o  3 oak_log, dirt",
                s -> ClientToolState.editPattern = s));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "\u00a7aRellenar patron", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.FILL_PATTERN, ClientToolState.editPattern, "", 0, 0, 0, 0)),
                "Rellena la seleccion con la mezcla del patron.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "\u00a7aMuros (patron)", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.WALLS, ClientToolState.editPattern, "", 0, 0, 0, 0)),
                "Construye los muros verticales del contorno con el patron.");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "\u00a7aContorno 6 caras", () -> PacketHandler.sendToServer(
                        new EditOperationPacket(EditOperationPacket.Op.OUTLINE, ClientToolState.editPattern, "", 0, 0, 0, 0)),
                "Rellena las SEIS caras de la seleccion (muros + suelo + techo) con el patron.");
        row += TerraformPanelScreen.RS + 2;

        // --- Apilar ---
        screen.section(x, row, "APILAR");
        row += 11;
        screen.addSlider(x, row, width, TerraformPanelScreen.RH, "Copias", 1, 32, ClientToolState.stackCount, true,
                "Numero de repeticiones del apilado.", v -> ClientToolState.stackCount = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, third, TerraformPanelScreen.RH, "Eje: " + AXES[ClientToolState.stackAxis % 3],
                () -> ClientToolState.stackAxis = (ClientToolState.stackAxis + 1) % 3,
                "Eje a lo largo del cual se repite la seleccion.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "Dir: " + (ClientToolState.stackPositive ? "+" : "-"),
                () -> ClientToolState.stackPositive = !ClientToolState.stackPositive,
                "Sentido de la repeticion.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "\u00a7aApilar x" + ClientToolState.stackCount,
                () -> PacketHandler.sendToServer(new EditOperationPacket(EditOperationPacket.Op.STACK, "", "",
                        ClientToolState.stackAxis, ClientToolState.stackPositive ? 0 : 1, ClientToolState.stackCount, 0)),
                "Repite el contenido de la seleccion N veces a lo largo del eje.");
    }

    /** Empaqueta rotacion (bits 0-1), espejo X/Y/Z (bits 2-4) y escala (bits 8-11). */
    private static int packedTransform() {
        int r = ClientToolState.pasteRotation & 0x3;
        if (ClientToolState.mirrorX) {
            r |= 0x4;
        }
        if (ClientToolState.mirrorY) {
            r |= 0x8;
        }
        if (ClientToolState.mirrorZ) {
            r |= 0x10;
        }
        r |= (Math.max(1, Math.min(8, ClientToolState.pasteScale)) & 0xF) << 8;
        return r;
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

    @Override
    public String status() {
        return "Edita la seleccion: rellena, reemplaza, esculpe formas y apila.";
    }
}
