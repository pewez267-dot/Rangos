package com.fantasticterraform.intelligent.population;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.ListWriteTask;
import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.biome.MoistureSampler;
import com.fantasticterraform.intelligent.biome.TemperatureSampler;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.TerrainUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Poblamiento inteligente y variado: arboles por clima (roble, abedul, pino, jungla,
 * acacia), flores de muchos tipos (incluidas dobles), hierba/helechos, setas, flora de
 * desierto (cactus, arbusto seco), flora de agua (cana de azucar, nenufares), rocas
 * (cantos rodados musgosos) y cristales. Cada categoria se activa por separado; la
 * distribucion usa el clima (humedad/temperatura) y un ruido de densidad para crear
 * agrupaciones naturales. Respeta la seleccion y la cola por ticks.
 */
public final class PopulationManager {

    public static final int TREES = 1;
    public static final int FLOWERS = 2;
    public static final int GRASS = 4;
    public static final int MUSHROOMS = 8;
    public static final int DESERT = 16;
    public static final int WATER = 32;
    public static final int ROCKS = 64;
    public static final int CRYSTALS = 128;

    private static final Block[] FLOWERS_SIMPLE = {
            Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET,
            Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY
    };
    private static final Block[] FLOWERS_DOUBLE = {Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY};

    private PopulationManager() {
    }

