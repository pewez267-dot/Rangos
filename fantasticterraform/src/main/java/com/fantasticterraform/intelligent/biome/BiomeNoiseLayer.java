package com.fantasticterraform.intelligent.biome;

import com.fantasticterraform.terrain.noise.PerlinNoise;

/**
 * Una capa de ruido fractal independiente con su propia semilla y escala. Base comun
 * de las cuatro capas (continentalidad, erosion, humedad, temperatura).
 */
public class BiomeNoiseLayer {

    private final PerlinNoise noise;
    private final double scale;
    private final int octaves;

    public BiomeNoiseLayer(long seed, double scale, int octaves) {
        this.noise = new PerlinNoise(seed);
        this.scale = scale;
        this.octaves = Math.max(1, octaves);
    }

    /** Valor en [-1, 1]. */
    public double sample(double x, double z) {
        return noise.fractal2D(x * scale, z * scale, octaves, 0.5D, 2.0D);
    }

    /** Valor normalizado en [0, 1]. */
    public double normalized(double x, double z) {
        return (sample(x, z) + 1.0D) * 0.5D;
    }
}
