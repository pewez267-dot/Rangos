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
 * CUEVA DE ARACNIDOS: una cueva ORGANICA y SINUOSA (camaras irregulares unidas por
 * tuneles serpenteantes con random-walk), NO una caja. Su identidad propia son las
 * CORTINAS DE TELARANA FUNCIONALES que cierran tramos de tunel (bloquean y ralentizan al
 * jugador de verdad), las CAMARAS DE HUEVOS con spawners de aranas, y una guarida final
 * mas amplia para el jefe.
 *
 * <p>Sellado: se talla el aire y luego se reviste con una cascara solida (todos los
 * vecinos solidos del aire), de modo que la superficie de la cueva queda cerrada y
 * continua. Las telaranas pueden flotar (mecanica vanilla) y son intencionadas.</p>
 */
public final class SpiderCaveBuilder {

    private SpiderCaveBuilder() {
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
        List<BlockPos> tunnelPath = new ArrayList<>();

        BlockPos prev = null;
        for (int i = 0; i < chambers; i++) {
            int cx = x0 + rnd.nextInt(Math.max(1, x1 - x0));
            int cy = y0 + rnd.nextInt(Math.max(1, y1 - y0));
            int cz = z0 + rnd.nextInt(Math.max(1, z1 - z0));
            BlockPos c = new BlockPos(cx, cy, cz);
            centers.add(c);
            // La guarida del jefe (ultima) es notablemente mas grande.
            boolean lair = (i == chambers - 1);
            int rx = (lair ? 7 : 4) + rnd.nextInt(4);
            int ry = (lair ? 5 : 3) + rnd.nextInt(3);
            int rz = (lair ? 7 : 4) + rnd.nextInt(4);
            carveBlob(air, sel, c, rx, ry, rz);
            if (prev != null) {
                carveWindingTunnel(air, sel, prev, c, rnd, tunnelPath);
            }
            prev = c;
        }

        // Aire interior.
        for (BlockPos p : air) {
            out.add(Placement.of(p, Blocks.AIR.defaultBlockState()));
        }
        // Cascara de muro continua: vecinos solidos del aire.
        BlockState wall = theme.wall();
        BlockState floorMat = theme.floor();
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
            // El piso de la cueva (cara inferior) usa el material de suelo del tema.
            boolean isFloor = air.contains(p.above());
            out.add(Placement.of(p, isFloor ? floorMat : wall));
        }

        // ----- identidad aracnida -----
        BlockState web = Blocks.COBWEB.defaultBlockState();
        String mobId = mobId(theme);

        // 1) Telaranas densas pegadas a techo/paredes (no obstruyen del todo).
        List<BlockPos> airList = new ArrayList<>(air);
        int webs = Math.min(4000, air.size() / 6);
        for (int i = 0; i < webs; i++) {
            BlockPos p = airList.get(rnd.nextInt(airList.size()));
            // Preferir celdas adyacentes a un solido (telarana anclada a la pared/techo).
            if (touchesSolid(air, p) || rnd.nextDouble() < 0.3) {
                out.add(Placement.of(p, web));
            }
        }

        // 2) Cortinas de telarana FUNCIONALES: cierran secciones enteras del tunel.
        int curtains = Math.max(2, tunnelPath.size() / 24);
        for (int i = 0; i < curtains && !tunnelPath.isEmpty(); i++) {
            BlockPos center = tunnelPath.get(rnd.nextInt(tunnelPath.size()));
            fillWebDisc(out, air, center);
        }

