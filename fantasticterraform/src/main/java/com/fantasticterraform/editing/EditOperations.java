package com.fantasticterraform.editing;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.shapes.FillShape;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operaciones basicas de edicion (Modulo 3). Todas validan el limite de volumen y la
 * contencion en la seleccion activa, y se ejecutan a traves de {@link BlockChangeQueue}.
 */
public final class EditOperations {

    private EditOperations() {
    }

    public static boolean checkVolume(ServerPlayer player, SelectionShape sel) {
        long max = TerraformConfig.GENERAL.maxSelectionVolume.get();
        long vol = sel.getVolume();
        if (vol > max) {
            player.sendSystemMessage(Component.literal("\u00a7cLa seleccion (" + vol
                    + " bloques) excede el maximo permitido (" + max + ")."));
            return false;
        }
        return true;
    }

    /** Rellena toda la seleccion con un bloque. */
    public static void fill(ServerPlayer player, ServerLevel level, SelectionShape sel, BlockState state, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> sel.contains(pos) ? state : null;
        enqueue(level, player, "Rellenar", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /** Vacia la seleccion (rellena con aire). */
    public static void clear(ServerPlayer player, ServerLevel level, SelectionShape sel, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> sel.contains(pos) ? air : null;
        enqueue(level, player, "Vaciar", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /** Sustituye {@code from} por {@code to} dentro de la seleccion. */
    public static void replace(ServerPlayer player, ServerLevel level, SelectionShape sel,
                               BlockState from, BlockState to, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            return lvl.getBlockState(pos).is(from.getBlock()) ? to : null;
        };
        enqueue(level, player, "Reemplazar", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /**
     * Rellena una forma adicional (esfera/cilindro/piramide) recortada al volumen de
     * la seleccion activa: nunca edita fuera de la seleccion.
     */
    public static void fillShape(ServerPlayer player, ServerLevel level, SelectionShape sel,
                                 FillShape shape, BlockState state, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        BlockPos min = maxOf(shape.getMin(), sel.getMin());
        BlockPos max = minOf(shape.getMax(), sel.getMax());
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            player.sendSystemMessage(Component.literal("\u00a7cLa forma no intersecta la seleccion activa."));
            return;
        }
        Iterator<BlockPos> it = boxIterator(min, max);
        StreamingEditTask.StateProvider provider =
                (lvl, pos) -> (shape.contains(pos) && sel.contains(pos)) ? state : null;
        enqueue(level, player, "Forma", boxCount(min, max), mask, it, provider);
    }

    /** Copia el contenido real de la seleccion (respetando contains) al portapapeles. */
    public static int copy(ServerPlayer player, ServerLevel level, SelectionShape sel) {
        if (!checkVolume(player, sel)) {
            return 0;
        }
        BlockPos origin = sel.getMin();
        List<ClipboardManager.Entry> entries = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(sel.getMin(), sel.getMax())) {
            if (!sel.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            CompoundTag nbt = null;
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                nbt = be.saveWithFullMetadata();
            }
            entries.add(new ClipboardManager.Entry(pos.subtract(origin), state, nbt));
        }
        ClipboardManager.set(player.getUUID(), new ClipboardManager.Clipboard(entries));
        return entries.size();
    }

    /** Pega el portapapeles en {@code origin} con la rotacion indicada. */
    public static boolean paste(ServerPlayer player, ServerLevel level, BlockPos origin, Rotation rotation, Mask mask) {
        ClipboardManager.Clipboard clip = ClipboardManager.get(player.getUUID());
        if (clip == null || clip.size() == 0) {
            player.sendSystemMessage(Component.literal("\u00a7cEl portapapeles esta vacio. Copia algo primero."));
            return false;
        }
        List<Placement> placements = clip.toPlacements(origin, rotation);
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(), "Pegar", mask, placements, true));
        return true;
    }

    /**
     * Mueve el contenido de la seleccion a un offset y limpia el origen. Se toma una
     * instantanea del origen y se aplican destino + limpieza como una sola operacion.
     */
    public static void move(ServerPlayer player, ServerLevel level, SelectionShape sel, BlockPos offset, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();

        // Instantanea del contenido del origen.
        List<BlockPos> sources = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        List<CompoundTag> nbts = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(sel.getMin(), sel.getMax())) {
            if (!sel.contains(pos)) {
                continue;
            }
            BlockPos p = pos.immutable();
            sources.add(p);
            states.add(level.getBlockState(p));
            BlockEntity be = level.getBlockEntity(p);
            nbts.add(be != null ? be.saveWithFullMetadata() : null);
        }

        Map<BlockPos, Placement> finalMap = new LinkedHashMap<>();
        // 1) Limpiar todo el origen.
        for (BlockPos src : sources) {
            finalMap.put(src, Placement.of(src, air));
        }
        // 2) Escribir el contenido movido (sobrescribe la limpieza donde solapen).
        for (int i = 0; i < sources.size(); i++) {
            BlockPos dest = sources.get(i).offset(offset);
            finalMap.put(dest, new Placement(dest, states.get(i), nbts.get(i)));
        }

        List<Placement> placements = new ArrayList<>(finalMap.values());
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(), "Mover", mask, placements, true));
    }

    // ----- helpers -----

    private static void enqueue(ServerLevel level, ServerPlayer player, String name, int total, Mask mask,
                                Iterator<BlockPos> it, StreamingEditTask.StateProvider provider) {
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), name, total, mask, it, provider));
    }

    private static Iterator<BlockPos> boxIterator(BlockPos min, BlockPos max) {
        return BlockPos.betweenClosed(min, max).iterator();
    }

    private static int boxCount(BlockPos min, BlockPos max) {
        long c = (long) (max.getX() - min.getX() + 1)
                * (long) (max.getY() - min.getY() + 1)
                * (long) (max.getZ() - min.getZ() + 1);
        return (int) Math.min(Integer.MAX_VALUE, c);
    }

    private static BlockPos maxOf(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static BlockPos minOf(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }
}
