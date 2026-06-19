package com.fantasticterraform.intelligent.dungeon;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Genera el grafo de la dungeon: numero de salas segun tier y volumen, packing en el
 * espacio, asignacion de tipos de sala y conexion mediante arbol de expansion minima
 * (MST) mas aristas extra para crear loops.
 */
public final class GraphGenerator {

    private GraphGenerator() {
    }

    public static DungeonGraph generate(DungeonConfig cfg, SelectionShape sel) {
        DungeonSizeRequirement req = DungeonSizeRequirement.forTier(cfg.tier);
        RandomSource rnd = RandomSource.create(cfg.seed);

        int desired = req.minRooms + rnd.nextInt(Math.max(1, req.maxRooms - req.minRooms + 1));
        // Escalar por volumen disponible (aprox. una sala por cada ~2000 bloques).
        int byVolume = (int) Math.max(req.minRooms, Math.min(req.maxRooms, sel.getVolume() / 2000L));
        desired = Math.max(req.minRooms, Math.min(desired, byVolume));

        int levels = 1;
        if (cfg.multiLevel && cfg.tier.supportsMultiLevel()) {
            int maxByConfig = TerraformConfig.GENERAL.dungeonMaxLevels.get();
            int maxByHeight = Math.max(1, (sel.getMax().getY() - sel.getMin().getY() + 1) / 10);
            levels = Math.max(1, Math.min(Math.min(cfg.levels, maxByConfig), maxByHeight));
        }

        int maxAttempts = TerraformConfig.GENERAL.dungeonMaxPackingAttempts.get();
        int minSize = 7;
        int maxSize = 14;
        switch (cfg.theme.id()) {
            case "catacombs":
                minSize = 5;
                maxSize = 9;
                break;
            case "ruined_fortress":
                minSize = 11;
                maxSize = 20;
                break;
            case "ancient_crypt":
                minSize = 8;
                maxSize = 13;
                break;
            default:
                break;
        }
        List<Room> rooms = RoomPacker.pack(sel, desired, levels, cfg.seed, maxAttempts, minSize, maxSize);

        DungeonGraph graph = new DungeonGraph();
        graph.rooms.addAll(rooms);
        if (rooms.size() < 2) {
            return graph;
        }

        assignTypes(graph, cfg, rnd);
        connectMst(graph);
        addLoops(graph, cfg, rnd);
        return graph;
    }

    private static void assignTypes(DungeonGraph graph, DungeonConfig cfg, RandomSource rnd) {
        List<Room> rooms = graph.rooms;
        rooms.get(0).type = RoomType.ENTRANCE;
        int bossIndex = -1;
        if (cfg.bossEnabled) {
            // El jefe va en la sala mas lejana a la entrada.
            double best = -1;
            BlockPos entrance = rooms.get(0).center();
            for (int i = 1; i < rooms.size(); i++) {
                double d = rooms.get(i).center().distSqr(entrance);
                if (d > best) {
                    best = d;
                    bossIndex = i;
                }
            }
            if (bossIndex >= 0) {
                rooms.get(bossIndex).type = RoomType.BOSS;
            }
        }
        for (int i = 1; i < rooms.size(); i++) {
            if (i == bossIndex) {
                continue;
            }
            rooms.get(i).type = rnd.nextDouble() < 0.22 ? RoomType.TREASURE : RoomType.NORMAL;
        }
    }

    private static void connectMst(DungeonGraph graph) {
        int n = graph.rooms.size();
        boolean[] inTree = new boolean[n];
        inTree[0] = true;
        int connected = 1;
        while (connected < n) {
            int bestA = -1;
            int bestB = -1;
            double bestDist = Double.MAX_VALUE;
            for (int a = 0; a < n; a++) {
                if (!inTree[a]) {
                    continue;
                }
                for (int b = 0; b < n; b++) {
                    if (inTree[b]) {
                        continue;
                    }
                    double d = graph.rooms.get(a).center().distSqr(graph.rooms.get(b).center());
                    if (d < bestDist) {
                        bestDist = d;
                        bestA = a;
                        bestB = b;
                    }
                }
            }
            if (bestB < 0) {
                break;
            }
            graph.corridors.add(new Corridor(bestA, bestB));
            inTree[bestB] = true;
            connected++;
        }
    }

    private static void addLoops(DungeonGraph graph, DungeonConfig cfg, RandomSource rnd) {
        int n = graph.rooms.size();
        int extra = (int) Math.round((cfg.loopDensityPercent / 100.0) * n);
        Set<Long> existing = new HashSet<>();
        for (Corridor c : graph.corridors) {
            existing.add(edgeKey(c.roomA, c.roomB));
        }
        int attempts = 0;
        while (extra > 0 && attempts < n * 4) {
            attempts++;
            int a = rnd.nextInt(n);
            int b = rnd.nextInt(n);
            if (a == b) {
                continue;
            }
            long key = edgeKey(a, b);
            if (existing.contains(key)) {
                continue;
            }
            existing.add(key);
            graph.corridors.add(new Corridor(a, b));
            extra--;
        }
    }

    private static long edgeKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }
}
