package com.fantasticterraform.intelligent.biome;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.StreamingEditTask;
import com.fantasticterraform.intelligent.population.PopulationManager;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.noise.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generador de terreno por capas de ruido, personalizable y determinista. El estilo de
 * relieve (llano, colinas, montanas, canon, islas), la amplitud, el nivel del mar y la
 * escala de las formas los elige el usuario. El acabado de superficie puede ser:
 * <ul>
 *   <li><b>Automatico por clima</b>: cada columna recibe un bioma elegido por el espacio
 *       temperatura x humedad (Whittaker) -> distintos biomas emergen en la misma region.</li>
 *   <li><b>Bioma forzado</b>: el usuario elige un bioma concreto y todo usa su paleta.</li>
 *   <li><b>Personalizado</b>: el usuario fija superficie/subsuelo/roca a mano.</li>
 * </ul>
 * Opcionalmente, al terminar el terreno se puebla automaticamente segun el bioma.
 */
public final class BiomeTerrainGenerator {

    public static final int STYLE_PLAINS = 0;
    public static final int STYLE_HILLS = 1;
    public static final int STYLE_MOUNTAINS = 2;
    public static final int STYLE_CANYON = 3;
    public static final int STYLE_ISLANDS = 4;

    private BiomeTerrainGenerator() {
    }

    public static void generate(ServerPlayer player, ServerLevel level, SelectionShape sel, long baseSeed,
                                int style, double featureScale, double amplitude, double seaFraction,
                                boolean useCustom, BlockState customSurface, BlockState customSub, BlockState customStone,
                                int forcedBiomeIndex, boolean autoPopulate) {
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
        int seaLevel = minY + (int) (span * clamp(seaFraction, 0.05D, 0.9D));
        double ampMul = 0.35D + clamp01(amplitude) * 1.85D;
        double fScale = featureScale > 0 ? featureScale : 0.006D;

        BiomeType[] all = BiomeType.values();
        BiomeType forced = (forcedBiomeIndex >= 0 && forcedBiomeIndex < all.length) ? all[forcedBiomeIndex] : null;

        ContinentalitySampler continental = new ContinentalitySampler(baseSeed, fScale);
        ErosionSampler erosion = new ErosionSampler(baseSeed, fScale * 2.5D);
        MoistureSampler moisture = new MoistureSampler(baseSeed, 0.030D);
        TemperatureSampler temperature = new TemperatureSampler(baseSeed, 0.030D);
        PerlinNoise peaks = new PerlinNoise(baseSeed + 707L);
        PerlinNoise rivers = new PerlinNoise(baseSeed + 909L);

        int[][] height = new int[w][d];
        boolean[][] river = new boolean[w][d];
        boolean[][] ocean = new boolean[w][d];
        BlockState[][] surface = new BlockState[w][d];
        BlockState[][] sub = new BlockState[w][d];

        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        // Pasada 1: alturas por columna segun el estilo.
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                int wx = min.getX() + ix;
                int wz = min.getZ() + iz;
                double cont = continental.normalized(wx, wz);
                double ero = erosion.normalized(wx, wz);
                double ridged = 1.0D - Math.abs(peaks.fractal2D(wx * fScale * 2.0D, wz * fScale * 2.0D, 4, 0.5D, 2.0D));

                double frac;
                switch (style) {
                    case STYLE_PLAINS:
                        frac = 0.30D + 0.10D * cont + 0.04D * ridged;
                        break;
                    case STYLE_MOUNTAINS:
                        frac = 0.22D + 0.40D * cont + (1.0D - ero) * ridged * ridged * 0.70D;
                        break;
                    case STYLE_CANYON:
                        frac = 0.62D + 0.18D * cont;
                        break;
                    case STYLE_ISLANDS:
                        if (cont < 0.46D) {
                            ocean[ix][iz] = true;
                            frac = 0.10D + cont * 0.30D;
                        } else {
                            frac = 0.42D + (cont - 0.46D) * 0.9D + ridged * 0.12D;
                        }
                        break;
                    case STYLE_HILLS:
                    default:
                        frac = 0.30D + 0.34D * cont + (1.0D - ero) * ridged * 0.18D;
                        break;
                }

                double seaFrac = (double) (seaLevel - minY) / span;
                double dev = (frac - seaFrac) * ampMul;
                int th = seaLevel + (int) Math.round(dev * span);

                double rv = Math.abs(rivers.fractal2D(wx * 0.006D, wz * 0.006D, 2, 0.5D, 2.0D));
                // Solo el estilo Canon talla cauces; los demas estilos NO generan rios (evita el "rio de piedra").
                double riverWidth = style == STYLE_CANYON ? 0.07D : 0.0D;
                if (!ocean[ix][iz] && riverWidth > 0 && rv < riverWidth && th > seaLevel - 1) {
                    th = seaLevel - 1 - (int) ((riverWidth - rv) / riverWidth * 24);
                    river[ix][iz] = true;
                }
                height[ix][iz] = Math.max(minY, Math.min(maxY, th));
            }
        }

        // Pasada 2: bioma + acabado de superficie.
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < d; iz++) {
                int th = height[ix][iz];
                int slope = slopeAt(height, ix, iz, w, d);
                if (useCustom) {
                    surface[ix][iz] = slope >= 4 ? customStone : customSurface;
                    sub[ix][iz] = customSub;
                    continue;
                }
                int wx = min.getX() + ix;
                int wz = min.getZ() + iz;
                double t = temperature.normalized(wx, wz);
                double m = moisture.normalized(wx, wz);
                double frac = (double) (th - minY) / span;

                BiomeType biome = forced != null ? forced : BiomeSelector.pick(t, m);

                BlockState top;
                BlockState below;
                if (slope >= 4) {
                    // Acantilados: roca expuesta en cualquier bioma.
                    top = t < 0.3D ? gravel : stone;
                    below = stone;
                } else if (forced != null) {
                    // Bioma forzado: uniforme y reconocible (desierto=arena, nevada=nieve, jungla=cesped...).
                    top = forced.surface();
                    below = forced.sub();
                } else if (frac > 0.85D && t < 0.5D) {
                    top = t < 0.25D ? ice : snow;
                    below = stone;
                } else if (!ocean[ix][iz] && th <= seaLevel + 1 && biome != BiomeType.SNOWY_PLAINS) {
                    top = sand;
                    below = sandstone;
                } else {
                    top = biome.surface();
                    below = biome.sub();
                }
                surface[ix][iz] = top;
                sub[ix][iz] = below;
            }
        }

        final int sea = seaLevel;
        final BlockState deep = useCustom ? customStone : stone;
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
                return deep;
            }
            if (y <= sea) {
                return water;
            }
            return air;
        };

        final int popMask = autoPopulate
                ? (forced != null ? forced.populationMask()
                : (PopulationManager.TREES | PopulationManager.FLOWERS | PopulationManager.GRASS
                | PopulationManager.MUSHROOMS | PopulationManager.DESERT | PopulationManager.WATER
                | PopulationManager.ROCKS))
                : 0;
        Runnable onFinish = () -> {
            if (popMask != 0) {
                PopulationManager.populate(player, level, sel, baseSeed, popMask);
            }
        };

        int total = (int) Math.min(Integer.MAX_VALUE, sel.getVolume());
        BlockChangeQueue.enqueue(new StreamingEditTask(level, player.getUUID(), "Biomas", total, null,
                BlockPos.betweenClosed(min, max).iterator(), provider, onFinish));
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

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
