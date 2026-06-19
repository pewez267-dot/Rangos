package com.fantasticterraform.brushes;

/**
 * Curvas de atenuacion (falloff) para los brushes. Devuelven un peso en [0,1] segun la
 * distancia normalizada al centro: 1 en el centro, decayendo hacia el borde. Permiten
 * que los brushes tengan BORDES SUAVES (resultado natural) en vez de cortes duros.
 *
 * <p>Profesionalmente equivale a las "brush curves" de VoxelSniper / GoBrush.</p>
 */
public enum Falloff {

    /** Borde duro: peso 1 dentro del radio, 0 fuera. Conserva el comportamiento clasico. */
    SHARP,
    /** Decaimiento lineal del centro al borde. */
    LINEAR,
    /** Decaimiento suave (smoothstep): bordes muy organicos. */
    SMOOTH,
    /** Campana gaussiana: centro fuerte y borde muy difuminado. */
    GAUSSIAN;

    /** Peso en [0,1] a partir de la distancia y el radio del brush. */
    public double weight(double distance, double radius) {
        if (radius <= 0.0D) {
            return distance <= 0.0D ? 1.0D : 0.0D;
        }
        double t = distance / radius;          // 0 centro .. 1 borde
        if (t >= 1.0D) {
            return this == SHARP ? (t <= 1.0D ? 1.0D : 0.0D) : 0.0D;
        }
        switch (this) {
            case LINEAR:
                return 1.0D - t;
            case SMOOTH: {
                double s = 1.0D - t;
                return s * s * (3.0D - 2.0D * s);   // smoothstep
            }
            case GAUSSIAN: {
                double sigma = 0.5D;                // radio/2 en espacio normalizado
                return Math.exp(-(t * t) / (2.0D * sigma * sigma));
            }
            case SHARP:
            default:
                return 1.0D;
        }
    }

    public String displayName() {
        switch (this) {
            case LINEAR:
                return "Lineal";
            case SMOOTH:
                return "Suave";
            case GAUSSIAN:
                return "Gaussiano";
            case SHARP:
            default:
                return "Duro";
        }
    }

    public static Falloff byIndex(int i) {
        Falloff[] v = values();
        return (i >= 0 && i < v.length) ? v[i] : SMOOTH;
    }
}
