package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.loot.DungeonLootAssigner;
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
 * Construye un CASTILLO real (no una caja): patio, muralla perimetral con almenas y
 * adarve, cuatro torres de esquina huecas con ventanas y almenas, una puerta con
 * rastrillo, y una torre del homenaje central de varias plantas con escalera, ventanas
 * y tejado almenado. El jefe va en lo alto del homenaje y los cofres dentro.
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
        int availH = max.getY() - gy;
        int wallH = Math.max(5, Math.min(8, availH / 3));
        int wallTop = gy + wallH;

        BlockState wall = theme.wall();
        BlockState floor = theme.floor();
        BlockState pillar = theme.pillar();
        BlockState light = theme.light();
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Patio (suelo).
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                add(out, sel, x, gy, z, floor);
            }
        }

        // 2) Muralla perimetral con almenas y adarve interior.
        for (int x = x0; x <= x1; x++) {
            curtain(out, sel, x, z0, gy, wallTop, wall);
            curtain(out, sel, x, z1, gy, wallTop, wall);
        }
        for (int z = z0; z <= z1; z++) {
            curtain(out, sel, x0, z, gy, wallTop, wall);
            curtain(out, sel, x1, z, gy, wallTop, wall);
        }
        // Adarve (camino de ronda) un bloque por dentro de la muralla.
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            add(out, sel, x, wallTop, z0 + 1, floor);
            add(out, sel, x, wallTop, z1 - 1, floor);
        }
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            add(out, sel, x0 + 1, wallTop, z, floor);
            add(out, sel, x1 - 1, wallTop, z, floor);
        }

        // 3) Puerta con rastrillo en el muro frontal (lado z0), centrada.
        int gateX = (x0 + x1) / 2;
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = gy + 1; y <= gy + 3; y++) {
                add(out, sel, gateX + dx, y, z0, air);
            }
            add(out, sel, gateX + dx, gy + 4, z0, bars);
        }

        // 4) Torres de esquina (huecas, con ventanas, plantas y almenas).
        int towerTop = Math.min(max.getY(), wallTop + 5);
        tower(out, sel, x0 + 2, z0 + 2, gy, towerTop, wall, floor, light);
        tower(out, sel, x1 - 2, z0 + 2, gy, towerTop, wall, floor, light);
        tower(out, sel, x0 + 2, z1 - 2, gy, towerTop, wall, floor, light);
        tower(out, sel, x1 - 2, z1 - 2, gy, towerTop, wall, floor, light);

        // 5) Torre del homenaje central (varias plantas).
        int innerW = x1 - x0;
        int innerL = z1 - z0;
        int kHalfX = Math.max(3, innerW / 5);
        int kHalfZ = Math.max(3, innerL / 5);
        int kcx = (x0 + x1) / 2;
        int kcz = (z0 + z1) / 2;
        int keepTop = Math.min(max.getY() - 1, wallTop + 7);
        keep(out, bossSpawns, sel, kcx, kcz, kHalfX, kHalfZ, gy, keepTop, theme, cfg, rnd, light, bars);

        // 6) Faroles en el patio.
        for (int x = x0 + 3; x < x1; x += 6) {
            for (int z = z0 + 3; z < z1; z += 6) {
                add(out, sel, x, gy + 1, z, pillar);
                add(out, sel, x, gy + 2, z, light);
            }
        }
    }

    private static void curtain(List<Placement> out, SelectionShape sel, int x, int z, int gy, int wallTop, BlockState wall) {
        for (int y = gy + 1; y <= wallTop; y++) {
            add(out, sel, x, y, z, wall);
        }
        // Almena (merlon) alternada en la cima.
        if (((x + z) & 1) == 0) {
            add(out, sel, x, wallTop + 1, z, wall);
        }
    }

    private static void tower(List<Placement> out, SelectionShape sel, int cx, int cz, int gy, int top,
                              BlockState wall, BlockState floor, BlockState light) {
        int r = 2;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = (x == cx - r || x == cx + r || z == cz - r || z == cz + r);
                for (int y = gy; y <= top; y++) {
                    if (y == gy) {
                        add(out, sel, x, y, z, floor);
                    } else if (edge) {
                        // Ventanas: hueco cada 3 alturas en el centro de cada lado.
                        boolean window = (y - gy) % 3 == 2 && ((x == cx && (z == cz - r || z == cz + r)) || (z == cz && (x == cx - r || x == cx + r)));
                        add(out, sel, x, y, z, window ? Blocks.AIR.defaultBlockState() : wall);
                    } else {
                        add(out, sel, x, y, z, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // Plantas internas cada 4 con hueco de escalera + escalera de mano.
        for (int fy = gy + 4; fy < top; fy += 4) {
            for (int x = cx - r + 1; x <= cx + r - 1; x++) {
                for (int z = cz - r + 1; z <= cz + r - 1; z++) {
                    if (x == cx + r - 1 && z == cz) {
                        continue; // hueco de escalera
                    }
                    add(out, sel, x, fy, z, floor);
                }
            }
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        for (int y = gy + 1; y < top; y++) {
            add(out, sel, cx + r - 1, y, cz, ladder);
        }
        // Almenas en la cima de la torre.
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = (x == cx - r || x == cx + r || z == cz - r || z == cz + r);
                if (edge && ((x + z) & 1) == 0) {
                    add(out, sel, x, top + 1, z, wall);
                }
            }
        }
        add(out, sel, cx, top - 1, cz, light);
    }

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
                        add(out, sel, x, y, z, y == gy ? floor : wall);
                    } else if (edge) {
                        boolean window = (y - gy) % 4 == 2 && (x == cx || z == cz);
                        add(out, sel, x, y, z, window ? Blocks.AIR.defaultBlockState() : wall);
                    } else {
                        add(out, sel, x, y, z, Blocks.AIR.defaultBlockState());
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
                    add(out, sel, x, fy, z, floor);
                }
            }
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        for (int y = gy + 1; y < top; y++) {
            add(out, sel, x1 - 1, y, cz, ladder);
        }
        // Puerta de entrada al homenaje (lado z0).
        for (int y = gy + 1; y <= gy + 2; y++) {
            add(out, sel, cx, y, z0, Blocks.AIR.defaultBlockState());
        }
        // Almenas del tejado.
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = (x == x0 || x == x1 || z == z0 || z == z1);
                if (edge && ((x + z) & 1) == 0) {
                    add(out, sel, x, top + 1, z, wall);
                }
            }
        }
        // Cofre del tesoro en planta baja y cofre/jefe en la cima.
        addChest(out, sel, new BlockPos(cx + 1, gy + 1, cz), cfg.treasureLootTable, rnd.nextLong());
        addChest(out, sel, new BlockPos(cx - 1, top - 3, cz), cfg.bossLootTable, rnd.nextLong());
        if (cfg.bossEnabled) {
            bossSpawns.add(new BlockPos(cx, top - 3, cz));
        }
        add(out, sel, cx, top - 1, cz, light);
    }

    private static void addChest(List<Placement> out, SelectionShape sel, BlockPos pos, String table, long seed) {
        if (sel.contains(pos)) {
            out.add(new Placement(pos, Blocks.CHEST.defaultBlockState(), DungeonLootAssigner.chestNbt(table, seed)));
        }
    }

    private static void add(List<Placement> out, SelectionShape sel, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (sel.contains(p)) {
            out.add(Placement.of(p, state));
        }
    }
}
