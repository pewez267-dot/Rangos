package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GUARIDA ELFICA / MISTICA: una ESTRUCTURA NATURAL construida e integrada con VEGETACION
 * REAL, no una cueva ni una caja de piedra. Un claro abovedado por un DOSEL DE HOJAS
 * sostenido por ARBOLES VIVOS (troncos que suben del suelo a la copa), con suelo de
 * cesped sembrado de flores, helechos y musgo, un ESTANQUE central, raices colgantes y
 * PLATAFORMAS DE MADERA elficas conectadas por escaleras.
 *
 * <p>Sellado: cascara exterior de piedra musgosa con el dosel de hojas como techo
 * continuo (hojas PERSISTENTES para que no decaigan). Cada arbol conecta suelo y dosel,
 * asi que nada flota.</p>
 */
public final class ElvenBuilder {

    private ElvenBuilder() {
    }

    public static void build(List<Placement> out, List<BlockPos> bossSpawns, SelectionShape sel,
                             DungeonTheme theme, DungeonConfig cfg, RandomSource rnd) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();

        int x0 = min.getX() + 1;
        int z0 = min.getZ() + 1;
        int x1 = max.getX() - 1;
        int z1 = max.getZ() - 1;
        int gy = min.getY() + 1;
        if (x1 - x0 < 8 || z1 - z0 < 8) {
            return;
        }

        int availH = max.getY() - gy;
        int hallH = BuildUtil.clamp(availH - 1, 6, 12);
        int ceilY = gy + hallH + 1;

        BlockState mossWall = theme.wall();                 // mossy cobblestone
        BlockState plank = theme.floor();                   // birch planks (para senderos/plataformas)
        BlockState glow = theme.light();                    // glowstone
        BlockState trunk = theme.pillar();                  // stripped birch log
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        BlockState azalea = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        BlockState grassGround = Blocks.GRASS_BLOCK.defaultBlockState();

        // 1) Cascara: muros de piedra musgosa, suelo de cesped, techo = dosel de hojas.
        BuildUtil.hollowRoom(out, sel, x0, gy, z0, x1, z1, hallH, mossWall, grassGround, leaf);

