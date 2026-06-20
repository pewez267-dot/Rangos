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

/**
 * Panel de Seleccion: geometria en rejilla compacta, altura de cilindro,
 * transformaciones de region (expandir/contraer/outset/desplazar), relleno smart,
 * apuntado y limpiar. Layout denso de 14px (estilo FantasticCrates).
 */
public final class SelectionPanel implements HudPanel {

    private static final String[] SHORT = {"Cuboide", "Esfera", "Cilindro", "Elipsoide", "Poligono", "Freehand", "Smart"};

    @Override
    public String title() {
        return "Seleccion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int third = (width - 8) / 3;
        int per = 3;
        int gap = 4;
        int bw = (width - (per - 1) * gap) / per;
        int row = y;

        // --- Geometria (rejilla compacta) ---
        screen.section(x, row, "GEOMETRIA");
        row += 11;
        SelectionType[] types = SelectionType.values();
        for (int i = 0; i < types.length; i++) {
            final SelectionType type = types[i];
            int col = i % per;
            int r = i / per;
            boolean active = ClientSelectionState.type() == type;
            String label = (active ? "\u25b6 " : "") + SHORT[Math.min(i, SHORT.length - 1)];
            screen.addButton(x + col * (bw + gap), row + r * TerraformPanelScreen.RS, bw, TerraformPanelScreen.RH,
                    label, () -> PacketHandler.sendToServer(new SetSelectionModePacket(type)), tooltipFor(type));
        }
        row += ((types.length + per - 1) / per) * TerraformPanelScreen.RS + 2;

        screen.addSlider(x, row, width, TerraformPanelScreen.RH, "Altura cilindro", 1, 384,
                ClientSelectionState.cylinderHeight(), true,
                "Altura del cilindro de seleccion. Solo afecta al modo Cilindro.",
                v -> PacketHandler.sendToServer(new SetCylinderHeightPacket(v.intValue())));
        row += TerraformPanelScreen.RS + 2;

        // --- Transformaciones de region ---
        screen.section(x, row, "REGION");
        row += 11;
        screen.addSlider(x, row, width, TerraformPanelScreen.RH, "Cantidad", 1, 32, ClientToolState.selectAmount, true,
                "Bloques para Expandir/Contraer/Outset.", v -> ClientToolState.selectAmount = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, third, TerraformPanelScreen.RH, "\u00a7aExpandir", () -> transform(0),
                "Agranda la region en todos los ejes por 'Cantidad'.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "\u00a7aContraer", () -> transform(1),
                "Reduce la region en todos los ejes por 'Cantidad'.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "\u00a7aOutset", () -> transform(2),
                "Agranda solo en horizontal (X/Z) por 'Cantidad'.");
        row += TerraformPanelScreen.RS;
        int q = (width - 8) / 3;
        screen.addSlider(x, row, q, TerraformPanelScreen.RH, "dX", -16, 16, ClientToolState.shiftX, true,
                "Desplazamiento en X.", v -> ClientToolState.shiftX = v.intValue());
        screen.addSlider(x + q + 4, row, q, TerraformPanelScreen.RH, "dY", -16, 16, ClientToolState.shiftY, true,
                "Desplazamiento en Y.", v -> ClientToolState.shiftY = v.intValue());
        screen.addSlider(x + 2 * (q + 4), row, q, TerraformPanelScreen.RH, "dZ", -16, 16, ClientToolState.shiftZ, true,
                "Desplazamiento en Z.", v -> ClientToolState.shiftZ = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "\u00a7aDesplazar (shift)", () -> PacketHandler.sendToServer(
                        new SelectionTransformPacket(3, 0, ClientToolState.shiftX, ClientToolState.shiftY, ClientToolState.shiftZ)),
                "Mueve toda la region por (dX,dY,dZ) sin re-marcar puntos.");
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Smart max", 100, 200000, ClientToolState.smartMaxBlocks, true,
                "Tope de bloques del relleno SMART (flood-fill).", v -> ClientToolState.smartMaxBlocks = v.intValue());
        row += TerraformPanelScreen.RS + 2;

        // --- Apuntado / utilidades ---
        screen.section(x, row, "APUNTADO");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Diagonal: " + (ClientToolState.smartDiagonal ? "Si" : "No"),
                () -> ClientToolState.smartDiagonal = !ClientToolState.smartDiagonal,
                "El relleno SMART tambien cruza por diagonales (26 vecinos).");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH,
                "Apuntar: " + (ClientToolState.selectAtLook ? "Mirada" : "Posicion"),
                () -> ClientToolState.selectAtLook = !ClientToolState.selectAtLook,
                "Donde marca la varita: el bloque que MIRAS o TU posicion.");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Varita: " + ClientToolState.wandMode,
                () -> ClientToolState.wandMode = ClientToolState.wandMode == ClientToolState.WandMode.SELECT
                        ? ClientToolState.WandMode.BRUSH : ClientToolState.WandMode.SELECT,
                "Alterna el click de la varita: marcar seleccion (SELECT) o aplicar brush (BRUSH). Tecla V.");
        row += TerraformPanelScreen.RS + 2;

        // --- Accion destructiva (unica de ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7cLimpiar seleccion",
                () -> PacketHandler.sendToServer(new ClearSelectionPacket()),
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
                return "Relleno por contiguidad: click izq sobre un bloque selecciona los contiguos iguales.";
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
}
