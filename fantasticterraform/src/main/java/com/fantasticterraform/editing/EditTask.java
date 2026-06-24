package com.fantasticterraform.editing;

import java.util.UUID;

/**
 * Trabajo de edicion procesado por lotes a traves de {@link BlockChangeQueue}.
 * Ninguna operacion masiva se ejecuta de golpe: cada tick se le concede un
 * presupuesto de bloques y debe respetarlo.
 */
public interface EditTask {

    /**
     * Procesa hasta {@code budget} posiciones este tick.
     *
     * @return numero de posiciones realmente consumidas (siempre &lt;= budget).
     */
    int tick(int budget);

    boolean isComplete();

    /** Se llama una sola vez cuando el trabajo termina (empuja historial, notifica). */
    void finish();

    UUID owner();

    String name();

    int processed();

    int total();
}
