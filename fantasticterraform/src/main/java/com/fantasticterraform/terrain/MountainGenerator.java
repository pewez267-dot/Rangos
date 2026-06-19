package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.noise.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generacion procedural de montanas mediante ruido 2D (heightmap Perlin fractal,
 * determinista). Para cada columna se calcula una altura objetivo y se rellena desde
 * la base de la seleccion hasta esa altura, con superficie/subsuelo/piedra.
 */
public final class MountainGenerator {

    private MountainGenerator() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             double amplitude, double frequency, int octaves, long seed,
                             BlockState surfaceBlock, BlockState dirtBlock, BlockState stoneBlock, Mask mask) {
        apply(player, level, sel, amplitude, frequency, octaves, 0, seed, surfaceBlock, dirtBlock, stoneBlock, mask);
    }

    /**
     * Variante con modo de ruido: 0 = FBM (colinas suaves), 1 = RIDGED (crestas y sierras
     * afiladas), 2 = BILLOW (lomas redondeadas y abultadas).
     */
    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             double amplitude, double frequency, int octaves, int noiseMode, long seed,
                             BlockState surfaceBlock, BlockState dirtBlock, BlockState stoneBlock, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        PerlinNoise noise = new PerlinNoise(seed);
        int baseY = sel.getMin().getY();
        int minX = sel.getMin().getX();
        int minZ = sel.getMin().getZ();
        int w = sel.getMax().getX() - minX + 1;
        int d = sel.getMax().getZ() - minZ + 1;
        double freq = frequency <= 0 ? 0.05D : frequency;
        int oct = Math.max(1, Math.min(8, octaves));

        int[][] genHeight = new int[w][d];
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                double n = noise.fractal2D((minX + ix) * freq, (minZ + iz) * freq, oct, 0.5D, 2.0D);
                double normalized = shape(n, noiseMode);
                genHeight[ix][iz] = baseY + (int) Math.round(amplitude * normalized);
            }
        }

        int dirtDepth = 4;
        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            int ix = pos.getX() - minX;
            int iz = pos.getZ() - minZ;
            if (ix < 0 || iz < 0 || ix >= w || iz >= d) {
                return null;
            }
            int top = genHeight[ix][iz];
            if (pos.getY() > top) {
                return null; // no talla lo que ya existe por encima.
            }
            if (!lvl.getBlockState(pos).isAir()) {
                return null; // conserva el solido existente, solo rellena huecos.
            }
            int depth = top - pos.getY();
            if (depth == 0) {
                return surfaceBlock;
            }
            if (depth <= dirtDepth) {
                return dirtBlock;
            }
            return stoneBlock;
        };

        int total = (int) Math.min(Integer.MAX_VALUE, sel.getVolume());
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), "Montanas", total, mask,
                BlockPos.betweenClosed(sel.getMin(), sel.getMax()).iterator(), provider));
    }

    /** Convierte el ruido [-1,1] en una altura normalizada [0,1] segun el modo. */
    private static double shape(double n, int mode) {
        switch (mode) {
            case 1: { // RIDGED: crestas afiladas
                double r = 1.0D - Math.abs(n);
                return r * r;
            }
            case 2: // BILLOW: lomas redondeadas
                return Math.abs(n);
            case 0:
            default: // FBM
                return (n + 1.0D) * 0.5D;
        }
    }
}
