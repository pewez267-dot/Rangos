package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientSelectionState;
import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.ClearSelectionPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.SetCylinderHeightPacket;
import com.fantasticterraform.network.SetSelectionModePacket;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Seleccion: elegir geometria, ajustar altura del cilindro, limpiar y modo varita. */
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
            screen.addButton(x + col * (half + 4), y + row * 22, half, 18, type.displayName(),
                    () -> PacketHandler.sendToServer(new SetSelectionModePacket(type)), tooltipFor(type));
        }
        int cy = y + 3 * 22 + 8;
        screen.addSlider(x, cy, width, 18, "Altura cilindro", 1, 384, ClientSelectionState.cylinderHeight(), true,
                "Altura del cilindro de seleccion (en bloques). Solo afecta al modo Cilindro.",
                v -> PacketHandler.sendToServer(new SetCylinderHeightPacket(v.intValue())));
        cy += 24;
        screen.addButton(x, cy, half, 18, "Limpiar", () -> PacketHandler.sendToServer(new ClearSelectionPacket()),
                "Borra todos los puntos y el wireframe de la seleccion actual.");
        screen.addButton(x + half + 4, cy, half, 18, "Varita: " + ClientToolState.wandMode, () -> {
            ClientToolState.wandMode = ClientToolState.wandMode == ClientToolState.WandMode.SELECT
                    ? ClientToolState.WandMode.BRUSH : ClientToolState.WandMode.SELECT;
        }, "Alterna que hace el click de la varita: marcar puntos de seleccion (SELECT) o aplicar el brush activo (BRUSH). Tambien con la tecla V.");
    }

    private static String tooltipFor(SelectionType type) {
        switch (type) {
            case CUBOID:
                return "Caja entre 2 puntos. Click izq = P1, click der = P2.";
            case SPHERE:
                return "Esfera. Click izq = centro, click der = punto del borde (define el radio).";
            case CYLINDER:
                return "Cilindro. Click izq = centro de la base, click der = borde (radio). La altura con el slider.";
            case ELLIPSOID:
                return "Elipsoide con radios X/Y/Z independientes. Click izq = centro, click der = esquina.";
            case POLYGON:
                return "Prisma vertical. Cada click izq anade un vertice; click der cierra el poligono.";
            case CONVEX_HULL:
                return "Forma irregular (envolvente convexo 3D). Click izq anade puntos; click der cierra.";
            default:
                return "";
        }
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        int infoY = y + 3 * 22 + 8 + 50;
        screen.drawLabel(g, "Modo: \u00a7f" + ClientSelectionState.type().displayName(), x, infoY);
        screen.drawLabel(g, "Puntos: \u00a7f" + ClientSelectionState.points().size()
                + (ClientSelectionState.closed() ? " \u00a7a(cerrada)" : ""), x, infoY + 11);
        screen.drawLabel(g, "Volumen: \u00a7f" + ClientSelectionState.volume(), x, infoY + 22);
        screen.drawLabel(g, "Estado: " + (ClientSelectionState.valid() ? "\u00a7avalida" : "\u00a7cincompleta"), x, infoY + 33);
    }
}
