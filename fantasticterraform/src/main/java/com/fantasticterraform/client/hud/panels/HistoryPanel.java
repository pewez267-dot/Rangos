package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientHistoryState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.HistoryRequestPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.UndoRedoPacket;

/**
 * Panel de Historial visual: pila de operaciones (etiqueta + bloques), con deshacer/rehacer
 * paso a paso y deshacer hasta una operacion concreta con un clic. Layout denso de 14px.
 */
public final class HistoryPanel implements HudPanel {

    private static final int MAX_SHOWN = 10;

    @Override
    public String title() {
        return "Historial";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        // Pide el estado actual al abrir/reconstruir el panel.
        PacketHandler.sendToServer(new HistoryRequestPacket());

        int third = (width - 8) / 3;
        int row = y;

        screen.addButton(x, row, third, TerraformPanelScreen.RH, "Deshacer",
                () -> PacketHandler.sendToServer(new UndoRedoPacket(true, 1)),
                "Revierte la ultima operacion de edicion que hiciste.");
        screen.addButton(x + third + 4, row, third, TerraformPanelScreen.RH, "Rehacer (" + ClientHistoryState.redoDepth() + ")",
                () -> PacketHandler.sendToServer(new UndoRedoPacket(false, 1)),
                "Reaplica la ultima operacion deshecha.");
        screen.addButton(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "Refrescar",
                () -> PacketHandler.sendToServer(new HistoryRequestPacket()),
                "Vuelve a pedir el historial al servidor.");
        row += TerraformPanelScreen.RS + 2;

        screen.section(x, row, "PILA DE OPERACIONES");
        row += 11;
        int shown = Math.min(MAX_SHOWN, ClientHistoryState.size());
        if (shown == 0) {
            screen.label(x, row + 3, "Sin operaciones registradas todavia.");
        }
        for (int i = 0; i < shown; i++) {
            final int count = i + 1;
            String label = ClientHistoryState.label(i);
            int blocks = ClientHistoryState.blocks(i);
            String prefix = (i == 0) ? "\u00bb " : count + ". ";
            screen.addButton(x, row + i * TerraformPanelScreen.RS, width, TerraformPanelScreen.RH,
                    prefix + shorten(label) + "  (" + blocks + ")",
                    () -> PacketHandler.sendToServer(new UndoRedoPacket(true, count)),
                    "Deshace desde la mas reciente hasta esta operacion (inclusive): " + count + " operacion(es).");
        }
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 40 ? s.substring(0, 39) + "\u2026" : s;
    }

    @Override
    public String status() {
        int n = ClientHistoryState.size();
        return n == 0 ? "Sin operaciones para deshacer."
                : ("Deshacer: " + n + " | Rehacer: " + ClientHistoryState.redoDepth()
                + " | Clic en una entrada para deshacer hasta ahi.");
    }
}
