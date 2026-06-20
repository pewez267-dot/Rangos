package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.loot.DungeonLootAssigner;
import com.fantasticterraform.intelligent.dungeon.traps.RedstoneCircuitBuilder;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Primitivas de construccion SELLADA compartidas por todos los builders de estructuras.
 *
 * <p>Regla de oro de este modulo: <b>nunca se escribe fuera de la geometria real de la
 * seleccion</b>. Cada metodo filtra por {@link SelectionShape#contains(BlockPos)}, de
 * modo que una seleccion esferica, cilindrica o de cualquier forma recorta la estructura
 * a su volumen real sin dejar bloques flotando fuera ni huecos dentro.</p>
 *
 * <p>La tecnica principal para garantizar "cero huecos / cero flotantes" es
 * <i>solid-fill-then-carve</i>: se rellena un bloque macizo y luego se talla el aire
 * interior. Todo lo que no se talla queda como muro continuo, por lo que el sellado es
 * estructural y no depende de colocar paredes una a una.</p>
 */
public final class BuildUtil {

    private BuildUtil() {
    }

    // ---------------------------------------------------------------- escritura basica

    /** Coloca un bloque solo si cae dentro del volumen real de la seleccion. */
    public static void set(List<Placement> out, SelectionShape sel, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (sel.contains(p)) {
            out.add(Placement.of(p, state));
        }
    }

    public static void set(List<Placement> out, SelectionShape sel, BlockPos p, BlockState state) {
        if (sel.contains(p)) {
            out.add(Placement.of(p.immutable(), state));
        }
    }

    /** Coloca aire (vacia) dentro de la seleccion. */
    public static void air(List<Placement> out, SelectionShape sel, int x, int y, int z) {
        set(out, sel, x, y, z, Blocks.AIR.defaultBlockState());
    }

    // ---------------------------------------------------------------- volumenes

    /** Rellena una caja macizada [x0..x1][y0..y1][z0..z1] (inclusive) con un bloque. */
    public static void fillBox(List<Placement> out, SelectionShape sel, int x0, int y0, int z0,
                               int x1, int y1, int z1, BlockState state) {
        int ax = Math.min(x0, x1);
        int bx = Math.max(x0, x1);
        int ay = Math.min(y0, y1);
        int by = Math.max(y0, y1);
        int az = Math.min(z0, z1);
        int bz = Math.max(z0, z1);
        for (int x = ax; x <= bx; x++) {
            for (int y = ay; y <= by; y++) {
                for (int z = az; z <= bz; z++) {
                    set(out, sel, x, y, z, state);
                }
            }
        }
    }

    /** Talla (pone aire) en una caja [x0..x1][y0..y1][z0..z1] (inclusive). */
    public static void carveBox(List<Placement> out, SelectionShape sel, int x0, int y0, int z0,
                                int x1, int y1, int z1) {
        fillBox(out, sel, x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
    }

    /**
     * Construye una sala rectangular HUECA y sellada: piso, techo y cuatro muros macizos
     * con el interior vacio. {@code wallH} es la altura interior libre (de piso a techo).
     * La sala ocupa de (x0,y0,z0) a (x1, y0+wallH+1, z1).
     */
    public static void hollowRoom(List<Placement> out, SelectionShape sel, int x0, int y0, int z0,
                                  int x1, int z1, int wallH, BlockState wall, BlockState floor, BlockState ceiling) {
        int ax = Math.min(x0, x1);
        int bx = Math.max(x0, x1);
        int az = Math.min(z0, z1);
        int bz = Math.max(z0, z1);
        int yTop = y0 + wallH + 1;
        // Cuatro muros (planos X y Z) entre piso y techo: cada bloque se escribe una sola vez.
        for (int y = y0; y <= yTop; y++) {
            for (int x = ax; x <= bx; x++) {
                set(out, sel, x, y, az, wall);
                set(out, sel, x, y, bz, wall);
            }
            for (int z = az + 1; z <= bz - 1; z++) {
                set(out, sel, ax, y, z, wall);
                set(out, sel, bx, y, z, wall);
            }
        }
        // Piso y techo.
        fillBox(out, sel, ax, y0, az, bx, y0, bz, floor);
        fillBox(out, sel, ax, yTop, az, bx, yTop, bz, ceiling);
        // Interior hueco (imprescindible si la seleccion estaba en roca maciza).
        carveBox(out, sel, ax + 1, y0 + 1, az + 1, bx - 1, yTop - 1, bz - 1);
    }

    /** Columna vertical solida de (x,z) entre y0 e y1 inclusive. */
    public static void pillar(List<Placement> out, SelectionShape sel, int x, int z, int y0, int y1, BlockState state) {
        int lo = Math.min(y0, y1);
        int hi = Math.max(y0, y1);
        for (int y = lo; y <= hi; y++) {
            set(out, sel, x, y, z, state);
        }
    }

    // ---------------------------------------------------------------- contenido

    /** Cofre con loot table real. */
    public static void chest(List<Placement> out, SelectionShape sel, BlockPos pos, String lootTable, long seed) {
        if (sel.contains(pos)) {
            out.add(new Placement(pos.immutable(), Blocks.CHEST.defaultBlockState(),
                    DungeonLootAssigner.chestNbt(lootTable, seed)));
        }
    }

    /** Spawner del mob indicado. */
    public static void spawner(List<Placement> out, SelectionShape sel, BlockPos pos, String entityId) {
        if (sel.contains(pos)) {
            out.add(new Placement(pos.immutable(), Blocks.SPAWNER.defaultBlockState(),
                    RedstoneCircuitBuilder.spawnerData(entityId)));
        }
    }

    /** Almenas (merlones) alternados sobre el contorno de un rectangulo, a una altura dada. */
    public static void crenellate(List<Placement> out, SelectionShape sel, int x0, int z0, int x1, int z1,
                                  int y, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            if (((x + z0) & 1) == 0) {
                set(out, sel, x, y, z0, state);
            }
            if (((x + z1) & 1) == 0) {
                set(out, sel, x, y, z1, state);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if (((x0 + z) & 1) == 0) {
                set(out, sel, x0, y, z, state);
            }
            if (((x1 + z) & 1) == 0) {
                set(out, sel, x1, y, z, state);
            }
        }
    }

    /**
     * Cornisa interior: un reborde de losa que recorre el perimetro interior de una sala
     * a una altura dada. Aporta profundidad y rompe la planitud de los muros (truco
     * clasico de construccion: usar losas/escaleras para fingir grosor).
     */
    public static void cornice(List<Placement> out, SelectionShape sel, int x0, int z0, int x1, int z1,
                               int y, BlockState slab) {
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            set(out, sel, x, y, z0 + 1, slab);
            set(out, sel, x, y, z1 - 1, slab);
        }
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            set(out, sel, x0 + 1, y, z, slab);
            set(out, sel, x1 - 1, y, z, slab);
        }
    }

    /** Pilastra adosada al muro: cuerpo vertical con base y capitel de acento. */
    public static void pilaster(List<Placement> out, SelectionShape sel, int x, int z, int y0, int y1,
                                BlockState body, BlockState accent) {
        set(out, sel, x, y0, z, accent);          // base
        pillar(out, sel, x, z, y0 + 1, y1 - 1, body);
        set(out, sel, x, y1, z, accent);          // capitel
    }

    /** Zocalo/borde decorativo en el piso, recorriendo el perimetro interior. */
    public static void floorBorder(List<Placement> out, SelectionShape sel, int x0, int z0, int x1, int z1,
                                   int y, BlockState state) {
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            set(out, sel, x, y, z0 + 1, state);
            set(out, sel, x, y, z1 - 1, state);
        }
        for (int z = z0 + 2; z <= z1 - 2; z++) {
            set(out, sel, x0 + 1, y, z, state);
            set(out, sel, x1 - 1, y, z, state);
        }
    }

    // ---------------------------------------------------------------- util

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Devuelve una lista que reenvia a {@code out} pero descarta cualquier {@link Placement}
     * fuera de la seleccion. Util para reutilizar builders auxiliares (pozos, escaleras)
     * que reciben una {@code List<Placement>} directa y deben respetar la geometria real.
     */
    public static List<Placement> selFilteredList(List<Placement> out, SelectionShape sel) {
        return new java.util.AbstractList<Placement>() {
            @Override
            public boolean add(Placement p) {
                if (p != null && p.pos != null && sel.contains(p.pos)) {
                    out.add(p);
                }
                return true;
            }

            @Override
            public Placement get(int index) {
                return out.get(index);
            }

            @Override
            public int size() {
                return out.size();
            }
        };
    }
}
