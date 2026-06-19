package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * CASTILLO ABANDONADO: una fortaleza inconfundible. MURALLA perimetral almenada con
 * adarve (camino de ronda), una CASA-PUERTA con rastrillo flanqueada por dos torres de
 * acceso, CUATRO TORRES DE ESQUINA que se alzan claramente sobre la muralla con
 * aspilleras y remate almenado, y una TORRE DEL HOMENAJE central de varias plantas con
 * escalera interior, ventanas, tejado almenado y estandarte. El jefe corona el homenaje.
 *
 * <p>Geometria cerrada y sellada: murallas y torres son cascaras solidas continuas; las
 * plantas de torres/homenaje son macizas salvo el hueco de escalera; nada flota.</p>
 */
public final class CastleBuilder {

    private CastleBuilder() {
    }

    public static void build(List<Placement> out, List<BlockPos> bossSpawns, SelectionShape sel,
                             DungeonTheme theme, DungeonConfig cfg, RandomSource rnd) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int x0 = min.getX() + 1;
        int z0 = min.getZ() + 1;
        int x1 = max.getX() - 1;
        int z1 = max.getZ() - 1;
        int gy = min.getY();
        if (x1 - x0 < 12 || z1 - z0 < 12) {
            return;
        }

        int availH = max.getY() - gy;
        int wallH = BuildUtil.clamp(availH / 4, 5, 9);
        int wallTop = gy + wallH;

        BlockState wall = theme.wall();
        BlockState floor = theme.floor();
        BlockState pillar = theme.pillar();
        BlockState light = theme.light();
        BlockState ground = Blocks.COBBLESTONE.defaultBlockState();
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();

        // 1) Patio empedrado.
        BuildUtil.fillBox(out, sel, x0, gy, z0, x1, gy, z1, ground);