        // 3) Camaras de huevos + spawners de aranas en cada camara.
        long lootSeed = cfg.seed;
        for (int i = 0; i < centers.size(); i++) {
            BlockPos c = centers.get(i);
            BlockPos f = floorOf(air, c);
            BlockPos base = (f != null) ? f : c;

            // Racimo de huevos (telaranas) alrededor de un punto del suelo.
            if (rnd.nextDouble() < 0.7) {
                eggCluster(out, air, base, rnd);
                out.add(new Placement(base, Blocks.SPAWNER.defaultBlockState(),
                        RedstoneCircuitBuilder.spawnerData(mobId)));
            }
            // Luz tenue colgante (cueva oscura: pocas).
            if (rnd.nextDouble() < 0.4) {
                out.add(Placement.of(new BlockPos(c.getX(), c.getY() + 2, c.getZ()), theme.light()));
            }
            // Cofre en el fondo de la camara.
            if (f != null) {
                String table = (i == centers.size() - 1) ? cfg.bossLootTable
                        : (i % 2 == 0 ? cfg.treasureLootTable : cfg.normalLootTable);
                out.add(new Placement(f, Blocks.CHEST.defaultBlockState(),
                        DungeonLootAssigner.chestNbt(table, lootSeed++)));
            }
        }

        // 4) Jefe en la guarida final.
        if (cfg.bossEnabled && !centers.isEmpty()) {
            BlockPos last = centers.get(centers.size() - 1);
            BlockPos f = floorOf(air, last);
            bossSpawns.add(f != null ? f : last);
        }
    }

    // ----- geometria organica -----

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

    /** Tunel serpenteante (random walk con fuerte ruido lateral). Registra el recorrido. */
    private static void carveWindingTunnel(Set<BlockPos> air, SelectionShape sel, BlockPos from, BlockPos to,
                                           RandomSource rnd, List<BlockPos> path) {
        double x = from.getX();
        double y = from.getY();
        double z = from.getZ();
        int guard = 0;
        while (guard++ < 5000) {
            int ix = (int) Math.round(x);
            int iy = (int) Math.round(y);
            int iz = (int) Math.round(z);
            BlockPos here = new BlockPos(ix, iy, iz);
            carveBlob(air, sel, here, 2, 2, 2);
            path.add(here);
            if (Math.abs(ix - to.getX()) <= 1 && Math.abs(iy - to.getY()) <= 1 && Math.abs(iz - to.getZ()) <= 1) {
                break;
            }
            // Mas serpenteo que una cueva generica.
            x += Math.signum(to.getX() - x) * 0.7D + (rnd.nextDouble() - 0.5D) * 1.1D;
            y += Math.signum(to.getY() - y) * 0.5D + (rnd.nextDouble() - 0.5D) * 0.6D;
            z += Math.signum(to.getZ() - z) * 0.7D + (rnd.nextDouble() - 0.5D) * 1.1D;
        }
    }

    /** Cortina funcional: rellena un disco de telaranas en la seccion del tunel (solo donde hay aire). */
    private static void fillWebDisc(List<Placement> out, Set<BlockPos> air, BlockPos center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dy * dy + dz * dz > 6) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    if (air.contains(p)) {
                        out.add(Placement.of(p, Blocks.COBWEB.defaultBlockState()));
                    }
                }
            }
        }
    }

    /** Racimo de huevos: telaranas apinadas alrededor de un punto del suelo. */
    private static void eggCluster(List<Placement> out, Set<BlockPos> air, BlockPos base, RandomSource rnd) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (air.contains(p) && rnd.nextDouble() < 0.7) {
                        out.add(Placement.of(p, Blocks.COBWEB.defaultBlockState()));
                    }
                }
            }
        }
    }

    private static boolean touchesSolid(Set<BlockPos> air, BlockPos p) {
        return !air.contains(p.above()) || !air.contains(p.below())
                || !air.contains(p.north()) || !air.contains(p.south())
                || !air.contains(p.east()) || !air.contains(p.west());
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

    private static String mobId(DungeonTheme theme) {
        if (theme.spawnerMobs().isEmpty()) {
            return "minecraft:cave_spider";
        }
        EntityType<?> t = theme.spawnerMobs().get(0);
        return ForgeRegistries.ENTITY_TYPES.getKey(t) == null
                ? "minecraft:cave_spider" : ForgeRegistries.ENTITY_TYPES.getKey(t).toString();
    }
}
