package com.fantasticterraform.intelligent.biome;

/** Erosion: suaviza o hace mas abrupto el terreno segun la continentalidad. */
public final class ErosionSampler extends BiomeNoiseLayer {

    public ErosionSampler(long baseSeed, double scale) {
        super(baseSeed + 202L, scale > 0 ? scale : 0.010D, 3);
    }
}
