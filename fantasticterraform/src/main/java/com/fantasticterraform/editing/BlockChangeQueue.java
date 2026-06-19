package com.fantasticterraform.editing;

import com.fantasticterraform.config.TerraformConfig;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Cola por ticks central. TODA operacion masiva (edicion, brushes, terreno,
 * deshacer/rehacer, pegado de schematics) se procesa aqui, nunca de golpe.
 *
 * <p>Cada tick del servidor se reparte un presupuesto total de
 * {@code max_blocks_per_tick} posiciones entre los trabajos en orden FIFO. Si un
 * trabajo agota el presupuesto sin terminar, continua el tick siguiente. El limite
 * es innegociable sin importar cuantas herramientas use el mod.</p>
 */
public final class BlockChangeQueue {

    private static final Deque<EditTask> TASKS = new ArrayDeque<>();

    private BlockChangeQueue() {
    }

    public static synchronized void enqueue(EditTask task) {
        TASKS.addLast(task);
    }

    public static synchronized int pending() {
        return TASKS.size();
    }

    public static synchronized boolean hasWorkFor(java.util.UUID owner) {
        for (EditTask t : TASKS) {
            if (t.owner().equals(owner)) {
                return true;
            }
        }
        return false;
    }

    /** Llamado una vez por tick del servidor. Reparte el presupuesto entre los trabajos. */
    public static synchronized void tick() {
        if (TASKS.isEmpty()) {
            return;
        }
        int budget = TerraformConfig.GENERAL.maxBlocksPerTick.get();
        while (budget > 0 && !TASKS.isEmpty()) {
            EditTask task = TASKS.peekFirst();
            int used = task.tick(budget);
            budget -= used;
            if (task.isComplete()) {
                task.finish();
                TASKS.pollFirst();
            } else if (used <= 0) {
                // El trabajo no avanzo pese a tener presupuesto: evitar bucle infinito.
                break;
            }
        }
    }

    public static synchronized void clear() {
        TASKS.clear();
    }
}
