package com.fantasticterraform.intelligent.dungeon;

import java.util.ArrayList;
import java.util.List;

/** Grafo de la dungeon: salas (nodos) y pasillos (aristas). */
public final class DungeonGraph {

    public final List<Room> rooms = new ArrayList<>();
    public final List<Corridor> corridors = new ArrayList<>();
}
