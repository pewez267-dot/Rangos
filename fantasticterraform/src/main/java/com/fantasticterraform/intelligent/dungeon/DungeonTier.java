package com.fantasticterraform.intelligent.dungeon;

/** Tiers de tamano de dungeon. El indice mapea a los arrays de config por tier. */
public enum DungeonTier {

    SMALL("Pequena", 0),
    MEDIUM("Mediana", 1),
    LARGE("Grande", 2),
    EPIC("Epica", 3);

    private final String displayName;
    private final int configIndex;

    DungeonTier(String displayName, int configIndex) {
        this.displayName = displayName;
        this.configIndex = configIndex;
    }

    public String displayName() {
        return displayName;
    }

    public int configIndex() {
        return configIndex;
    }

    /** Los tiers Grande y Epica admiten multi-nivel por defecto. */
    public boolean supportsMultiLevel() {
        return this == LARGE || this == EPIC;
    }
}
