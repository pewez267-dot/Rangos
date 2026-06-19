package com.fantasticterraform.selection;

/**
 * Los seis modos de seleccion. Cada uno calcula su geometria real.
 */
public enum SelectionType {

    /** Bounding box entre P1 y P2. Necesita 2 puntos. */
    CUBOID("cuboid", "Cuboide", 2, false),
    /** P1 = centro, P2 define el radio. Necesita 2 puntos. */
    SPHERE("sphere", "Esfera", 2, false),
    /** P1 = centro base, P2 define el radio; altura ajustable. Necesita 2 puntos. */
    CYLINDER("cylinder", "Cilindro", 2, false),
    /** Radios independientes en X/Y/Z desde el bounding box entre P1 y P2. Necesita 2 puntos. */
    ELLIPSOID("ellipsoid", "Elipsoide", 2, false),
    /** Prisma vertical; base = poligono 2D en XZ. N vertices (minimo 3). */
    POLYGON("polygon", "Polígono", 3, true),
    /** Envolvente convexo 3D de N puntos arbitrarios. */
    CONVEX_HULL("convex_hull", "Freehand / Convex Hull", 4, true),
    /** Seleccion inteligente: relleno por contiguidad (flood-fill) desde un bloque. */
    SMART("smart", "Smart (relleno)", 1, false);

    private final String id;
    private final String displayName;
    private final int minPoints;
    private final boolean multiPoint;

    SelectionType(String id, String displayName, int minPoints, boolean multiPoint) {
        this.id = id;
        this.displayName = displayName;
        this.minPoints = minPoints;
        this.multiPoint = multiPoint;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Numero minimo de puntos necesarios para formar una geometria valida. */
    public int minPoints() {
        return minPoints;
    }

    /**
     * {@code true} para poligono y convex hull (numero variable de puntos que se
     * cierran con click derecho); {@code false} para los modos de exactamente 2 puntos.
     */
    public boolean isMultiPoint() {
        return multiPoint;
    }

    public static SelectionType byId(String id) {
        for (SelectionType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return CUBOID;
    }
}
