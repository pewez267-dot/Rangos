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
        screen.addButton(x, y, half, 18, "Deshacer", () -> PacketHandler.sendToServer(new UndoRedoPacket(true)));
        screen.addButton(x + half + 4, y, half, 18, "Rehacer", () -> PacketHandler.sendToServer(new UndoRedoPacket(false)));
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Deshacer revierte la ultima operacion; rehacer la reaplica.", x, y + 26);
        screen.drawLabel(g, "Limites: history_stack_size y max_undo_blocks_per_operation.", x, y + 37);
    }
}
