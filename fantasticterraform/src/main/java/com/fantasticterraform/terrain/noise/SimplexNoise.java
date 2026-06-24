package com.fantasticterraform.terrain.noise;

import java.util.Random;

/**
 * Ruido Simplex (Stefan Gustavson), determinista con semilla. Alternativa al Perlin
 * con menos artefactos direccionales. Implementa 2D y 3D mas una utilidad fractal.
 */
public final class SimplexNoise {

    private static final double F2 = 0.5D * (Math.sqrt(3.0D) - 1.0D);
    private static final double G2 = (3.0D - Math.sqrt(3.0D)) / 6.0D;
    private static final double F3 = 1.0D / 3.0D;
    private static final double G3 = 1.0D / 6.0D;

    private static final int[][] GRAD3 = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };

    private final short[] perm = new short[512];
    private final short[] permMod12 = new short[512];

    public SimplexNoise(long seed) {
        short[] p = new short[256];
        for (int i = 0; i < 256; i++) {
            p[i] = (short) i;
        }
        Random rng = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            short tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
            permMod12[i] = (short) (perm[i] % 12);
        }
    }

    public double noise2D(double xin, double yin) {
        double s = (xin + yin) * F2;
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        double t = (i + j) * G2;
        double x0 = xin - (i - t);
        double y0 = yin - (j - t);

        int i1;
        int j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2;
        double y2 = y0 - 1.0D + 2.0D * G2;

        int ii = i & 255;
        int jj = j & 255;
        int gi0 = permMod12[ii + perm[jj]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1]];
        int gi2 = permMod12[ii + 1 + perm[jj + 1]];

        double n0 = corner2D(x0, y0, gi0);
        double n1 = corner2D(x1, y1, gi1);
        double n2 = corner2D(x2, y2, gi2);

        return 70.0D * (n0 + n1 + n2);
    }

    public double noise3D(double xin, double yin, double zin) {
        double s = (xin + yin + zin) * F3;
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        int k = fastFloor(zin + s);
        double t = (i + j + k) * G3;
        double x0 = xin - (i - t);
        double y0 = yin - (j - t);
        double z0 = zin - (k - t);

        int i1;
        int j1;
        int k1;
        int i2;
        int j2;
        int k2;
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1;
            } else {
                i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            }
        }

        double x1 = x0 - i1 + G3;
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0D * G3;
        double y2 = y0 - j2 + 2.0D * G3;
        double z2 = z0 - k2 + 2.0D * G3;
        double x3 = x0 - 1.0D + 3.0D * G3;
        double y3 = y0 - 1.0D + 3.0D * G3;
        double z3 = z0 - 1.0D + 3.0D * G3;

        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int gi0 = permMod12[ii + perm[jj + perm[kk]]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]];
        int gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]];
        int gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]];

        double n0 = corner3D(x0, y0, z0, gi0);
        double n1 = corner3D(x1, y1, z1, gi1);
        double n2 = corner3D(x2, y2, z2, gi2);
        double n3 = corner3D(x3, y3, z3, gi3);

        return 32.0D * (n0 + n1 + n2 + n3);
    }

    public double fractal3D(double x, double y, double z, int octaves, double persistence, double lacunarity) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;
        double maxValue = 0.0D;
        for (int o = 0; o < octaves; o++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return maxValue == 0 ? 0 : total / maxValue;
    }

    public double fractal2D(double x, double z, int octaves, double persistence, double lacunarity) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;
        double maxValue = 0.0D;
        for (int o = 0; o < octaves; o++) {
            total += noise2D(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return maxValue == 0 ? 0 : total / maxValue;
    }

    private static double corner2D(double x, double y, int gi) {
        double t = 0.5D - x * x - y * y;
        if (t < 0) {
            return 0.0D;
        }
        t *= t;
        return t * t * (GRAD3[gi][0] * x + GRAD3[gi][1] * y);
    }

    private static double corner3D(double x, double y, double z, int gi) {
        double t = 0.6D - x * x - y * y - z * z;
        if (t < 0) {
            return 0.0D;
        }
        t *= t;
        return t * t * (GRAD3[gi][0] * x + GRAD3[gi][1] * y + GRAD3[gi][2] * z);
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
