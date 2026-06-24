package com.fantasticterraform.client;

import com.fantasticterraform.network.ClipboardPreviewPacket;

/**
 * Espejo client-side de la vista previa del portapapeles (el "fantasma" del pegado).
 * Se actualiza solo al recibir un {@link ClipboardPreviewPacket}, nunca por frame.
 */
public final class ClientGhostState {

    private static volatile int width;
    private static volatile int height;
    private static volatile int length;
    private static volatile int[] xs = new int[0];
    private static volatile int[] ys = new int[0];
    private static volatile int[] zs = new int[0];
    private static volatile int[] colors = new int[0];

    private ClientGhostState() {
    }

    public static void update(ClipboardPreviewPacket p) {
        width = p.width;
        height = p.height;
        length = p.length;
        xs = p.xs;
        ys = p.ys;
        zs = p.zs;
        colors = p.colors;
    }

    public static void clear() {
        xs = new int[0];
        ys = new int[0];
        zs = new int[0];
        colors = new int[0];
        width = height = length = 0;
    }

    public static boolean hasPreview() {
        return xs.length > 0;
    }

    public static int count() {
        return xs.length;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    public static int length() {
        return length;
    }

    public static int x(int i) {
        return xs[i];
    }

    public static int y(int i) {
        return ys[i];
    }

    public static int z(int i) {
        return zs[i];
    }

    public static int color(int i) {
        return colors[i];
    }
}
