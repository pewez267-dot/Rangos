package com.fantasticterraform.network;

import net.minecraft.server.level.ServerPlayer;

/** Resolucion de semillas para la generacion inteligente (0 = aleatoria, o config). */
public final class IntelligentSeeds {

    private IntelligentSeeds() {
    }

    /**
     * @param packetSeed semilla enviada por el HUD (0 = usar config/aleatoria)
     * @param configSeed semilla de config (-1 = aleatoria)
     */
    public static long resolve(long packetSeed, long configSeed, ServerPlayer player) {
        if (packetSeed != 0L) {
            return packetSeed;
        }
        if (configSeed != -1L) {
            return configSeed;
        }
        return player.level().getGameTime() * 1099511628211L
                ^ player.getUUID().getLeastSignificantBits()
                ^ System.nanoTime();
    }
}
