package com.fantasticterraform.terrain.noise;

import java.util.Random;

/**
 * Ruido Perlin mejorado (Ken Perlin, 2002), determinista con semilla. Coherente en
 * el espacio: valores cercanos producen resultados cercanos. No es un Random simple
 * sin coherencia espacial. Incluye una utilidad fractal (suma de octavas).
 */
public final class PerlinNoise {

    private final int[] p = new int[512];

    public PerlinNoise(long seed) {
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) {
            perm[i] = i;
        }
        Random rng = new Random(seed);
        // Barajado de Fisher-Yates determinista.
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            p[i] = perm[i & 255];
        }
    }

    public double noise3D(double x, double y, double z) {
        int xi = floor(x) & 255;
        int yi = floor(y) & 255;
        int zi = floor(z) & 255;
        double xf = x - floor(x);
        double yf = y - floor(y);
        double zf = z - floor(z);

        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        int a = p[xi] + yi;
        int aa = p[a] + zi;
        int ab = p[a + 1] + zi;
        int b = p[xi + 1] + yi;
        int ba = p[b] + zi;
        int bb = p[b + 1] + zi;

        double x1 = lerp(grad(p[aa], xf, yf, zf), grad(p[ba], xf - 1, yf, zf), u);
        double x2 = lerp(grad(p[ab], xf, yf - 1, zf), grad(p[bb], xf - 1, yf - 1, zf), u);
        double y1 = lerp(x1, x2, v);

        double x3 = lerp(grad(p[aa + 1], xf, yf, zf - 1), grad(p[ba + 1], xf - 1, yf, zf - 1), u);
        double x4 = lerp(grad(p[ab + 1], xf, yf - 1, zf - 1), grad(p[bb + 1], xf - 1, yf - 1, zf - 1), u);
        double y2 = lerp(x3, x4, v);

        return lerp(y1, y2, w);
    }

    public double noise2D(double x, double z) {
        return noise3D(x, 0.0D, z);
    }

    /** Ruido fractal: suma de {@code octaves} octavas con persistencia dada, normalizado a [-1, 1]. */
    public double fractal3D(double x, double y, double z, int octaves, double persistence, double lacunarity) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;
        double maxValue = 0.0D;
        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return maxValue == 0 ? 0 : total / maxValue;
    }

    public double fractal2D(double x, double z, int octaves, double persistence, double lacunarity) {
        return fractal3D(x, 0.0D, z, octaves, persistence, lacunarity);
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
