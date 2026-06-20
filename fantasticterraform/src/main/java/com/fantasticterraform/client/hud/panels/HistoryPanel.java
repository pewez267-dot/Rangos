package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientHistoryState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.HistoryRequestPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.UndoRedoPacket;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Panel de Historial VISUAL: muestra la pila de operaciones (etiqueta + bloques) y
 * permite deshacer hasta una operacion concreta con un clic, ademas de deshacer/rehacer
 * paso a paso. El estado se pide al servidor al abrir el panel y se refresca tras cada
 * accion.
 */
public final class HistoryPanel implements HudPanel {

    private static final int MAX_SHOWN = 8;

    @Override
    public String title() {
        return "Historial";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        // Pide el estado actual al abrir/reconstruir el panel.
        PacketHandler.sendToServer(new HistoryRequestPacket());

        int half = (width - 4) / 2;
        int row = y;
        screen.addButton(x, row, half, 18, "Deshacer", () -> PacketHandler.sendToServer(new UndoRedoPacket(true, 1)),
                "Revierte la ultima operacion de edición que hiciste.");
        screen.addButton(x + half + 4, row, half, 18, "Rehacer (" + ClientHistoryState.redoDepth() + ")",
                () -> PacketHandler.sendToServer(new UndoRedoPacket(false, 1)),
                "Reaplica la ultima operacion deshecha.");
        row += 20;
        screen.addButton(x, row, width, 16, "Refrescar historial", () -> PacketHandler.sendToServer(new HistoryRequestPacket()),
                "Vuelve a pedir el historial al servidor.");
        row += 20;

        // Lista visual: cada entrada deshace hasta ese punto inclusive.
        int shown = Math.min(MAX_SHOWN, ClientHistoryState.size());
        for (int i = 0; i < shown; i++) {
            final int count = i + 1;
            String label = ClientHistoryState.label(i);
            int blocks = ClientHistoryState.blocks(i);
            String prefix = (i == 0) ? "\u00a7e\u00bb " : "\u00a77" + count + ". ";
            screen.addButton(x, row + i * 17, width, 15,
                    prefix + shorten(label) + " \u00a78(" + blocks + ")",
                    () -> PacketHandler.sendToServer(new UndoRedoPacket(true, count)),
                    "Deshace desde la mas reciente hasta esta operacion (inclusive): " + count + " operacion(es).");
        }
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 22 ? s.substring(0, 21) + "\u2026" : s;
    }

    @Override
    public String status() {
        int n = ClientHistoryState.size();
        return n == 0 ? "Sin operaciones para deshacer."
                : ("Deshacer: " + n + " | Rehacer: " + ClientHistoryState.redoDepth()
                + " | Clic en una entrada para deshacer hasta ahi.");
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
