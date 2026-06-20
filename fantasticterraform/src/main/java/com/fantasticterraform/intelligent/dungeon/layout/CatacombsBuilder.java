package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.multilevel.VerticalShaftBuilder;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * CATACUMBAS: un laberinto ESTRECHO, OSCURO y LABERINTICO, no una sala grande.
 *
 * <p>Tecnica: <i>solid-fill-then-carve</i>. Se rellena un bloque macizo (la roca de las
 * catacumbas) y se talla un laberinto perfecto con backtracker recursivo. Los pasillos
 * miden 1 bloque de ancho y 3 de alto; los muros entre pasillos miden 1 bloque. El
 * resultado es un dedalo cerrado y sellado por construccion: todo lo que no se talla es
 * muro continuo, asi que no hay huecos ni bloques flotantes.</p>
 *
 * <p>Detalles tematicos: iluminacion escasa (faroles de alma muy espaciados -> oscuro),
 * nichos funerarios con huesos y telaranas en los muertos del laberinto, cofres en los
 * callejones sin salida, spawners y el jefe en la celda mas lejana a la entrada.</p>
 */
public final class CatacombsBuilder {

    /** Altura libre de los pasillos (bloques de aire). */
    private static final int PASSAGE_HEIGHT = 3;
    /** Altura total de una "planta" del laberinto: piso + aire + techo. */
    private static final int BAND = PASSAGE_HEIGHT + 2;
    /** Limite de dimension del laberinto para mantener el coste de bloques acotado. */
    private static final int MAX_SPAN = 180;

    private CatacombsBuilder() {
    }

    public static void build(List<Placement> out, List<BlockPos> bossSpawns, SelectionShape sel,
                             DungeonTheme theme, DungeonConfig cfg, RandomSource rnd) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();

        int originX = min.getX() + 1;
        int originZ = min.getZ() + 1;
        int spanX = Math.min(MAX_SPAN, max.getX() - min.getX() - 1);
        int spanZ = Math.min(MAX_SPAN, max.getZ() - min.getZ() - 1);
        if (spanX < 5 || spanZ < 5) {
            return;
        }

        // Celdas del laberinto (pitch 2: 1 pasillo + 1 muro).
        int cols = (spanX - 1) / 2;
        int rows = (spanZ - 1) / 2;
        if (cols < 2 || rows < 2) {
            return;
        }

        // Cuantas plantas verticales caben / se piden.
        int totalH = max.getY() - min.getY() + 1;
        int floors = 1;
        if (cfg.multiLevel) {
            int maxByHeight = Math.max(1, (totalH - 2) / BAND);
            floors = BuildUtil.clamp(cfg.levels, 1, Math.min(maxByHeight, 4));
        }

        BlockState wall = theme.wall();
        BlockState floor = theme.floor();
        BlockState ceiling = theme.ceiling();
        String mobId = mobId(theme);

        // Celda de entrada (esquina) y, por planta, celda mas lejana para escaleras/jefe.
        int entranceCol = 0;
        int entranceRow = 0;

        int globalLootSeed = (int) (cfg.seed & 0xFFFF);
        BlockPos lastFarFloorPos = null;

