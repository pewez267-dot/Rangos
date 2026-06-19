package com.fantasticterraform.intelligent.population;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.ListWriteTask;
import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.biome.MoistureSampler;
import com.fantasticterraform.intelligent.biome.TemperatureSampler;
import com.fantasticterraform.intelligent.population.conditions.DistanceFromOtherCondition;
import com.fantasticterraform.intelligent.population.conditions.MoistureCondition;
import com.fantasticterraform.intelligent.population.conditions.SlopeCondition;
import com.fantasticterraform.intelligent.population.conditions.SurfaceBlockCondition;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.terrain.TerrainUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Poblamiento inteligente: coloca vegetacion, rocas y decoracion sobre el terreno
 * existente evaluando reglas por columna (condiciones AND + peso probabilistico
 * modulado por ruido de densidad). Respeta la seleccion y la cola por ticks.
 */
public final class PopulationManager {

    private PopulationManager() {
    }

    public static void populate(ServerPlayer player, ServerLevel level, SelectionShape sel, long seed,
                                boolean trees, boolean rocks, boolean vegetation, boolean crystals) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        MoistureSampler moisture = new MoistureSampler(seed, 0.02D);
        TemperatureSampler temperature = new TemperatureSampler(seed, 0.02D);
        DensityNoiseSampler density = new DensityNoiseSampler(seed, TerraformConfig.GENERAL.populationDensityScale.get());
        int minDist = TerraformConfig.GENERAL.populationMinDistance.get();

        List<PlacementRule> rules = new ArrayList<>();
        if (trees) {
            PlacementRule treeRule = new PlacementRule("trees", true, Blocks.OAK_LOG.defaultBlockState(),
                    new ArrayList<>(), 0.06D, 0.18D);
            treeRule.conditions.add(new SurfaceBlockCondition(Blocks.GRASS_BLOCK));
            treeRule.conditions.add(new SlopeCondition(2));
            treeRule.conditions.add(new DistanceFromOtherCondition(treeRule.placed, Math.max(3, minDist + 2)));
            rules.add(treeRule);
        }
        if (rocks) {
            PlacementRule rockRule = new PlacementRule("rocks", false, Blocks.COBBLESTONE.defaultBlockState(),
                    new ArrayList<>(Arrays.asList(new SlopeCondition(4))), 0.02D, 0.06D);
            rockRule.conditions.add(new DistanceFromOtherCondition(rockRule.placed, minDist));
            rules.add(rockRule);
        }
        if (vegetation) {
            PlacementRule grassRule = new PlacementRule("vegetation", false, Blocks.GRASS.defaultBlockState(),
                    new ArrayList<>(), 0.18D, 0.25D);
            grassRule.conditions.add(new SurfaceBlockCondition(Blocks.GRASS_BLOCK));
            grassRule.conditions.add(new MoistureCondition(0.35D, 1.0D));
            rules.add(grassRule);
        }
        if (crystals) {
            PlacementRule crystalRule = new PlacementRule("crystals", false, Blocks.AMETHYST_CLUSTER.defaultBlockState(),
                    new ArrayList<>(Arrays.asList(new SurfaceBlockCondition(Blocks.STONE))), 0.04D, 0.06D);
            crystalRule.conditions.add(new DistanceFromOtherCondition(crystalRule.placed, minDist));
            rules.add(crystalRule);
        }

        if (rules.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7eNo hay reglas de poblamiento activas."));
            return;
        }

        RandomSource rnd = RandomSource.create(seed ^ 0x5DEECE66DL);
        List<Placement> out = new ArrayList<>();

        for (int ix = 0; ix < hm.width; ix++) {
            for (int iz = 0; iz < hm.depth; iz++) {
                if (!hm.hasColumn(ix, iz)) {
                    continue;
                }
                int surfaceY = hm.height[ix][iz];
                int wx = hm.minX + ix;
                int wz = hm.minZ + iz;
                BlockPos surfacePos = new BlockPos(wx, surfaceY, wz);
                double slope = slopeAt(hm, ix, iz);
                double m = moisture.normalized(wx, wz);
                double t = temperature.normalized(wx, wz);
                double dens = density.normalized(wx, wz);

                for (PlacementRule rule : rules) {
                    if (!rule.applies(surfacePos, level, slope, surfaceY, m, t)) {
                        continue;
                    }
                    if (rnd.nextDouble() >= rule.finalWeight(dens)) {
                        continue;
                    }
                    BlockPos base = surfacePos.above();
                    if (rule.tree) {
                        buildTree(out, sel, base, rnd);
                    } else {
                        addIfInside(out, sel, base, rule.toPlace);
                    }
                    rule.placed.add(surfacePos);
                    break;
                }
            }
        }

        if (out.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7eNinguna regla coincidio con el terreno (revisa que haya cesped/piedra en superficie)."));
            return;
        }
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(), "Poblar", null, out, true));
    }

    private static double slopeAt(TerrainUtil.Heightmap hm, int ix, int iz) {
        int h = hm.height[ix][iz];
        int max = 0;
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : n) {
            int nx = ix + d[0];
            int nz = iz + d[1];
            if (nx < 0 || nz < 0 || nx >= hm.width || nz >= hm.depth || !hm.hasColumn(nx, nz)) {
                continue;
            }
            max = Math.max(max, Math.abs(hm.height[nx][nz] - h));
        }
        return max;
    }

    private static void buildTree(List<Placement> out, SelectionShape sel, BlockPos base, RandomSource rnd) {
        int trunk = 4 + rnd.nextInt(3);
        for (int i = 0; i < trunk; i++) {
            addIfInside(out, sel, base.above(i), Blocks.OAK_LOG.defaultBlockState());
        }
        BlockPos top = base.above(trunk - 1);
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz + dy * dy > r * r + 1) {
                        continue;
                    }
                    BlockPos leaf = top.offset(dx, dy, dz);
                    if (dx == 0 && dz == 0 && dy <= 0) {
                        continue;
                    }
                    addIfInside(out, sel, leaf, Blocks.OAK_LEAVES.defaultBlockState());
                }
            }
        }
    }

    private static void addIfInside(List<Placement> out, SelectionShape sel, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (sel.contains(pos)) {
            out.add(Placement.of(pos.immutable(), state));
        }
    }
}
