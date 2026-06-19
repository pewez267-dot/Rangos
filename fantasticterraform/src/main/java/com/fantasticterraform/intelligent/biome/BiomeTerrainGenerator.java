package com.fantasticterraform.intelligent.biome;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Genera terreno coherente combinando cuatro capas de ruido independientes
 * (continentalidad, erosion, humedad, temperatura), como el principio del generador de
 * mundo moderno. Produce una altura y un tipo de superficie por columna XZ dentro de
 * la seleccion, con nivel del mar para lagos. Se aplica por la cola por ticks.
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
        int seaLevel = minY + (int) ((maxY - minY) * 0.40D);

        ContinentalitySampler continental = new ContinentalitySampler(baseSeed, contScale);
        ErosionSampler erosion = new ErosionSampler(baseSeed, eroScale);
        MoistureSampler moisture = new MoistureSampler(baseSeed, moistScale);
        TemperatureSampler temperature = new TemperatureSampler(baseSeed, tempScale);

        int[][] height = new int[w][d];
        BlockState[][] surface = new BlockState[w][d];
        BlockState[][] sub = new BlockState[w][d];

        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();

        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                int wx = min.getX() + ix;
                int wz = min.getZ() + iz;
                double cont = continental.normalized(wx, wz);
                double ero = erosion.normalized(wx, wz);
                double hf = cont * (0.45D + 0.55D * (1.0D - ero));
                int th = minY + (int) Math.round(hf * (maxY - minY));
                th = Math.max(minY, Math.min(maxY, th));
                height[ix][iz] = th;

                double t = temperature.normalized(wx, wz);
                double m = moisture.normalized(wx, wz);
                if (t < 0.30D) {
                    surface[ix][iz] = snow;
                    sub[ix][iz] = dirt;
                } else if (m < 0.30D && t > 0.55D) {
                    surface[ix][iz] = sand;
                    sub[ix][iz] = sandstone;
                } else if (th <= seaLevel + 1) {
                    surface[ix][iz] = sand;
                    sub[ix][iz] = dirt;
                } else {
                    surface[ix][iz] = grass;
                    sub[ix][iz] = dirt;
                }
            }
        }

        final int sea = seaLevel;
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

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
                return stone;
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
}
