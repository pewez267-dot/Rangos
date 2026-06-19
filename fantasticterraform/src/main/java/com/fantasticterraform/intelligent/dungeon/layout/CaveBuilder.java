package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.loot.DungeonLootAssigner;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.intelligent.dungeon.traps.RedstoneCircuitBuilder;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Construye una CUEVA organica: camaras esfericas/elipsoidales de tamano variable
 * conectadas por tuneles sinuosos (random walk). No es una caja: la forma es irregular.
 * Solo se escribe la SUPERFICIE de la cueva (aire interior + cascara de muro alrededor),
 * por lo que es eficiente en memoria. Incluye telaranas (segun el acento del tema),
 * luces colgantes, cofres, spawners y una camara de jefe al final.
 */
public final class CaveBuilder {

    private CaveBuilder() {
    }

    public static void build(List<Placement> out, List<BlockPos> bossSpawns, SelectionShape sel,
                             DungeonTheme theme, DungeonConfig cfg, RandomSource rnd) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int pad = 6;
        int x0 = min.getX() + pad;
        int x1 = max.getX() - pad;
        int y0 = min.getY() + 3;
        int y1 = max.getY() - 3;
        int z0 = min.getZ() + pad;
        int z1 = max.getZ() - pad;
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) {
            return;
        }

        int chambers = 5 + rnd.nextInt(6);
        List<BlockPos> centers = new ArrayList<>();
        Set<BlockPos> air = new HashSet<>();

        BlockPos prev = null;
        for (int i = 0; i < chambers; i++) {
            int cx = x0 + rnd.nextInt(Math.max(1, x1 - x0));
            int cy = y0 + rnd.nextInt(Math.max(1, y1 - y0));
            int cz = z0 + rnd.nextInt(Math.max(1, z1 - z0));
            BlockPos c = new BlockPos(cx, cy, cz);
            centers.add(c);
            int rx = 4 + rnd.nextInt(4);
            int ry = 3 + rnd.nextInt(3);
            int rz = 4 + rnd.nextInt(4);
            carveBlob(air, sel, c, rx, ry, rz);
            if (prev != null) {
                carveTunnel(air, sel, prev, c, rnd);
            }
            prev = c;
        }

        // Escribir aire interior.
        for (BlockPos p : air) {
            out.add(Placement.of(p, Blocks.AIR.defaultBlockState()));
        }
        // Cascara de muro: vecinos solidos de las celdas de aire.
        BlockState wall = theme.wall();
        Set<BlockPos> shell = new HashSet<>();
        for (BlockPos p : air) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos n = p.offset(dx, dy, dz);
                        if (!air.contains(n) && sel.contains(n)) {
                            shell.add(n);
                        }
                    }
                }
            }
        }
        for (BlockPos p : shell) {
            out.add(Placement.of(p, wall));
        }

        // Decoracion: telaranas (acento), luces colgantes, cofres y spawners en camaras.
        BlockState accent = theme.accent();
        BlockState light = theme.light();
        String mobId = theme.spawnerMobs().isEmpty() ? "minecraft:cave_spider"
                : (ForgeRegistries.ENTITY_TYPES.getKey(theme.spawnerMobs().get(0)) == null
                ? "minecraft:cave_spider" : ForgeRegistries.ENTITY_TYPES.getKey(theme.spawnerMobs().get(0)).toString());

        int webs = Math.min(2000, air.size() / 12);
        List<BlockPos> airList = new ArrayList<>(air);
        for (int i = 0; i < webs; i++) {
            BlockPos p = airList.get(rnd.nextInt(airList.size()));
            out.add(Placement.of(p, accent));
        }

        for (int i = 0; i < centers.size(); i++) {
            BlockPos c = centers.get(i);
            // Luz colgante.
            out.add(Placement.of(new BlockPos(c.getX(), c.getY() + 2, c.getZ()), light));
            // Cofre en el fondo de la camara.
            BlockPos chestPos = floorOf(air, c);
            if (chestPos != null) {
                String table = (i == centers.size() - 1) ? cfg.bossLootTable
                        : (i % 2 == 0 ? cfg.treasureLootTable : cfg.normalLootTable);
                out.add(new Placement(chestPos, Blocks.CHEST.defaultBlockState(),
                        DungeonLootAssigner.chestNbt(table, rnd.nextLong())));
            }
            // Spawner ocasional.
            if (rnd.nextDouble() < 0.5) {
                out.add(new Placement(new BlockPos(c.getX() + 1, c.getY(), c.getZ()),
                        Blocks.SPAWNER.defaultBlockState(), RedstoneCircuitBuilder.spawnerData(mobId)));
            }
        }

        // Camara de jefe = ultima camara.
        if (cfg.bossEnabled && !centers.isEmpty()) {
            BlockPos last = centers.get(centers.size() - 1);
            BlockPos f = floorOf(air, last);
            bossSpawns.add(f != null ? f : last);
        }
    }

    private static void carveBlob(Set<BlockPos> air, SelectionShape sel, BlockPos c, int rx, int ry, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    double nx = (double) dx / rx;
                    double ny = (double) dy / ry;
                    double nz = (double) dz / rz;
                    if (nx * nx + ny * ny + nz * nz <= 1.0D) {
                        BlockPos p = c.offset(dx, dy, dz);
                        if (sel.contains(p)) {
                            air.add(p);
                        }
                    }
                }
            }
        }
    }

    private static void carveTunnel(Set<BlockPos> air, SelectionShape sel, BlockPos from, BlockPos to, RandomSource rnd) {
        double x = from.getX();
        double y = from.getY();
        double z = from.getZ();
        int guard = 0;
        while (guard++ < 4000) {
            int ix = (int) Math.round(x);
            int iy = (int) Math.round(y);
            int iz = (int) Math.round(z);
            carveBlob(air, sel, new BlockPos(ix, iy, iz), 2, 2, 2);
            if (Math.abs(ix - to.getX()) <= 1 && Math.abs(iy - to.getY()) <= 1 && Math.abs(iz - to.getZ()) <= 1) {
                break;
            }
            x += Math.signum(to.getX() - x) * 0.8D + (rnd.nextDouble() - 0.5D) * 0.6D;
            y += Math.signum(to.getY() - y) * 0.6D + (rnd.nextDouble() - 0.5D) * 0.4D;
            z += Math.signum(to.getZ() - z) * 0.8D + (rnd.nextDouble() - 0.5D) * 0.6D;
        }
    }

    private static BlockPos floorOf(Set<BlockPos> air, BlockPos c) {
        for (int y = c.getY(); y > c.getY() - 12; y--) {
            BlockPos p = new BlockPos(c.getX(), y, c.getZ());
            if (air.contains(p) && !air.contains(p.below())) {
                return p;
            }
        }
        return null;
    }
}
