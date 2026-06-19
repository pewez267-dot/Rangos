package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.UndoRedoPacket;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Historial: deshacer y rehacer. Las reversiones pasan por la cola por ticks. */
public final class HistoryPanel implements HudPanel {

    @Override
    public String title() {
        return "Historial";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        screen.addButton(x, y, half, 18, "Deshacer", () -> PacketHandler.sendToServer(new UndoRedoPacket(true)),
                "Revierte la ultima operacion de edicion que hiciste.");
        screen.addButton(x + half + 4, y, half, 18, "Rehacer", () -> PacketHandler.sendToServer(new UndoRedoPacket(false)),
                "Reaplica la ultima operacion deshecha (si no hiciste otra nueva).");
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Deshacer/Rehacer afectan solo a tus", x, y + 28);
        screen.drawLabel(g, "operaciones. Limites en config.toml.", x, y + 39);
    }
}