        // 2) Muralla perimetral con almenas y adarve interior.
        BlockState plinth = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            curtain(out, sel, x, z0, gy, wallTop, wall, plinth, pillar);
            curtain(out, sel, x, z1, gy, wallTop, wall, plinth, pillar);
        }
        for (int z = z0; z <= z1; z++) {
            curtain(out, sel, x0, z, gy, wallTop, wall, plinth, pillar);
            curtain(out, sel, x1, z, gy, wallTop, wall, plinth, pillar);
        }
        // Adarve (camino de ronda) un bloque por dentro.
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            BuildUtil.set(out, sel, x, wallTop, z0 + 1, floor);
            BuildUtil.set(out, sel, x, wallTop, z1 - 1, floor);
        }
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            BuildUtil.set(out, sel, x0 + 1, wallTop, z, floor);
            BuildUtil.set(out, sel, x1 - 1, wallTop, z, floor);
        }

        // 3) Casa-puerta con rastrillo en el muro frontal (z0), flanqueada por torres de acceso.
        int gateX = (x0 + x1) / 2;
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = gy + 1; y <= gy + 3; y++) {
                BuildUtil.air(out, sel, gateX + dx, y, z0);
            }
            BuildUtil.set(out, sel, gateX + dx, gy + 4, z0, bars);   // rastrillo
        }
        int gateTowerTop = Math.min(max.getY(), wallTop + 4);
        tower(out, sel, gateX - 3, z0, gy, gateTowerTop, wall, floor, light);
        tower(out, sel, gateX + 3, z0, gy, gateTowerTop, wall, floor, light);

        // 4) Cuatro torres de esquina (claramente mas altas que la muralla).
        int towerTop = Math.min(max.getY(), wallTop + 6);
        tower(out, sel, x0 + 3, z0 + 3, gy, towerTop, wall, floor, light);
        tower(out, sel, x1 - 3, z0 + 3, gy, towerTop, wall, floor, light);
        tower(out, sel, x0 + 3, z1 - 3, gy, towerTop, wall, floor, light);
        tower(out, sel, x1 - 3, z1 - 3, gy, towerTop, wall, floor, light);

        // 5) Torre del homenaje central (varias plantas, tejado almenado, estandarte).
        int innerW = x1 - x0;
        int innerL = z1 - z0;
        int kHalfX = BuildUtil.clamp(innerW / 6, 3, 7);
        int kHalfZ = BuildUtil.clamp(innerL / 6, 3, 7);
        int kcx = (x0 + x1) / 2;
        int kcz = (z0 + z1) / 2;
        int keepTop = Math.min(max.getY() - 1, wallTop + 9);
        keep(out, bossSpawns, sel, kcx, kcz, kHalfX, kHalfZ, gy, keepTop, theme, cfg, rnd, light, bars);

        // 6) Pozo y faroles en el patio.
        well(out, sel, x0 + 4, z1 - 4, gy);
        for (int x = x0 + 4; x < x1 - 1; x += 7) {
            for (int z = z0 + 4; z < z1 - 1; z += 7) {
                if (Math.abs(x - kcx) <= kHalfX + 1 && Math.abs(z - kcz) <= kHalfZ + 1) {
                    continue; // no pisar el homenaje
                }
                BuildUtil.set(out, sel, x, gy + 1, z, pillar);
                BuildUtil.set(out, sel, x, gy + 2, z, light);
            }
        }
    }

    /** Lienzo de muralla con plinto de base, cuerpo y merlón de acento alternado en la cima. */
    private static void curtain(List<Placement> out, SelectionShape sel, int x, int z, int gy, int wallTop,
                                BlockState wall, BlockState plinth, BlockState accent) {
        BuildUtil.set(out, sel, x, gy + 1, z, plinth);            // plinto/base
        BuildUtil.pillar(out, sel, x, z, gy + 2, wallTop, wall);  // cuerpo del muro
        if (((x + z) & 1) == 0) {
            BuildUtil.set(out, sel, x, wallTop + 1, z, accent);   // merlón de acento
        }
    }

    /** Torre cuadrada hueca (7x7) con aspilleras, plantas con escalera, almenas y tejado cónico. */
    private static void tower(List<Placement> out, SelectionShape sel, int cx, int cz, int gy, int top,
                              BlockState wall, BlockState floor, BlockState light) {
        int r = 3;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = (x == cx - r || x == cx + r || z == cz - r || z == cz + r);
                for (int y = gy; y <= top; y++) {
                    if (y == gy) {
                        BuildUtil.set(out, sel, x, y, z, floor);
                    } else if (edge) {
                        boolean slit = (y - gy) % 3 == 2
                                && ((x == cx && (z == cz - r || z == cz + r)) || (z == cz && (x == cx - r || x == cx + r)));
                        BuildUtil.set(out, sel, x, y, z, slit ? Blocks.AIR.defaultBlockState() : wall);
                    } else {
                        BuildUtil.air(out, sel, x, y, z);
                    }
                }
            }
        }
        // Plantas internas cada 4 con hueco de escalera.
        for (int fy = gy + 4; fy < top; fy += 4) {
            for (int x = cx - r + 1; x <= cx + r - 1; x++) {
                for (int z = cz - r + 1; z <= cz + r - 1; z++) {
                    if (x == cx + r - 1 && z == cz) {
                        continue;
                    }
                    BuildUtil.set(out, sel, x, fy, z, floor);
                }
            }
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        for (int y = gy + 1; y < top; y++) {
            BuildUtil.set(out, sel, cx + r - 1, y, cz, ladder);
        }
        // Remate almenado + tejado cónico (techo a cuatro aguas que corona la torre).
        BuildUtil.crenellate(out, sel, cx - r, cz - r, cx + r, cz + r, top + 1, wall);
        conicalRoof(out, sel, cx, cz, top + 2, r, wall, light);
    }

    /** Tejado cónico de capas decrecientes que corona una torre/torreón. */
    private static void conicalRoof(List<Placement> out, SelectionShape sel, int cx, int cz, int baseY,
                                    int radius, BlockState roof, BlockState light) {
        for (int layer = 0; layer <= radius; layer++) {
            int rr = radius - layer;
            int y = baseY + layer;
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == rr) {
                        BuildUtil.set(out, sel, cx + dx, y, cz + dz, roof);
                    } else if (rr == 0) {
                        BuildUtil.set(out, sel, cx + dx, y, cz + dz, roof);
                    }
                }
            }
        }
        // Fanal/estandarte luminoso en la cúspide.
        BuildUtil.set(out, sel, cx, baseY + radius + 1, cz, light);
    }

    /** Torre del homenaje: cascara solida, plantas, escalera, tejado almenado, estandarte y jefe. */
    private static void keep(List<Placement> out, List<BlockPos> bossSpawns, SelectionShape sel,
                             int cx, int cz, int hx, int hz, int gy, int top, DungeonTheme theme,
                             DungeonConfig cfg, RandomSource rnd, BlockState light, BlockState bars) {
        int x0 = cx - hx;
        int x1 = cx + hx;
        int z0 = cz - hz;
        int z1 = cz + hz;
        BlockState wall = theme.wall();
        BlockState floor = theme.floor();

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = (x == x0 || x == x1 || z == z0 || z == z1);
                for (int y = gy; y <= top; y++) {
                    if (y == gy || y == top) {
                        BuildUtil.set(out, sel, x, y, z, y == gy ? floor : wall);
                    } else if (edge) {
                        boolean window = (y - gy) % 4 == 2 && (x == cx || z == cz);
                        BuildUtil.set(out, sel, x, y, z, window ? Blocks.AIR.defaultBlockState() : wall);
                    } else {
                        BuildUtil.air(out, sel, x, y, z);
                    }
                }
            }
        }
        // Plantas intermedias cada 4 con hueco de escalera.
        for (int fy = gy + 4; fy < top; fy += 4) {
            for (int x = x0 + 1; x < x1; x++) {
                for (int z = z0 + 1; z < z1; z++) {
                    if (x == x1 - 1 && z == cz) {
                        continue;
                    }
                    BuildUtil.set(out, sel, x, fy, z, floor);
                }
            }
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        for (int y = gy + 1; y < top; y++) {
            BuildUtil.set(out, sel, x1 - 1, y, cz, ladder);
        }
        // Puerta del homenaje (z0).
        for (int y = gy + 1; y <= gy + 2; y++) {
            BuildUtil.air(out, sel, cx, y, z0);
        }
        // Tejado almenado + estandarte.
        BuildUtil.crenellate(out, sel, x0, z0, x1, z1, top + 1, wall);
        BuildUtil.pillar(out, sel, cx, cz, top + 1, top + 3, Blocks.OAK_FENCE.defaultBlockState());
        BuildUtil.set(out, sel, cx, top + 3, cz, light);
        // Cofres y jefe.
        BuildUtil.chest(out, sel, new BlockPos(cx + 1, gy + 1, cz), cfg.treasureLootTable, rnd.nextLong());
        BuildUtil.chest(out, sel, new BlockPos(cx - 1, top - 3, cz), cfg.bossLootTable, rnd.nextLong());
        BuildUtil.set(out, sel, cx, top - 1, cz, light);
        if (cfg.bossEnabled) {
            bossSpawns.add(new BlockPos(cx, top - 3, cz));
        }
    }

    /** Pozo de patio: anillo de piedra con agua. */
    private static void well(List<Placement> out, SelectionShape sel, int cx, int cz, int gy) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if (edge) {
                    BuildUtil.set(out, sel, cx + dx, gy + 1, cz + dz, Blocks.COBBLESTONE_WALL.defaultBlockState());
                } else {
                    BuildUtil.set(out, sel, cx + dx, gy, cz + dz, Blocks.WATER.defaultBlockState());
                }
            }
        }
    }
}
