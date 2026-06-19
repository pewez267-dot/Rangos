package com.fantasticterraform.intelligent.biome;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.noise.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generador de terreno por capas de ruido, al estilo del generador moderno de Minecraft
 * pero propio y determinista. Combina:
 * <ul>
 *   <li><b>Continentalidad</b> (escala grande) para la altura base.</li>
 *   <li><b>Erosion</b> que aplana o deja montanoso.</li>
 *   <li><b>Picos/valles</b> (ruido ridged = 1 - |ruido|) para crestas afiladas donde la erosion es baja.</li>
 *   <li><b>Rios</b> (banda cercana a cero de un ruido propio) que tallan cauces hasta el nivel del mar.</li>
 *   <li><b>Humedad/Temperatura</b> + pendiente para elegir la superficie (cesped, arena, nieve, grava, piedra de acantilado...).</li>
 * </ul>
 */
public final class BiomeTerrainGenerator {

    private BiomeTerrainGenerator() {
    }

    public static void generate(ServerPlayer player, ServerLevel level, SelectionShape sel, long baseSeed,
                                double contScale, double eroScale, double moistScale, double tempScale) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - min.getX() + 1;
        int d = max.getZ() - min.getZ() + 1;
        int minY = min.getY();
        int maxY = max.getY();
        int span = Math.max(1, maxY - minY);
        int seaLevel = minY + (int) (span * 0.42D);

        ContinentalitySampler continental = new ContinentalitySampler(baseSeed, contScale);
        ErosionSampler erosion = new ErosionSampler(baseSeed, eroScale);
        MoistureSampler moisture = new MoistureSampler(baseSeed, moistScale);
        TemperatureSampler temperature = new TemperatureSampler(baseSeed, tempScale);
        PerlinNoise peaks = new PerlinNoise(baseSeed + 707L);
        PerlinNoise rivers = new PerlinNoise(baseSeed + 909L);
        double peakScale = 0.012D;
        double riverScale = 0.006D;

        int[][] height = new int[w][d];
        boolean[][] river = new boolean[w][d];
        BlockState[][] surface = new BlockState[w][d];
        BlockState[][] sub = new BlockState[w][d];

        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState packedIce = Blocks.PACKED_ICE.defaultBlockState();

        // Pasada 1: altura por columna (con rios).
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                int wx = min.getX() + ix;
                int wz = min.getZ() + iz;
                double cont = continental.normalized(wx, wz);
                double ero = erosion.normalized(wx, wz);
                double ridged = 1.0D - Math.abs(peaks.fractal2D(wx * peakScale, wz * peakScale, 4, 0.5D, 2.0D));
                double baseFrac = 0.25D + 0.50D * cont;
                double mountain = (1.0D - ero) * ridged * ridged * 0.55D;
                double frac = clamp01(baseFrac + mountain - ero * 0.12D);
                int th = minY + (int) Math.round(frac * span);

                double rv = Math.abs(rivers.fractal2D(wx * riverScale, wz * riverScale, 2, 0.5D, 2.0D));
                if (rv < 0.035D && th > seaLevel - 1) {
                    th = seaLevel - 1 - (int) ((0.035D - rv) * 40.0D);
                    river[ix][iz] = true;
                }
                height[ix][iz] = Math.max(minY, Math.min(maxY, th));
            }
        }

        // Pasada 2: superficie segun pendiente, humedad, temperatura y altura.
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                int wx = min.getX() + ix;
                int wz = min.getZ() + iz;
                int th = height[ix][iz];
                double t = temperature.normalized(wx, wz);
                double m = moisture.normalized(wx, wz);
                double frac = (double) (th - minY) / span;
                int slope = slopeAt(height, ix, iz, w, d);

                BlockState top;
                BlockState below;
                if (slope >= 4) {
                    // Acantilado: roca expuesta (o grava si frio).
                    top = t < 0.3D ? gravel : stone;
                    below = stone;
                } else if (frac > 0.82D && t < 0.5D) {
                    top = t < 0.25D ? packedIce : snow;
                    below = stone;
                } else if (t < 0.22D) {
                    top = snow;
                    below = dirt;
                } else if (t > 0.70D && m < 0.30D) {
                    top = sand;
                    below = sandstone;
                } else if (river[ix][iz] || th <= seaLevel + 1) {
                    top = m < 0.4D ? sand : gravel;
                    below = m < 0.4D ? sandstone : dirt;
                } else if (m > 0.72D) {
                    top = grass;
                    below = dirt;
                } else if (m < 0.30D) {
                    top = coarse;
                    below = dirt;
                } else {
                    top = grass;
                    below = dirt;
                }
                surface[ix][iz] = top;
                sub[ix][iz] = below;
            }
        }

        final int sea = seaLevel;
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bedrockLike = Blocks.STONE.defaultBlockState();

        StreamingEditTask.StateProvider provider = (lvl, pos) -> {
            if (!sel.contains(pos)) {
                return null;
            }
            int ix = pos.getX() - min.getX();
            int iz = pos.getZ() - min.getZ();
            if (ix < 0 || iz < 0 || ix >= w || iz >= d) {
                return null;
            }
            int th = height[ix][iz];
            int y = pos.getY();
            if (y <= th) {
                if (y == th) {
                    return surface[ix][iz];
                }
                if (y >= th - 3) {
                    return sub[ix][iz];
                }
                return bedrockLike;
            }
            if (y <= sea) {
                return water;
            }
            return air;
        };

        int total = (int) Math.min(Integer.MAX_VALUE, sel.getVolume());
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), "Biomas", total, null,
                BlockPos.betweenClosed(min, max).iterator(), provider));
    }

    private static int slopeAt(int[][] height, int ix, int iz, int w, int d) {
        int h = height[ix][iz];
        int max = 0;
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] off : n) {
            int nx = ix + off[0];
            int nz = iz + off[1];
            if (nx < 0 || nz < 0 || nx >= w || nz >= d) {
                continue;
            }
            max = Math.max(max, Math.abs(height[nx][nz] - h));
        }
        return max;
    }

    private static double clamp01(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }
}
