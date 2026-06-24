package com.fantasticterraform.intelligent.biome;

/** Temperatura: variantes de superficie (frio = nieve, calido = arena). */
public final class TemperatureSampler extends BiomeNoiseLayer {

    public TemperatureSampler(long baseSeed, double scale) {
        super(baseSeed + 404L, scale > 0 ? scale : 0.020D, 3);
    }
}
