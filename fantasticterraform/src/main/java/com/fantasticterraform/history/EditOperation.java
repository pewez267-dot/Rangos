package com.fantasticterraform.history;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Una operacion de edicion atomica: el conjunto de cambios de bloque que produjo una
 * sola accion del jugador. Es la unidad de deshacer/rehacer.
 */
public final class EditOperation {

    public final UUID playerId;
    public final long timestamp;
    public final ResourceKey<Level> dimension;
    public final String label;
    private final List<BlockChange> changes = new ArrayList<>();
    private boolean truncated;

    public EditOperation(UUID playerId, ResourceKey<Level> dimension, String label) {
        this.playerId = playerId;
        this.dimension = dimension;
        this.label = label;
        this.timestamp = System.currentTimeMillis();
    }

    public List<BlockChange> changes() {
        return changes;
    }

    public void add(BlockChange change) {
        changes.add(change);
    }

    public int size() {
        return changes.size();
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /** Indica que la operacion supero el limite de historial y no se grabo completa. */
    public boolean isTruncated() {
        return truncated;
    }

    public void markTruncated() {
        this.truncated = true;
    }
}
