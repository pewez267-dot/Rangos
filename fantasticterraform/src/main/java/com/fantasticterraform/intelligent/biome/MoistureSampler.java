package com.fantasticterraform.intelligent.biome;

/** Humedad: determina el bioma de superficie (humedo = pasto, seco = arena). */
public final class MoistureSampler extends BiomeNoiseLayer {

    public MoistureSampler(long baseSeed, double scale) {
        super(baseSeed + 303L, scale > 0 ? scale : 0.020D, 3);
    }
}
