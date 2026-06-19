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

/** Panel de Seleccion: elegir modo, ajustar altura del cilindro, limpiar y modo varita. */
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
            int bx = x + col * (half + 4);
            int by = y + row * 22;
            screen.addButton(bx, by, half, 18, type.displayName(),
                    () -> PacketHandler.sendToServer(new SetSelectionModePacket(type)));
        }
        int cy = y + 3 * 22 + 6;
        screen.addSlider(x, cy, width, 18, "Altura cilindro", 1, 384, ClientSelectionState.cylinderHeight(), true,
                v -> PacketHandler.sendToServer(new SetCylinderHeightPacket(v.intValue())));
        cy += 24;
        screen.addButton(x, cy, half, 18, "Limpiar seleccion",
                () -> PacketHandler.sendToServer(new ClearSelectionPacket()));
        screen.addButton(x + half + 4, cy, half, 18, "Alternar varita", () -> {
            ClientToolState.wandMode = ClientToolState.wandMode == ClientToolState.WandMode.SELECT
                    ? ClientToolState.WandMode.BRUSH : ClientToolState.WandMode.SELECT;
        });
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        int infoY = y + 3 * 22 + 6 + 48;
        screen.drawLabel(g, "Modo: \u00a7f" + ClientSelectionState.type().displayName(), x, infoY);
        screen.drawLabel(g, "Puntos: \u00a7f" + ClientSelectionState.points().size()
                + (ClientSelectionState.closed() ? " (cerrada)" : ""), x, infoY + 11);
        screen.drawLabel(g, "Volumen: \u00a7f" + ClientSelectionState.volume(), x, infoY + 22);
        boolean valid = ClientSelectionState.valid();
        screen.drawLabel(g, "Estado: " + (valid ? "\u00a7avalida" : "\u00a7cincompleta"), x, infoY + 33);
        screen.drawLabel(g, "Varita: \u00a7f" + ClientToolState.wandMode, x, infoY + 44);
    }
}
