package com.fantasticterraform.client;

import net.minecraft.core.BlockPos;

/**
 * Estado de "arrastre" de la seleccion del lado cliente. Tras el primer click
 * (izquierdo) se fija el ancla; mientras no se confirme, el segundo punto sigue al
 * bloque al que apuntas, actualizandose en vivo para previsualizar el contorno
 * dinamico. El click derecho confirma. Es puramente local: no genera trafico de red
 * por frame.
 */
public final class ClientDragState {

    private static volatile boolean active;
    private static volatile BlockPos anchor;
    private static volatile BlockPos preview;

    private ClientDragState() {
    }

    public static void begin(BlockPos a) {
        anchor = a;
        preview = a;
        active = true;
    }

    public static void updatePreview(BlockPos p) {
        if (active && p != null) {
            preview = p;
        }
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static BlockPos anchor() {
        return anchor;
    }

    public static BlockPos preview() {
        return preview;
    }
}
