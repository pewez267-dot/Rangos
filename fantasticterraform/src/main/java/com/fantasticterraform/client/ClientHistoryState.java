package com.fantasticterraform.client;

import com.fantasticterraform.network.HistoryListPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Espejo client-side del historial de edicion para el panel visual. Solo cambia al
 * recibir un {@link HistoryListPacket}.
 */
public final class ClientHistoryState {

    private static volatile List<String> labels = new ArrayList<>();
    private static volatile int[] sizes = new int[0];
    private static volatile int redoDepth;

    private ClientHistoryState() {
    }

    public static void update(HistoryListPacket p) {
        labels = p.labels;
        sizes = p.sizes;
        redoDepth = p.redoDepth;
    }

    public static int size() {
        return labels.size();
    }

    public static String label(int i) {
        return labels.get(i);
    }

    public static int blocks(int i) {
        return (i >= 0 && i < sizes.length) ? sizes[i] : 0;
    }

    public static int redoDepth() {
        return redoDepth;
    }
}
