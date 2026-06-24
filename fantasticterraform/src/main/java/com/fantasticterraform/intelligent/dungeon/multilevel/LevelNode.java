package com.fantasticterraform.intelligent.dungeon.multilevel;

/** Banda vertical de un nivel de la dungeon (rango de Y donde se colocan sus salas). */
public final class LevelNode {

    public final int level;
    public final int yMin;
    public final int yMax;

    public LevelNode(int level, int yMin, int yMax) {
        this.level = level;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public int height() {
        return yMax - yMin + 1;
    }
}
