package com.fantasticterraform.intelligent.biome;

/** Continentalidad: ruido de baja frecuencia, zonas elevadas vs. deprimidas a gran escala. */
public final class ContinentalitySampler extends BiomeNoiseLayer {

    public ContinentalitySampler(long baseSeed, double scale) {
        super(baseSeed + 101L, scale > 0 ? scale : 0.004D, 4);
    }
}
