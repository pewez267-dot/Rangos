package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientSelectionState;
import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.ClearSelectionPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.SelectionTransformPacket;
import com.fantasticterraform.network.SetCylinderHeightPacket;
import com.fantasticterraform.network.SetSelectionModePacket;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Panel de Seleccion: geometria (incluida la SMART por relleno), altura de cilindro,
 * transformaciones de region (expandir/contraer/outset/desplazar), apuntado por mirada
 * o posicion, y limpiar / modo varita.
 */
public final class SelectionPanel implements HudPanel {

    @Override
    public String title() {
        return "Seleccion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        SelectionType[] types = SelectionType.values();
        for (int i = 0; i < types.length; i++) {
            final SelectionType type = types[i];
            int col = i % 2;
            int row = i / 2;
            boolean active = ClientSelectionState.type() == type;
            String label = (active ? "\u00a7a\u25b6 " : "") + type.displayName();
            screen.addButton(x + col * (half + 4), y + row * 20, half, 18, label,
                    () -> PacketHandler.sendToServer(new SetSelectionModePacket(type)), tooltipFor(type));
        }
        int cy = y + ((types.length + 1) / 2) * 20 + 6;

        screen.addSlider(x, cy, width, 16, "Altura cilindro", 1, 384, ClientSelectionState.cylinderHeight(), true,
                "Altura del cilindro de seleccion. Solo afecta al modo Cilindro.",
                v -> PacketHandler.sendToServer(new SetCylinderHeightPacket(v.intValue())));
        cy += 20;

        // --- Transformaciones de region ---
        screen.addSlider(x, cy, width, 16, "Cantidad", 1, 32, ClientToolState.selectAmount, true,
                "Bloques para Expandir/Contraer/Outset.", v -> ClientToolState.selectAmount = v.intValue());
        cy += 18;
        int third = (width - 8) / 3;
        screen.addButton(x, cy, third, 18, "Expandir", () -> transform(0),
                "Agranda la region en todos los ejes por 'Cantidad'.");
        screen.addButton(x + third + 4, cy, third, 18, "Contraer", () -> transform(1),
                "Reduce la region en todos los ejes por 'Cantidad'.");
        screen.addButton(x + 2 * (third + 4), cy, third, 18, "Outset", () -> transform(2),
                "Agranda solo en horizontal (X/Z) por 'Cantidad'.");
        cy += 22;

        // --- Shift ---
        int q = (width - 8) / 3;
        screen.addSlider(x, cy, q, 16, "dX", -16, 16, ClientToolState.shiftX, true,
                "Desplazamiento en X.", v -> ClientToolState.shiftX = v.intValue());
        screen.addSlider(x + q + 4, cy, q, 16, "dY", -16, 16, ClientToolState.shiftY, true,
                "Desplazamiento en Y.", v -> ClientToolState.shiftY = v.intValue());
        screen.addSlider(x + 2 * (q + 4), cy, q, 16, "dZ", -16, 16, ClientToolState.shiftZ, true,
                "Desplazamiento en Z.", v -> ClientToolState.shiftZ = v.intValue());
        cy += 18;
        screen.addButton(x, cy, width, 18, "Desplazar (shift)", () -> PacketHandler.sendToServer(
                        new SelectionTransformPacket(3, 0, ClientToolState.shiftX, ClientToolState.shiftY, ClientToolState.shiftZ)),
                "Mueve toda la region por (dX,dY,dZ) sin re-marcar puntos.");
        cy += 22;

        // --- Smart ---
        screen.addSlider(x, cy, half, 16, "Smart max", 100, 200000, ClientToolState.smartMaxBlocks, true,
                "Tope de bloques del relleno SMART (flood-fill).", v -> ClientToolState.smartMaxBlocks = v.intValue());
        screen.addButton(x + half + 4, cy, half, 18, "Diagonal: " + (ClientToolState.smartDiagonal ? "\u00a7aSI" : "\u00a77NO"),
                () -> ClientToolState.smartDiagonal = !ClientToolState.smartDiagonal,
                "El relleno SMART tambien cruza por diagonales (26 vecinos).");
        cy += 22;

        // --- Apuntado / utilidades ---
        screen.addButton(x, cy, half, 18, "Apuntar: " + (ClientToolState.selectAtLook ? "Mirada" : "Posicion"),
                () -> ClientToolState.selectAtLook = !ClientToolState.selectAtLook,
                "Donde marca la varita: el bloque que MIRAS o TU posicion.");
        screen.addButton(x + half + 4, cy, half, 18, "Varita: " + ClientToolState.wandMode, () ->
                        ClientToolState.wandMode = ClientToolState.wandMode == ClientToolState.WandMode.SELECT
                                ? ClientToolState.WandMode.BRUSH : ClientToolState.WandMode.SELECT,
                "Alterna el click de la varita: marcar seleccion (SELECT) o aplicar brush (BRUSH). Tecla V.");
        cy += 22;
        screen.addButton(x, cy, width, 18, "Limpiar seleccion", () -> PacketHandler.sendToServer(new ClearSelectionPacket()),
                "Borra todos los puntos y el wireframe de la seleccion actual.");
    }

    private static void transform(int mode) {
        PacketHandler.sendToServer(new SelectionTransformPacket(mode, ClientToolState.selectAmount, 0, 0, 0));
    }

    private static String tooltipFor(SelectionType type) {
        switch (type) {
            case CUBOID:
                return "Caja entre 2 puntos. Click izq = P1, click der = P2.";
            case SPHERE:
                return "Esfera. Click izq = centro, click der = punto del borde (radio).";
            case CYLINDER:
                return "Cilindro. Click izq = centro de la base, click der = borde. Altura con el slider.";
            case ELLIPSOID:
                return "Elipsoide con radios X/Y/Z. Click izq = centro, click der = esquina.";
            case POLYGON:
                return "Prisma vertical. Cada click izq anade vertice; click der cierra.";
            case CONVEX_HULL:
                return "Forma irregular (envolvente convexo 3D). Click izq anade puntos; click der cierra.";
            case SMART:
                return "Relleno por contiguidad: click izq sobre un bloque selecciona todos los contiguos iguales.";
            default:
                return "";
        }
    }

    @Override
    public String status() {
        return "Modo " + ClientSelectionState.type().displayName()
                + " | Pts " + ClientSelectionState.points().size()
                + " | Vol " + ClientSelectionState.volume()
                + (ClientSelectionState.valid() ? " | valida" : " | incompleta");
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
