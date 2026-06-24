package com.fantasticterraform.history;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.ListWriteTask;
import com.fantasticterraform.editing.Placement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de deshacer/rehacer por jugador. Cada operacion completada se apila; las
 * operaciones de reversion tambien pasan por la cola por ticks (no se aplican de
 * golpe) y respetan {@code history_stack_size}.
 */
public final class HistoryManager {

    private static final HistoryManager INSTANCE = new HistoryManager();

    private final Map<UUID, Deque<EditOperation>> undo = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<EditOperation>> redo = new ConcurrentHashMap<>();

    private HistoryManager() {
    }

    public static HistoryManager get() {
        return INSTANCE;
    }

    /** Registra una operacion recien aplicada. Invalida la pila de rehacer. */
    public synchronized void pushDone(EditOperation op) {
        if (op.isEmpty()) {
            return;
        }
        Deque<EditOperation> stack = undo.computeIfAbsent(op.playerId, k -> new ArrayDeque<>());
        stack.push(op);
        redo.computeIfAbsent(op.playerId, k -> new ArrayDeque<>()).clear();
        int max = TerraformConfig.GENERAL.historyStackSize.get();
        while (stack.size() > max) {
            stack.removeLast();
        }
    }

    public synchronized boolean undo(ServerPlayer player) {
        Deque<EditOperation> stack = undo.get(player.getUUID());
        if (stack == null || stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7eNada que deshacer."));
            return false;
        }
        EditOperation op = stack.pop();
        ServerLevel level = player.server.getLevel(op.dimension);
        if (level == null) {
            return false;
        }
        // Revertir en orden inverso al de aplicacion, restaurando estado y NBT anteriores.
        List<BlockChange> changes = op.changes();
        List<Placement> placements = new ArrayList<>(changes.size());
        for (int i = changes.size() - 1; i >= 0; i--) {
            BlockChange c = changes.get(i);
            placements.add(new Placement(c.pos, c.previousState, c.previousBlockEntityData));
        }
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(),
                "Deshacer (" + op.label + ")", null, placements, false));
        redo.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).push(op);
        player.sendSystemMessage(Component.literal("\u00a7aDeshaciendo \u00a7f" + op.label
                + " \u00a77(" + op.size() + " bloques)" + (op.isTruncated() ? " \u00a7c[parcial]" : "")));
        return true;
    }

    public synchronized boolean redo(ServerPlayer player) {
        Deque<EditOperation> stack = redo.get(player.getUUID());
        if (stack == null || stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7eNada que rehacer."));
            return false;
        }
        EditOperation op = stack.pop();
        ServerLevel level = player.server.getLevel(op.dimension);
        if (level == null) {
            return false;
        }
        List<BlockChange> changes = op.changes();
        List<Placement> placements = new ArrayList<>(changes.size());
        for (BlockChange c : changes) {
            placements.add(Placement.of(c.pos, c.newState));
        }
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(),
                "Rehacer (" + op.label + ")", null, placements, false));
        undo.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).push(op);
        player.sendSystemMessage(Component.literal("\u00a7aRehaciendo \u00a7f" + op.label));
        return true;
    }

    public synchronized int undoDepth(UUID id) {
        Deque<EditOperation> s = undo.get(id);
        return s == null ? 0 : s.size();
    }

    public synchronized int redoDepth(UUID id) {
        Deque<EditOperation> s = redo.get(id);
        return s == null ? 0 : s.size();
    }

    /** Etiquetas de la pila de deshacer (la cima = la mas reciente, primero). */
    public synchronized List<String> undoLabels(UUID id) {
        List<String> out = new ArrayList<>();
        Deque<EditOperation> s = undo.get(id);
        if (s != null) {
            for (EditOperation op : s) {
                out.add(op.label);
            }
        }
        return out;
    }

    /** Tamanos (bloques) correspondientes a {@link #undoLabels}. */
    public synchronized List<Integer> undoSizes(UUID id) {
        List<Integer> out = new ArrayList<>();
        Deque<EditOperation> s = undo.get(id);
        if (s != null) {
            for (EditOperation op : s) {
                out.add(op.size());
            }
        }
        return out;
    }

    public synchronized void clear(UUID id) {
        undo.remove(id);
        redo.remove(id);
    }
}
