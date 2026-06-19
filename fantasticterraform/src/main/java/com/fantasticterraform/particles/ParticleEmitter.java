package com.fantasticterraform.particles;

import java.util.UUID;

/**
 * Definicion persistente de un emisor de particulas. Se genera del lado cliente: el
 * servidor solo envia esta definicion una vez cuando el jugador entra en rango, nunca
 * particula por particula.
 */
public final class ParticleEmitter {

    public String id = UUID.randomUUID().toString();
    public String dimension = "minecraft:overworld";
    public double x;
    public double y;
    public double z;
    /** ResourceLocation del tipo de particula, p.ej. "minecraft:flame". */
    public String particleType = "minecraft:flame";
    /** Particulas por segundo. */
    public double emissionRate = 10.0D;
    public double vx;
    public double vy;
    public double vz;
    /** Color (para particulas tintables tipo dust). */
    public float red = 1.0F;
    public float green = 1.0F;
    public float blue = 1.0F;
    public float size = 1.0F;
    /** Radio (bloques) dentro del cual el cliente renderiza el emisor. */
    public double visibilityRadius = 32.0D;
    /** Duracion en ticks; -1 = infinito. */
    public long durationTicks = -1L;
    /** Tiempo de juego del servidor en que se creo (para expiracion). No persistido como vital. */
    public transient long createdGameTime;

    public ParticleEmitter copy() {
        ParticleEmitter e = new ParticleEmitter();
        e.id = id;
        e.dimension = dimension;
        e.x = x;
        e.y = y;
        e.z = z;
        e.particleType = particleType;
        e.emissionRate = emissionRate;
        e.vx = vx;
        e.vy = vy;
        e.vz = vz;
        e.red = red;
        e.green = green;
        e.blue = blue;
        e.size = size;
        e.visibilityRadius = visibilityRadius;
        e.durationTicks = durationTicks;
        e.createdGameTime = createdGameTime;
        return e;
    }
}
