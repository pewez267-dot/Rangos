package com.fantasticterraform.editing;

import com.fantasticterraform.masks.Mask;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

/**
 * Trabajo que aplica una lista precalculada de {@link Placement}s. Se usa cuando los
 * datos de origen deben leerse antes de escribir (mover, copiar/pegar, pegar
 * schematic) o cuando se reproduce un historial (deshacer/rehacer).
 */
public final class ListWriteTask extends AbstractEditTask {

    private final List<Placement> placements;
    private final Runnable onFinish;
    private int index;

    public ListWriteTask(ServerLevel level, UUID owner, String name, Mask mask,
                         List<Placement> placements, boolean recordHistory) {
        this(level, owner, name, mask, placements, recordHistory, null);
    }

    public ListWriteTask(ServerLevel level, UUID owner, String name, Mask mask,
                         List<Placement> placements, boolean recordHistory, Runnable onFinish) {
        super(level, owner, name, placements.size(), mask, recordHistory);
        this.placements = placements;
        this.onFinish = onFinish;
    }

    @Override
    public void finish() {
        super.finish();
        if (onFinish != null) {
            onFinish.run();
        }
    }

    @Override
    protected boolean hasNext() {
        return index < placements.size();
    }

    @Override
    protected Placement next() {
        return placements.get(index++);
    }
}
