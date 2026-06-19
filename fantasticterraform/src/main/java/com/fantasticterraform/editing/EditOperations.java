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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
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
        return paste(player, level, origin, rotation, false, false, false, 1, mask);
    }

    /** Pega el portapapeles con transformacion completa (rotacion Y, espejo X/Y/Z, escala). */
    public static boolean paste(ServerPlayer player, ServerLevel level, BlockPos origin, Rotation rotation,
                                boolean mirrorX, boolean mirrorY, boolean mirrorZ, int scale, Mask mask) {
        ClipboardManager.Clipboard clip = ClipboardManager.get(player.getUUID());
        if (clip == null || clip.size() == 0) {
            player.sendSystemMessage(Component.literal("\u00a7cEl portapapeles esta vacio. Copia algo primero."));
            return false;
        }
        List<Placement> placements = clip.toPlacements(origin, rotation, mirrorX, mirrorY, mirrorZ, scale);
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

    // ----- operaciones avanzadas (//hollow //walls //stack //smooth3D //replace patron) -----

    /** Rellena la seleccion con un PATRON ponderado (mezcla aleatoria coherente). */
    public static void fillPattern(ServerPlayer player, ServerLevel level, SelectionShape sel,
                                   BlockPattern pattern, long seed, Mask mask) {
        if (!checkVolume(player, sel) || pattern == null) {
            return;
        }
        RandomSource rng = RandomSource.create(seed);
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> sel.contains(pos) ? pattern.pick(rng) : null;
        enqueue(level, player, "Rellenar patron", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /** Reemplaza {@code from} (o cualquier solido si {@code from==null}) por un PATRON. */
    public static void replacePattern(ServerPlayer player, ServerLevel level, SelectionShape sel,
                                      BlockState from, BlockPattern pattern, long seed, Mask mask) {
        if (!checkVolume(player, sel) || pattern == null) {
            return;
        }
        RandomSource rng = RandomSource.create(seed);
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            BlockState cur = lvl.getBlockState(pos);
            boolean match = (from == null) ? !cur.isAir() : cur.is(from.getBlock());
            return match ? pattern.pick(rng) : null;
        };
        enqueue(level, player, "Reemplazar patron", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /** Hueca la seleccion: vacia los bloques interiores (rodeados por solido en los 6 lados). */
    public static void hollow(ServerPlayer player, ServerLevel level, SelectionShape sel, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos) || lvl.getBlockState(pos).isAir()) {
                return null;
            }
            for (int[] o : FACE6) {
                BlockPos n = pos.offset(o[0], o[1], o[2]);
                if (!sel.contains(n) || lvl.getBlockState(n).isAir()) {
                    return null; // tiene una cara expuesta: es cascara, conservar.
                }
            }
            return air; // totalmente rodeado: interior, vaciar.
        };
        enqueue(level, player, "Huecar", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /** Construye muros verticales en el contorno horizontal de la seleccion. */
    public static void walls(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             BlockPattern pattern, long seed, Mask mask) {
        if (!checkVolume(player, sel) || pattern == null) {
            return;
        }
        RandomSource rng = RandomSource.create(seed);
        Iterator<BlockPos> it = boxIterator(sel.getMin(), sel.getMax());
        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            boolean edge = !sel.contains(pos.offset(1, 0, 0)) || !sel.contains(pos.offset(-1, 0, 0))
                    || !sel.contains(pos.offset(0, 0, 1)) || !sel.contains(pos.offset(0, 0, -1));
            return edge ? pattern.pick(rng) : null;
        };
        enqueue(level, player, "Muros", boxCount(sel.getMin(), sel.getMax()), mask, it, provider);
    }

    /** Repite el contenido de la seleccion {@code count} veces a lo largo de un eje. */
    public static void stack(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             int axis, int sign, int count, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        int n = Math.max(1, Math.min(64, count));
        int s = sign >= 0 ? 1 : -1;
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        int stepX = axis == 0 ? sizeX * s : 0;
        int stepY = axis == 1 ? sizeY * s : 0;
        int stepZ = axis == 2 ? sizeZ * s : 0;

        // Instantanea del contenido real.
        List<BlockPos> srcPos = new ArrayList<>();
        List<BlockState> srcState = new ArrayList<>();
        List<CompoundTag> srcNbt = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!sel.contains(pos)) {
                continue;
            }
            BlockPos p = pos.immutable();
            srcPos.add(p);
            srcState.add(level.getBlockState(p));
            BlockEntity be = level.getBlockEntity(p);
            srcNbt.add(be != null ? be.saveWithFullMetadata() : null);
        }

        List<Placement> placements = new ArrayList<>(srcPos.size() * n);
        for (int k = 1; k <= n; k++) {
            int ox = stepX * k;
            int oy = stepY * k;
            int oz = stepZ * k;
            for (int i = 0; i < srcPos.size(); i++) {
                BlockPos dest = srcPos.get(i).offset(ox, oy, oz);
                placements.add(new Placement(dest, srcState.get(i), srcNbt.get(i)));
            }
        }
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(), "Apilar", mask, placements, true));
    }

    /**
     * Suavizado 3D REAL (no heightmap): regla de mayoria sobre el vecindario 26 en un
     * grid instantaneo, iterado {@code passes} veces. Funde salientes y rellena
     * concavidades en cualquier orientacion (cuevas, arcos, voladizos), no solo la
     * superficie. Solo modifica bloques dentro de la seleccion.
     */
    public static void smooth3D(ServerPlayer player, ServerLevel level, SelectionShape sel, int passes, Mask mask) {
        if (!checkVolume(player, sel)) {
            return;
        }
        int n = Math.max(1, Math.min(6, passes));
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - min.getX() + 1;
        int h = max.getY() - min.getY() + 1;
        int d = max.getZ() - min.getZ() + 1;

        boolean[][][] solid = new boolean[w][h][d];
        BlockState[][][] state = new BlockState[w][h][d];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    BlockState bs = level.getBlockState(cursor.set(min.getX() + x, min.getY() + y, min.getZ() + z));
                    solid[x][y][z] = !bs.isAir();
                    state[x][y][z] = bs;
                }
            }
        }

        boolean[][][] orig = deepCopy(solid, w, h, d);
        for (int pass = 0; pass < n; pass++) {
            boolean[][][] next = deepCopy(solid, w, h, d);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        if (!sel.contains(cursor.set(min.getX() + x, min.getY() + y, min.getZ() + z))) {
                            continue; // fuera del volumen real: no se altera
                        }
                        int count = 0;
                        for (int[] o : NEIGH26) {
                            int nx = x + o[0];
                            int ny = y + o[1];
                            int nz = z + o[2];
                            boolean sv = (nx < 0 || ny < 0 || nz < 0 || nx >= w || ny >= h || nz >= d)
                                    ? solid[x][y][z] : solid[nx][ny][nz];
                            if (sv) {
                                count++;
                            }
                        }
                        next[x][y][z] = count >= 14; // mayoria de 26
                    }
                }
            }
            solid = next;
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        List<Placement> placements = new ArrayList<>();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    if (solid[x][y][z] == orig[x][y][z]) {
                        continue;
                    }
                    BlockPos p = new BlockPos(min.getX() + x, min.getY() + y, min.getZ() + z);
                    if (!sel.contains(p)) {
                        continue;
                    }
                    if (!solid[x][y][z]) {
                        placements.add(Placement.of(p, air));
                    } else {
                        placements.add(Placement.of(p, representative(orig, state, w, h, d, x, y, z)));
                    }
                }
            }
        }
        if (placements.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7eEl suavizado 3D no cambio nada aqui."));
            return;
        }
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(), "Suavizado 3D", mask, placements, true));
    }

    private static boolean[][][] deepCopy(boolean[][][] src, int w, int h, int d) {
        boolean[][][] out = new boolean[w][h][d];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                System.arraycopy(src[x][y], 0, out[x][y], 0, d);
            }
        }
        return out;
    }

    /** Bloque representativo para una celda que pasa a solida: el solido original mas comun a su alrededor. */
    private static BlockState representative(boolean[][][] origSolid, BlockState[][][] state,
                                             int w, int h, int d, int x, int y, int z) {
        Map<Block, Integer> votes = new HashMap<>();
        for (int[] o : NEIGH26) {
            int nx = x + o[0];
            int ny = y + o[1];
            int nz = z + o[2];
            if (nx < 0 || ny < 0 || nz < 0 || nx >= w || ny >= h || nz >= d) {
                continue;
            }
            if (origSolid[nx][ny][nz] && state[nx][ny][nz] != null) {
                votes.merge(state[nx][ny][nz].getBlock(), 1, Integer::sum);
            }
        }
        Block best = null;
        int bestCount = 0;
        for (Map.Entry<Block, Integer> e : votes.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return best != null ? best.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    private static final int[][] FACE6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final int[][] NEIGH26 = buildNeigh26();

    private static int[][] buildNeigh26() {
        int[][] a = new int[26][3];
        int k = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    a[k][0] = dx;
                    a[k][1] = dy;
                    a[k][2] = dz;
                    k++;
                }
            }
        }
        return a;
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