    public static void populate(ServerPlayer player, ServerLevel level, SelectionShape sel, long seed, int mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        if (mask == 0) {
            player.sendSystemMessage(Component.literal("\u00a7eNo activaste ninguna categoria de poblamiento."));
            return;
        }
        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        MoistureSampler moisture = new MoistureSampler(seed, 0.02D);
        TemperatureSampler temperature = new TemperatureSampler(seed, 0.02D);
        DensityNoiseSampler density = new DensityNoiseSampler(seed, TerraformConfig.GENERAL.populationDensityScale.get());
        RandomSource rnd = RandomSource.create(seed ^ 0x5DEECE66DL);

        Set<Long> treeGrid = new HashSet<>();
        Set<Long> cactusGrid = new HashSet<>();
        Set<Long> boulderGrid = new HashSet<>();
        List<Placement> out = new ArrayList<>();

        for (int ix = 0; ix < hm.width; ix++) {
            for (int iz = 0; iz < hm.depth; iz++) {
                if (!hm.hasColumn(ix, iz)) {
                    continue;
                }
                int wx = hm.minX + ix;
                int wz = hm.minZ + iz;
                int sy = hm.height[ix][iz];
                BlockPos surfacePos = new BlockPos(wx, sy, wz);
                BlockPos above = surfacePos.above();
                BlockState surf = level.getBlockState(surfacePos);
                double t = temperature.normalized(wx, wz);
                double m = moisture.normalized(wx, wz);
                double dens = density.normalized(wx, wz);

                // --- AGUA ---
                if (surf.is(Blocks.WATER)) {
                    if (has(mask, WATER) && rnd.nextDouble() < 0.12D + dens * 0.2D) {
                        addIfInside(out, sel, above, Blocks.LILY_PAD.defaultBlockState());
                    }
                    continue;
                }
                if (has(mask, WATER) && (surf.is(Blocks.SAND) || surf.is(Blocks.DIRT) || surf.is(Blocks.GRASS_BLOCK))
                        && nextToWater(level, surfacePos) && rnd.nextDouble() < 0.30D) {
                    int h = 1 + rnd.nextInt(3);
                    for (int i = 0; i < h; i++) {
                        addIfInside(out, sel, above.above(i), Blocks.SUGAR_CANE.defaultBlockState());
                    }
                    continue;
                }

                // --- HIERBA / CESPED ---
                if (surf.is(Blocks.GRASS_BLOCK) || surf.is(Blocks.PODZOL)) {
                    if (has(mask, TREES) && spacingOk(treeGrid, wx, wz, 5) && rnd.nextDouble() < 0.05D + dens * 0.06D) {
                        placeTree(out, sel, above, rnd, t, m);
                    } else if (has(mask, FLOWERS) && rnd.nextDouble() < 0.05D + dens * 0.12D * m) {
                        placeFlower(out, sel, above, rnd);
                    } else if (has(mask, GRASS) && rnd.nextDouble() < 0.16D + dens * 0.20D) {
                        placeGrassFeature(out, sel, above, rnd, m);
                    } else if (has(mask, MUSHROOMS) && rnd.nextDouble() < 0.02D) {
                        addIfInside(out, sel, above, (rnd.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState());
                    }
                }

                // --- DESIERTO ---
                if (surf.is(Blocks.SAND) && has(mask, DESERT)) {
                    if (spacingOk(cactusGrid, wx, wz, 3) && rnd.nextDouble() < 0.06D) {
                        int h = 1 + rnd.nextInt(3);
                        for (int i = 0; i < h; i++) {
                            addIfInside(out, sel, above.above(i), Blocks.CACTUS.defaultBlockState());
                        }
                    } else if (rnd.nextDouble() < 0.06D) {
                        addIfInside(out, sel, above, Blocks.DEAD_BUSH.defaultBlockState());
                    }
                }

                // --- ROCAS ---
                if (has(mask, ROCKS) && !surf.isAir() && spacingOk(boulderGrid, wx, wz, 6) && rnd.nextDouble() < 0.02D) {
                    placeBoulder(out, sel, surfacePos, rnd);
                }

                // --- CRISTALES ---
                if (has(mask, CRYSTALS) && surf.is(Blocks.STONE) && rnd.nextDouble() < 0.06D) {
                    addIfInside(out, sel, above, Blocks.AMETHYST_CLUSTER.defaultBlockState()
                            .setValue(BlockStateProperties.FACING, Direction.UP));
                }
            }
        }

        if (out.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7eNada que poblar: revisa que el terreno tenga cesped/arena/agua en superficie."));
            return;
        }
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(), "Poblar", null, out, true));
        player.sendSystemMessage(Component.literal("\u00a7aPoblando con " + out.size() + " elementos..."));
    }

    // ----- arboles -----

    private static void placeTree(List<Placement> out, SelectionShape sel, BlockPos base, RandomSource rnd, double t, double m) {
        if (t < 0.30D) {
            spruce(out, sel, base, rnd);
        } else if (t > 0.70D && m > 0.60D) {
            jungle(out, sel, base, rnd);
        } else if (t > 0.60D && m < 0.40D) {
            acacia(out, sel, base, rnd);
        } else if (rnd.nextBoolean()) {
            broadleaf(out, sel, base, rnd, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5 + rnd.nextInt(3), 2);
        } else {
            broadleaf(out, sel, base, rnd, Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4 + rnd.nextInt(3), 2);
        }
    }

    private static void broadleaf(List<Placement> out, SelectionShape sel, BlockPos base, RandomSource rnd,
                                  Block log, Block leaves, int trunk, int radius) {
        for (int i = 0; i < trunk; i++) {
            addIfInside(out, sel, base.above(i), log.defaultBlockState());
        }
        BlockPos top = base.above(trunk - 1);
        leafBlob(out, sel, top, radius, leaves);
        leafBlob(out, sel, top.above(), Math.max(1, radius - 1), leaves);
    }

    private static void spruce(List<Placement> out, SelectionShape sel, BlockPos base, RandomSource rnd) {
        int trunk = 6 + rnd.nextInt(4);
        for (int i = 0; i < trunk; i++) {
            addIfInside(out, sel, base.above(i), Blocks.SPRUCE_LOG.defaultBlockState());
        }
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState();
        int layers = trunk - 2;
        for (int i = 0; i < layers; i++) {
            int y = base.getY() + 2 + i;
            int r = ((layers - i) / 2);
            ring(out, sel, base.getX(), y, base.getZ(), r, leaves);
        }
        addIfInside(out, sel, base.above(trunk), leaves);
    }

    private static void jungle(List<Placement> out, SelectionShape sel, BlockPos base, RandomSource rnd) {
        int trunk = 9 + rnd.nextInt(5);
        for (int i = 0; i < trunk; i++) {
            addIfInside(out, sel, base.above(i), Blocks.JUNGLE_LOG.defaultBlockState());
        }
        BlockPos top = base.above(trunk - 1);
        leafBlob(out, sel, top, 3, Blocks.JUNGLE_LEAVES);
        leafBlob(out, sel, top.above(), 2, Blocks.JUNGLE_LEAVES);
    }

    private static void acacia(List<Placement> out, SelectionShape sel, BlockPos base, RandomSource rnd) {
        int trunk = 4 + rnd.nextInt(2);
        for (int i = 0; i < trunk; i++) {
            addIfInside(out, sel, base.above(i), Blocks.ACACIA_LOG.defaultBlockState());
        }
        int y = base.getY() + trunk;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 9) {
                    addIfInside(out, sel, new BlockPos(base.getX() + dx, y, base.getZ() + dz), Blocks.ACACIA_LEAVES.defaultBlockState());
                }
            }
        }
    }

    private static void leafBlob(List<Placement> out, SelectionShape sel, BlockPos center, int r, Block leaves) {
        BlockState state = leaves.defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz + dy * dy <= r * r + 1) {
                        addIfInside(out, sel, center.offset(dx, dy, dz), state);
                    }
                }
            }
        }
    }

    private static void ring(List<Placement> out, SelectionShape sel, int cx, int y, int cz, int r, BlockState leaves) {
        if (r <= 0) {
            addIfInside(out, sel, new BlockPos(cx, y, cz), leaves);
            return;
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r + 1) {
                    addIfInside(out, sel, new BlockPos(cx + dx, y, cz + dz), leaves);
                }
            }
        }
    }

    // ----- flora menor -----

    private static void placeFlower(List<Placement> out, SelectionShape sel, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(5) == 0) {
            placeDouble(out, sel, pos, FLOWERS_DOUBLE[rnd.nextInt(FLOWERS_DOUBLE.length)]);
        } else {
            addIfInside(out, sel, pos, FLOWERS_SIMPLE[rnd.nextInt(FLOWERS_SIMPLE.length)].defaultBlockState());
        }
    }

    private static void placeGrassFeature(List<Placement> out, SelectionShape sel, BlockPos pos, RandomSource rnd, double m) {
        int roll = rnd.nextInt(10);
        if (roll < 5) {
            addIfInside(out, sel, pos, Blocks.GRASS.defaultBlockState());
        } else if (roll < 7) {
            addIfInside(out, sel, pos, Blocks.FERN.defaultBlockState());
        } else if (roll < 9) {
            placeDouble(out, sel, pos, Blocks.TALL_GRASS);
        } else {
            placeDouble(out, sel, pos, Blocks.LARGE_FERN);
        }
    }

    private static void placeDouble(List<Placement> out, SelectionShape sel, BlockPos base, Block plant) {
        addIfInside(out, sel, base, plant.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        addIfInside(out, sel, base.above(), plant.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }

    private static void placeBoulder(List<Placement> out, SelectionShape sel, BlockPos surface, RandomSource rnd) {
        int r = 1 + rnd.nextInt(2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = 0; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= r * r + 1) {
                        BlockState rock = rnd.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                : (rnd.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState());
                        addIfInside(out, sel, surface.offset(dx, dy, dz), rock);
                    }
                }
            }
        }
    }

    // ----- helpers -----

    private static boolean has(int mask, int bit) {
        return (mask & bit) != 0;
    }

    private static boolean nextToWater(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean spacingOk(Set<Long> grid, int x, int z, int cell) {
        long key = ((long) Math.floorDiv(x, cell) << 32) | (Math.floorDiv(z, cell) & 0xFFFFFFFFL);
        return grid.add(key);
    }

    private static void addIfInside(List<Placement> out, SelectionShape sel, BlockPos pos, BlockState state) {
        if (sel.contains(pos)) {
            out.add(Placement.of(pos.immutable(), state));
        }
    }
}
