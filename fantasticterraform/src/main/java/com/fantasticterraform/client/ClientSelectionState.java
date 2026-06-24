package com.fantasticterraform.client;

import com.fantasticterraform.network.SelectionUpdatePacket;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Espejo client-side de la seleccion del jugador, usado por el wireframe y el
 * indicador del HUD. Se actualiza solo cuando llega un {@link SelectionUpdatePacket},
 * nunca por frame, por lo que el wireframe no genera trafico de red.
 */
public final class ClientSelectionState {

    private static volatile SelectionType type = SelectionType.CUBOID;
    private static volatile int cylinderHeight = 5;
    private static volatile boolean closed;
    private static volatile boolean valid;
    private static volatile long volume;
    private static volatile List<BlockPos> points = new ArrayList<>();

    private ClientSelectionState() {
    }

    public static void update(SelectionUpdatePacket packet) {
        type = packet.type();
        cylinderHeight = packet.cylinderHeight;
        closed = packet.closed;
        valid = packet.valid;
        volume = packet.volume;
        points = new ArrayList<>(packet.points);
    }

    public static void clear() {
        points = new ArrayList<>();
        closed = false;
        valid = false;
        volume = 0;
    }

    public static SelectionType type() {
        return type;
    }

    public static int cylinderHeight() {
        return cylinderHeight;
    }

    public static boolean closed() {
        return closed;
    }

    public static boolean valid() {
        return valid;
    }

    public static long volume() {
        return volume;
    }

    public static List<BlockPos> points() {
        return Collections.unmodifiableList(points);
    }
}
