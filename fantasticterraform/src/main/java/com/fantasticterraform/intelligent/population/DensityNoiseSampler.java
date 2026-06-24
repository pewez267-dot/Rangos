package com.fantasticterraform.intelligent.population;

import com.fantasticterraform.terrain.noise.PerlinNoise;

/** Ruido de densidad de baja frecuencia que crea agrupaciones naturales del poblamiento. */
public final class DensityNoiseSampler {

    private final PerlinNoise noise;
    private final double scale;

    public DensityNoiseSampler(long seed, double scale) {
        this.noise = new PerlinNoise(seed + 505L);
        this.scale = scale > 0 ? scale : 0.05D;
    }

    /** Valor normalizado en [0, 1]. */
    public double normalized(double x, double z) {
        return (noise.fractal2D(x * scale, z * scale, 3, 0.5D, 2.0D) + 1.0D) * 0.5D;
    }
}
