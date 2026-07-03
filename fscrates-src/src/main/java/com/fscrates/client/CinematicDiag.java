package com.fscrates.client;

/**
 * Puente de diagnostico entre el Mixin de corte de mundo y la pantalla de la cinematica.
 * El Mixin (LevelRendererMixin) NO se puede referenciar directo desde codigo normal en
 * produccion (el transformador de Mixin consume esa clase), asi que escribe aqui su estado
 * y la pantalla lo lee desde una clase normal. Seguro en runtime.
 */
public final class CinematicDiag {
    /** nanoTime de la ultima vez que el Mixin corto el render del mundo 3D. 0 = nunca. */
    public static volatile long lastCullNanos = 0L;
    /** contador total de frames en los que se corto el mundo. */
    public static volatile long cullFrames = 0L;

    private CinematicDiag() {
    }

    public static void markCull() {
        lastCullNanos = System.nanoTime();
        ++cullFrames;
    }
}