        for (int f = 0; f < floors; f++) {
            int floorY = min.getY() + 1 + f * BAND;
            int ceilY = floorY + PASSAGE_HEIGHT;          // plano de techo
            if (ceilY > max.getY()) {
                break;
            }

            // 1) Macizo solido de toda la banda (sellado estructural).
            BuildUtil.fillBox(out, sel, originX, floorY - 1, originZ,
                    originX + spanX - 1, ceilY, originZ + spanZ - 1, wall);
            // Piso y techo con sus materiales.
            BuildUtil.fillBox(out, sel, originX, floorY - 1, originZ,
                    originX + spanX - 1, floorY - 1, originZ + spanZ - 1, floor);
            BuildUtil.fillBox(out, sel, originX, ceilY, originZ,
                    originX + spanX - 1, ceilY, originZ + spanZ - 1, ceiling);

            // 2) Generar el laberinto perfecto (backtracker recursivo) sobre la rejilla.
            boolean[][] visited = new boolean[cols][rows];
            // connV[c][r] = pasaje abierto entre (c,r) y (c,r+1); connH[c][r] = entre (c,r) y (c+1,r).
            boolean[][] connV = new boolean[cols][rows];
            boolean[][] connH = new boolean[cols][rows];
            carveMaze(cols, rows, entranceCol, entranceRow, visited, connH, connV, rnd);

            // 3) Tallar pasillos: celdas + uniones abiertas.
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    int cx = originX + 1 + c * 2;
                    int cz = originZ + 1 + r * 2;
                    BuildUtil.carveBox(out, sel, cx, floorY, cz, cx, ceilY - 1, cz);
                    if (c + 1 < cols && connH[c][r]) {
                        BuildUtil.carveBox(out, sel, cx + 1, floorY, cz, cx + 1, ceilY - 1, cz);
                    }
                    if (r + 1 < rows && connV[c][r]) {
                        BuildUtil.carveBox(out, sel, cx, floorY, cz + 1, cx, ceilY - 1, cz + 1);
                    }
                }
            }

            // 4) BFS desde la entrada para distancias (jefe = celda mas lejana, escaleras).
            int[][] dist = bfs(cols, rows, entranceCol, entranceRow, connH, connV);
            int farC = entranceCol;
            int farR = entranceRow;
            int best = -1;
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    if (dist[c][r] > best) {
                        best = dist[c][r];
                        farC = c;
                        farR = r;
                    }
                }
            }
            BlockPos farPos = new BlockPos(originX + 1 + farC * 2, floorY, originZ + 1 + farR * 2);

            // 5) Decoracion tematica por celda.
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    int cx = originX + 1 + c * 2;
                    int cz = originZ + 1 + r * 2;
                    int degree = degree(c, r, cols, rows, connH, connV);

                    // Nichos funerarios: tallados en un muro lateral de algunos pasillos.
                    if (rnd.nextDouble() < 0.18) {
                        carveNiche(out, sel, cx, floorY, cz, rnd, theme);
                    }
                    // Telaranas colgando del techo, dispersas.
                    if (rnd.nextDouble() < 0.12) {
                        BuildUtil.set(out, sel, cx, ceilY - 1, cz, Blocks.COBWEB.defaultBlockState());
                    }
                    // Iluminación MUY escasa (catacumbas oscuras): farol de alma en el suelo.
                    if (rnd.nextDouble() < 0.05) {
                        BuildUtil.set(out, sel, cx, floorY, cz, theme.light());
                    } else if (rnd.nextDouble() < 0.06) {
                        // Vela tenue sobre el suelo (luz baja, ambiente funerario).
                        BuildUtil.set(out, sel, cx, floorY, cz, Blocks.CANDLE.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true));
                    }
                    // Losas agrietadas y musgo en el piso: aspecto antiguo y ruinoso.
                    if (rnd.nextDouble() < 0.18) {
                        BlockState tile = rnd.nextBoolean()
                                ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                                : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                        BuildUtil.set(out, sel, cx, floorY - 1, cz, tile);
                    }
                    // Callejones sin salida: cofre o spawner (no en la entrada).
                    if (degree == 1 && !(c == entranceCol && r == entranceRow) && f == 0) {
                        double k = rnd.nextDouble();
                        if (k < 0.30) {
                            BuildUtil.chest(out, sel, new BlockPos(cx, floorY, cz),
                                    cfg.normalLootTable, (globalLootSeed++) * 0x2545F4914F6CDD1DL);
                        } else if (k < 0.45) {
                            BuildUtil.spawner(out, sel, new BlockPos(cx, floorY, cz), mobId);
                        }
                    }
                }
            }

            // Cofre del tesoro en la celda mas profunda de esta planta.
            BuildUtil.chest(out, sel, new BlockPos(farPos.getX(), floorY, farPos.getZ()),
                    cfg.treasureLootTable, (globalLootSeed++) * 0x9E3779B97F4A7C15L);

            // 6) Escalera al siguiente nivel: pozo vertical en la celda mas lejana.
            if (f + 1 < floors) {
                int nextFloorY = min.getY() + 1 + (f + 1) * BAND;
                if (nextFloorY + PASSAGE_HEIGHT <= max.getY()) {
                    // Asegurar columna tallada y conectada en ambas plantas.
                    VerticalShaftBuilder.build(BuildUtil.selFilteredList(out, sel), farPos.getX(), farPos.getZ(),
                            floorY, nextFloorY, wall);
                    // La entrada de la siguiente planta arranca justo encima del pozo.
                    entranceCol = farC;
                    entranceRow = farR;
                }
            }

            lastFarFloorPos = farPos;
        }

        // 7) Pozo de entrada desde la superficie hasta la primera planta.
        int firstFloorY = min.getY() + 1;
        int surfaceTop = max.getY();
        if (surfaceTop > firstFloorY + PASSAGE_HEIGHT) {
            VerticalShaftBuilder.build(BuildUtil.selFilteredList(out, sel),
                    originX + 1, originZ + 1, firstFloorY, surfaceTop, wall);
        }

        // 8) Jefe en la celda mas lejana de la ultima planta.
        if (cfg.bossEnabled && lastFarFloorPos != null) {
            bossSpawns.add(lastFarFloorPos);
        }
    }

    // ----- laberinto -----

    private static void carveMaze(int cols, int rows, int sc, int sr, boolean[][] visited,
                                  boolean[][] connH, boolean[][] connV, RandomSource rnd) {
        Deque<int[]> stack = new ArrayDeque<>();
        visited[sc][sr] = true;
        stack.push(new int[] {sc, sr});
        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            int c = cur[0];
            int r = cur[1];
            List<int[]> neighbors = new ArrayList<>(4);
            if (c > 0 && !visited[c - 1][r]) {
                neighbors.add(new int[] {c - 1, r});
            }
            if (c + 1 < cols && !visited[c + 1][r]) {
                neighbors.add(new int[] {c + 1, r});
            }
            if (r > 0 && !visited[c][r - 1]) {
                neighbors.add(new int[] {c, r - 1});
            }
            if (r + 1 < rows && !visited[c][r + 1]) {
                neighbors.add(new int[] {c, r + 1});
            }
            if (neighbors.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] n = neighbors.get(rnd.nextInt(neighbors.size()));
            int nc = n[0];
            int nr = n[1];
            // Abrir la pared entre la celda actual y la vecina.
            if (nc == c + 1) {
                connH[c][r] = true;
            } else if (nc == c - 1) {
                connH[nc][r] = true;
            } else if (nr == r + 1) {
                connV[c][r] = true;
            } else {
                connV[c][nr] = true;
            }
            visited[nc][nr] = true;
            stack.push(new int[] {nc, nr});
        }
    }

    private static int[][] bfs(int cols, int rows, int sc, int sr, boolean[][] connH, boolean[][] connV) {
        int[][] dist = new int[cols][rows];
        for (int[] row : dist) {
            Arrays.fill(row, -1);
        }
        Deque<int[]> q = new ArrayDeque<>();
        dist[sc][sr] = 0;
        q.add(new int[] {sc, sr});
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int c = cur[0];
            int r = cur[1];
            int d = dist[c][r];
            for (int[] nb : openNeighbors(c, r, cols, rows, connH, connV)) {
                if (dist[nb[0]][nb[1]] < 0) {
                    dist[nb[0]][nb[1]] = d + 1;
                    q.add(nb);
                }
            }
        }
        return dist;
    }

    private static List<int[]> openNeighbors(int c, int r, int cols, int rows, boolean[][] connH, boolean[][] connV) {
        List<int[]> list = new ArrayList<>(4);
        if (c + 1 < cols && connH[c][r]) {
            list.add(new int[] {c + 1, r});
        }
        if (c - 1 >= 0 && connH[c - 1][r]) {
            list.add(new int[] {c - 1, r});
        }
        if (r + 1 < rows && connV[c][r]) {
            list.add(new int[] {c, r + 1});
        }
        if (r - 1 >= 0 && connV[c][r - 1]) {
            list.add(new int[] {c, r - 1});
        }
        return list;
    }

    private static int degree(int c, int r, int cols, int rows, boolean[][] connH, boolean[][] connV) {
        return openNeighbors(c, r, cols, rows, connH, connV).size();
    }

    /** Talla un nicho funerario de 1x2 en un muro lateral con huesos/telarana. */
    private static void carveNiche(List<Placement> out, SelectionShape sel, int cx, int floorY, int cz,
                                   RandomSource rnd, DungeonTheme theme) {
        int dir = rnd.nextInt(4);
        int dx = dir == 0 ? 1 : dir == 1 ? -1 : 0;
        int dz = dir == 2 ? 1 : dir == 3 ? -1 : 0;
        int nx = cx + dx;
        int nz = cz + dz;
        // Hueco del nicho (1 ancho x 2 alto) y un bloque de hueso como "tumba".
        BuildUtil.air(out, sel, nx, floorY, nz);
        BuildUtil.air(out, sel, nx, floorY + 1, nz);
        BuildUtil.set(out, sel, nx, floorY, nz, Blocks.BONE_BLOCK.defaultBlockState());
        // Cráneo sobre la tumba o telaraña en el hueco (osario).
        double k = rnd.nextDouble();
        if (k < 0.45) {
            BuildUtil.set(out, sel, nx, floorY + 1, nz, Blocks.SKELETON_SKULL.defaultBlockState());
        } else if (k < 0.75) {
            BuildUtil.set(out, sel, nx, floorY + 1, nz, Blocks.COBWEB.defaultBlockState());
        }
    }

    private static String mobId(DungeonTheme theme) {
        if (theme.spawnerMobs().isEmpty()) {
            return "minecraft:skeleton";
        }
        EntityType<?> t = theme.spawnerMobs().get(0);
        return ForgeRegistries.ENTITY_TYPES.getKey(t) == null
                ? "minecraft:skeleton" : ForgeRegistries.ENTITY_TYPES.getKey(t).toString();
    }
}
