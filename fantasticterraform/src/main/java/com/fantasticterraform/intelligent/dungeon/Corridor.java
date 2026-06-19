package com.fantasticterraform.intelligent.dungeon;

/** Conexion entre dos salas (por indice en la lista del grafo). */
public final class Corridor {

    public final int roomA;
    public final int roomB;

    public Corridor(int roomA, int roomB) {
        this.roomA = roomA;
        this.roomB = roomB;
    }
}