        // 2) Refuerzo del dosel: doble capa de hojas + claros de luz (glowstone embebido).
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int z = z0 + 1; z <= z1 - 1; z++) {
                if ((x + z) % 9 == 0) {
                    BuildUtil.set(out, sel, x, ceilY, z, glow);            // tragaluz luminoso sellado
                } else if (rnd.nextDouble() < 0.35) {
                    BuildUtil.set(out, sel, x, ceilY - 1, z, leaf);        // copa frondosa colgante
                }
            }
        }

        // 3) Suelo natural: musgo, flores, helechos sobre el cesped.
        int floorY = gy + 1;
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int z = z0 + 1; z <= z1 - 1; z++) {
                double k = rnd.nextDouble();
                if (k < 0.10) {
                    BuildUtil.set(out, sel, x, floorY, z, flower(rnd));
                } else if (k < 0.22) {
                    BuildUtil.set(out, sel, x, floorY, z, Blocks.FERN.defaultBlockState());
                } else if (k < 0.30) {
                    BuildUtil.set(out, sel, x, floorY, z, Blocks.GRASS.defaultBlockState());
                } else if (k < 0.34) {
                    BuildUtil.set(out, sel, x, gy, z, Blocks.MOSS_BLOCK.defaultBlockState());
                }
            }
        }

        // 4) Estanque central reflejante (agua + nenufares + borde de musgo).
        int cx = (x0 + x1) / 2;
        int cz = (z0 + z1) / 2;
        int pondR = Math.max(2, Math.min(x1 - x0, z1 - z0) / 6);
        for (int dx = -pondR; dx <= pondR; dx++) {
            for (int dz = -pondR; dz <= pondR; dz++) {
                if (dx * dx + dz * dz <= pondR * pondR) {
                    BuildUtil.set(out, sel, cx + dx, gy, cz + dz, Blocks.WATER.defaultBlockState());
                    // Vaciar lo que el suelo natural pudiera haber sembrado encima.
                    BuildUtil.air(out, sel, cx + dx, floorY, cz + dz);
                    if (rnd.nextDouble() < 0.18) {
                        BuildUtil.set(out, sel, cx + dx, floorY, cz + dz, Blocks.LILY_PAD.defaultBlockState());
                    }
                }
            }
        }

        // 5) Arboles vivos: troncos del suelo al dosel con copa de hojas (columnas naturales).
        String mobId = mobId(theme);
        long lootSeed = cfg.seed;
        int treeCount = Math.max(3, (x1 - x0) * (z1 - z0) / 90);
        List<BlockPos> treeBases = new ArrayList<>();
        int guard = 0;
        while (treeBases.size() < treeCount && guard++ < treeCount * 12) {
            int tx = x0 + 3 + rnd.nextInt(Math.max(1, x1 - x0 - 5));
            int tz = z0 + 3 + rnd.nextInt(Math.max(1, z1 - z0 - 5));
            // Evitar el estanque.
            if (Math.abs(tx - cx) <= pondR + 1 && Math.abs(tz - cz) <= pondR + 1) {
                continue;
            }
            boolean tooClose = false;
            for (BlockPos b : treeBases) {
                if (Math.abs(b.getX() - tx) < 4 && Math.abs(b.getZ() - tz) < 4) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) {
                continue;
            }
            treeBases.add(new BlockPos(tx, floorY, tz));
            buildTree(out, sel, tx, tz, floorY, ceilY, trunk, leaf, azalea, glow, rnd);

            // Raices y vida al pie del arbol: arbusto de azalea, hongo luminoso, spawner velado.
            if (rnd.nextDouble() < 0.5) {
                BuildUtil.set(out, sel, tx + 1, floorY, tz, Blocks.AZALEA.defaultBlockState());
            }
            if (rnd.nextDouble() < 0.35) {
                BuildUtil.spawner(out, sel, new BlockPos(tx - 1, floorY, tz), mobId);
            }
            if (rnd.nextDouble() < 0.30) {
                BuildUtil.chest(out, sel, new BlockPos(tx, floorY, tz + 1), cfg.normalLootTable, lootSeed++);
            }
        }

        // 6) Plataformas elficas de madera con barandilla y escalera (terrazas en altura).
        if (availH >= 11 && !treeBases.isEmpty()) {
            BlockPos anchor = treeBases.get(0);
            int platY = gy + hallH / 2;
            buildPlatform(out, sel, anchor.getX(), anchor.getZ(), platY, plank, rnd);
            // Escalera de caracol simple alrededor del tronco hasta la plataforma.
            spiralStair(out, sel, anchor.getX(), anchor.getZ(), floorY, platY, plank);
            BuildUtil.chest(out, sel, new BlockPos(anchor.getX() + 1, platY + 1, anchor.getZ()),
                    cfg.treasureLootTable, lootSeed++);
        }

        // 7) Claro del jefe: dais de raices/musgo al fondo con luz vegetal y la copa mas alta.
        int bossX = cx;
        int bossZ = z1 - 3;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz <= 5) {
                    BuildUtil.set(out, sel, bossX + dx, gy, bossZ + dz, Blocks.MOSS_BLOCK.defaultBlockState());
                    BuildUtil.air(out, sel, bossX + dx, floorY, bossZ + dz);
                }
            }
        }
        BuildUtil.set(out, sel, bossX, floorY, bossZ, Blocks.GLOWSTONE.defaultBlockState());
        BuildUtil.chest(out, sel, new BlockPos(bossX, floorY, bossZ - 1), cfg.bossLootTable, lootSeed++);
        if (cfg.bossEnabled) {
            bossSpawns.add(new BlockPos(bossX, floorY, bossZ));
        }

        // 8) Entrada: portico vegetal en el muro frontal (z0).
        for (int dx = -1; dx <= 1; dx++) {
            int h = (dx == 0) ? 4 : 3;
            for (int dy = 0; dy < h; dy++) {
                BuildUtil.air(out, sel, cx + dx, floorY + dy, z0);
            }
        }
        BuildUtil.set(out, sel, cx - 1, floorY + 3, z0, trunk);
        BuildUtil.set(out, sel, cx + 1, floorY + 3, z0, trunk);
        BuildUtil.set(out, sel, cx, floorY + 4, z0, leaf);
    }

    /** Arbol vivo: tronco recto del suelo al dosel y una copa de hojas que se funde con el techo. */
    private static void buildTree(List<Placement> out, SelectionShape sel, int x, int z, int floorY, int ceilY,
                                  BlockState trunk, BlockState leaf, BlockState azalea, BlockState glow, RandomSource rnd) {
        int top = ceilY - 1;
        for (int y = floorY; y <= top; y++) {
            BuildUtil.set(out, sel, x, y, z, trunk);
        }
        // Copa: blob de hojas centrado bajo el dosel.
        int crownY = top - 1;
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    if (dx * dx + dz * dz + (dy * dy) <= r * r + 1) {
                        BlockState l = (rnd.nextDouble() < 0.15) ? azalea : leaf;
                        BuildUtil.set(out, sel, x + dx, crownY + dy, z + dz, l);
                    }
                }
            }
        }
        // Resplandor entre la copa.
        if (rnd.nextBoolean()) {
            BuildUtil.set(out, sel, x, crownY, z + 1, glow);
        }
        // Raices colgantes / vida al pie.
        BuildUtil.set(out, sel, x, floorY, z, Blocks.MOSS_BLOCK.defaultBlockState());
    }

    /** Plataforma circular de tablones con barandilla de valla. */
    private static void buildPlatform(List<Placement> out, SelectionShape sel, int cx, int cz, int y,
                                      BlockState plank, RandomSource rnd) {
        int r = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= r * r) {
                    BuildUtil.set(out, sel, cx + dx, y, cz + dz, plank);
                }
                if (d2 > (r - 1) * (r - 1) && d2 <= r * r) {
                    BuildUtil.set(out, sel, cx + dx, y + 1, cz + dz, Blocks.BIRCH_FENCE.defaultBlockState());
                }
            }
        }
    }

    /** Escalera helicoidal sencilla de tablones alrededor de un eje, de y0 a y1. */
    private static void spiralStair(List<Placement> out, SelectionShape sel, int cx, int cz, int y0, int y1,
                                    BlockState step) {
        int[][] ring = {{2, 0}, {1, 1}, {0, 2}, {-1, 1}, {-2, 0}, {-1, -1}, {0, -2}, {1, -1}};
        int idx = 0;
        for (int y = y0; y <= y1; y++) {
            int[] o = ring[idx % ring.length];
            BuildUtil.set(out, sel, cx + o[0], y, cz + o[1], step);
            idx++;
        }
    }

    private static BlockState flower(RandomSource rnd) {
        switch (rnd.nextInt(6)) {
            case 0:
                return Blocks.POPPY.defaultBlockState();
            case 1:
                return Blocks.DANDELION.defaultBlockState();
            case 2:
                return Blocks.BLUE_ORCHID.defaultBlockState();
            case 3:
                return Blocks.OXEYE_DAISY.defaultBlockState();
            case 4:
                return Blocks.AZURE_BLUET.defaultBlockState();
            default:
                return Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
        }
    }

    private static String mobId(DungeonTheme theme) {
        if (theme.spawnerMobs().isEmpty()) {
            return "minecraft:witch";
        }
        EntityType<?> t = theme.spawnerMobs().get(0);
        return ForgeRegistries.ENTITY_TYPES.getKey(t) == null
                ? "minecraft:witch" : ForgeRegistries.ENTITY_TYPES.getKey(t).toString();
    }
}
