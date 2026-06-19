package com.fantasticterraform.intelligent.dungeon;

import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.ListWriteTask;
import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.boss.BossEntityPlacer;
import com.fantasticterraform.intelligent.dungeon.boss.BossRoomConfig;
import com.fantasticterraform.intelligent.dungeon.loot.DungeonLootAssigner;
import com.fantasticterraform.intelligent.dungeon.multilevel.VerticalShaftBuilder;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.intelligent.dungeon.traps.HiddenLavaTrap;
import com.fantasticterraform.intelligent.dungeon.traps.HiddenPitTrap;
import com.fantasticterraform.intelligent.dungeon.traps.PressurePlateArrowTrap;
import com.fantasticterraform.intelligent.dungeon.traps.SpawnerAmbushTrap;
import com.fantasticterraform.intelligent.dungeon.traps.Trap;
import com.fantasticterraform.intelligent.dungeon.traps.WallArrowVolleyTrap;
import com.fantasticterraform.intelligent.dungeon.traps.RedstoneCircuitBuilder;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Materializa una dungeon en bloques: talla salas (rectangulares y circulares),
 * pasillos en L, pozos verticales entre niveles, decoracion, iluminacion, cofres con
 * loot, spawners y trampas funcionales. Todo se aplica por la cola por ticks; el jefe
 * se invoca al terminar. Solo coloca dentro de la geometria real de la seleccion.
 */
public final class DungeonMaterializer {

    private static final int SAFETY_CAP = 3_000_000;

    private DungeonMaterializer() {
    }

    public static void generate(ServerPlayer player, ServerLevel level, SelectionShape sel, DungeonConfig cfg) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        DungeonGraph graph = GraphGenerator.generate(cfg, sel);
        if (graph.rooms.size() < 2) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7cNo caben suficientes salas en la seleccion. Agrandala o baja el tier."));
            return;
        }

        RandomSource rnd = RandomSource.create(cfg.seed ^ 0x9E3779B97F4A7C15L);
        DungeonTheme theme = cfg.theme;
        List<Placement> out = new ArrayList<>();
        List<BlockPos> bossSpawns = new ArrayList<>();
        List<Trap> traps = enabledTraps(cfg);

        long lootSeed = cfg.seed;

        for (Room room : graph.rooms) {
            buildRoom(out, sel, room, theme);
            decorate(out, sel, room, theme, rnd);
            lighting(out, sel, room, theme, cfg.lightSpacing);

            BlockPos centerFloor = new BlockPos(room.center().getX(), room.min.getY() + 1, room.center().getZ());
            switch (room.type) {
                case TREASURE:
                    addChest(out, sel, centerFloor, cfg.treasureLootTable, lootSeed++);
                    break;
                case BOSS:
                    addChest(out, sel, centerFloor.relative(Direction.NORTH, 2), cfg.bossLootTable, lootSeed++);
                    if (cfg.bossEnabled) {
                        bossSpawns.add(centerFloor);
                    }
                    break;
                case NORMAL:
                    if (rnd.nextDouble() < 0.35) {
                        addSpawner(out, sel, new BlockPos(room.min.getX() + 1, room.min.getY() + 1, room.min.getZ() + 1), theme);
                    }
                    if (rnd.nextDouble() < 0.25) {
                        addChest(out, sel, centerFloor, cfg.normalLootTable, lootSeed++);
                    }
                    break;
                default:
                    break;
            }

            // Trampa segun densidad (no en la entrada).
            if (room.type != RoomType.ENTRANCE && !traps.isEmpty() && rnd.nextDouble() < cfg.trapDensity) {
                Trap trap = traps.get(rnd.nextInt(traps.size()));
                BlockPos walk = new BlockPos(room.min.getX() + 2, room.min.getY() + 1, room.center().getZ());
                addTrap(out, sel, level, trap, walk, Direction.EAST, rnd, theme);
            }

            if (out.size() > SAFETY_CAP) {
                break;
            }
        }

        // Pasillos (despues de las salas, para perforar puertas).
        for (Corridor c : graph.corridors) {
            if (out.size() > SAFETY_CAP) {
                break;
            }
            carveCorridor(out, sel, graph.rooms.get(c.roomA), graph.rooms.get(c.roomB), theme, rnd, graph.rooms);
        }

        // Entrada inteligente a la superficie: pozo con escalera desde la sala de entrada hacia arriba.
        for (Room room : graph.rooms) {
            if (room.type == RoomType.ENTRANCE) {
                int top = sel.getMax().getY();
                if (top > room.maxY() + 1) {
                    VerticalShaftBuilder.build(filteredList(out, sel),
                            room.center().getX(), room.center().getZ(), room.maxY(), top, theme.wall());
                }
                break;
            }
        }

        BossRoomConfig bossCfg = new BossRoomConfig(cfg.bossEntityId, cfg.bossCount, true);
        Runnable onFinish = () -> {
            for (BlockPos pos : bossSpawns) {
                BossEntityPlacer.spawn(level, pos, bossCfg);
            }
            player.sendSystemMessage(Component.literal("\u00a7aDungeon generada: \u00a7f" + graph.rooms.size()
                    + " salas, " + graph.corridors.size() + " pasillos. Tema: " + theme.displayName() + "."));
        };

        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(),
                "Dungeon (" + theme.displayName() + ")", null, out, true, onFinish));
    }

    // ----- salas -----

    private static void buildRoom(List<Placement> out, SelectionShape sel, Room room, DungeonTheme theme) {
        if (room.shape == Room.Shape.CIRCLE) {
            buildCircularRoom(out, sel, room, theme);
        } else {
            buildRectRoom(out, sel, room, theme);
        }
    }

    private static void buildRectRoom(List<Placement> out, SelectionShape sel, Room room, DungeonTheme theme) {
        for (int x = room.min.getX(); x <= room.maxX(); x++) {
            for (int y = room.min.getY(); y <= room.maxY(); y++) {
                for (int z = room.min.getZ(); z <= room.maxZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    boolean ex = x == room.min.getX() || x == room.maxX();
                    boolean ez = z == room.min.getZ() || z == room.maxZ();
                    boolean floor = y == room.min.getY();
                    boolean ceil = y == room.maxY();
                    BlockState state;
                    if (floor) {
                        state = theme.floor();
                    } else if (ceil) {
                        state = theme.ceiling();
                    } else if (ex || ez) {
                        state = theme.wall();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    add(out, sel, p, state);
                }
            }
        }
    }

    private static void buildCircularRoom(List<Placement> out, SelectionShape sel, Room room, DungeonTheme theme) {
        double cx = room.min.getX() + room.sizeX / 2.0 - 0.5;
        double cz = room.min.getZ() + room.sizeZ / 2.0 - 0.5;
        double rx = room.sizeX / 2.0;
        double rz = room.sizeZ / 2.0;
        for (int x = room.min.getX(); x <= room.maxX(); x++) {
            for (int z = room.min.getZ(); z <= room.maxZ(); z++) {
                if (!insideEllipse(x, z, cx, cz, rx, rz)) {
                    continue;
                }
                // Es muro si algun vecino ortogonal queda FUERA de la elipse (borde solido continuo).
                boolean boundary = !insideEllipse(x + 1, z, cx, cz, rx, rz)
                        || !insideEllipse(x - 1, z, cx, cz, rx, rz)
                        || !insideEllipse(x, z + 1, cx, cz, rx, rz)
                        || !insideEllipse(x, z - 1, cx, cz, rx, rz);
                for (int y = room.min.getY(); y <= room.maxY(); y++) {
                    BlockState state;
                    if (y == room.min.getY()) {
                        state = theme.floor();
                    } else if (y == room.maxY()) {
                        state = theme.ceiling();
                    } else if (boundary) {
                        state = theme.wall();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    add(out, sel, new BlockPos(x, y, z), state);
                }
            }
        }
    }

    private static boolean insideEllipse(int x, int z, double cx, double cz, double rx, double rz) {
        double nx = (x - cx) / rx;
        double nz = (z - cz) / rz;
        return nx * nx + nz * nz <= 1.0D;
    }

    private static void decorate(List<Placement> out, SelectionShape sel, Room room, DungeonTheme theme, RandomSource rnd) {
        int x0 = room.min.getX() + 1;
        int x1 = room.maxX() - 1;
        int z0 = room.min.getZ() + 1;
        int z1 = room.maxZ() - 1;
        // Pilares en las esquinas interiores (toda la altura).
        for (int x : new int[] {x0, x1}) {
            for (int z : new int[] {z0, z1}) {
                for (int y = room.min.getY() + 1; y < room.maxY(); y++) {
                    add(out, sel, new BlockPos(x, y, z), theme.pillar());
                }
            }
        }
        // Pilares intermedios en salas grandes.
        if (room.sizeX >= 13 && room.sizeZ >= 13) {
            int mx = room.center().getX();
            int mz = room.center().getZ();
            int[][] offs = {{-room.sizeX / 4, 0}, {room.sizeX / 4, 0}, {0, -room.sizeZ / 4}, {0, room.sizeZ / 4}};
            for (int[] o : offs) {
                for (int y = room.min.getY() + 1; y < room.maxY(); y++) {
                    add(out, sel, new BlockPos(mx + o[0], y, mz + o[1]), theme.pillar());
                }
            }
        }
        // Apliques de luz en las paredes a media altura.
        int midY = room.min.getY() + Math.max(2, room.sizeY / 2);
        for (int x = room.min.getX() + 2; x < room.maxX(); x += 4) {
            add(out, sel, new BlockPos(x, midY, room.min.getZ() + 1), theme.light());
            add(out, sel, new BlockPos(x, midY, room.maxZ() - 1), theme.light());
        }
        // Acentos dispersos en el piso.
        int accents = room.sizeX * room.sizeZ / 16;
        for (int i = 0; i < accents; i++) {
            int x = room.min.getX() + 1 + rnd.nextInt(Math.max(1, room.sizeX - 2));
            int z = room.min.getZ() + 1 + rnd.nextInt(Math.max(1, room.sizeZ - 2));
            add(out, sel, new BlockPos(x, room.min.getY() + 1, z), theme.accent());
        }
        // Dressing especifico segun el tema (cada generacion lo distribuye distinto).
        String id = theme.id();
        if ("spider_cave".equals(id)) {
            int webs = room.sizeX * room.sizeZ / 8;
            for (int i = 0; i < webs; i++) {
                int x = room.min.getX() + 1 + rnd.nextInt(Math.max(1, room.sizeX - 2));
                int z = room.min.getZ() + 1 + rnd.nextInt(Math.max(1, room.sizeZ - 2));
                int y = room.maxY() - 1 - rnd.nextInt(Math.max(1, room.sizeY - 2));
                add(out, sel, new BlockPos(x, y, z), Blocks.COBWEB.defaultBlockState());
            }
        } else if ("ancient_crypt".equals(id)) {
            BlockPos c = room.center();
            add(out, sel, new BlockPos(c.getX(), room.min.getY() + 1, c.getZ()), theme.accent());
            add(out, sel, new BlockPos(c.getX(), room.min.getY() + 2, c.getZ()), theme.pillar());
        } else if ("mystic_elven".equals(id)) {
            int vines = room.sizeX * room.sizeZ / 12;
            for (int i = 0; i < vines; i++) {
                int x = room.min.getX() + 1 + rnd.nextInt(Math.max(1, room.sizeX - 2));
                int z = room.min.getZ() + 1 + rnd.nextInt(Math.max(1, room.sizeZ - 2));
                add(out, sel, new BlockPos(x, room.maxY() - 1, z), Blocks.OAK_LEAVES.defaultBlockState());
            }
        }

        // Pieza central variable: cada sala recibe una de varias para que no se vean iguales.
        if (room.sizeX >= 9 && room.sizeZ >= 9) {
            BlockPos c = room.center();
            int fy = room.min.getY() + 1;
            switch (rnd.nextInt(4)) {
                case 1: // altar elevado
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            add(out, sel, new BlockPos(c.getX() + dx, fy, c.getZ() + dz), theme.accent());
                        }
                    }
                    add(out, sel, new BlockPos(c.getX(), fy + 1, c.getZ()), theme.pillar());
                    break;
                case 2: // estanque central
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            add(out, sel, new BlockPos(c.getX() + dx, room.min.getY(), c.getZ() + dz),
                                    Blocks.WATER.defaultBlockState());
                        }
                    }
                    break;
                case 3: // anillo de columnas
                    int r = Math.max(2, Math.min(room.sizeX, room.sizeZ) / 2 - 2);
                    for (int k = 0; k < 8; k++) {
                        double ang = k * Math.PI / 4.0;
                        int px = c.getX() + (int) Math.round(Math.cos(ang) * r);
                        int pz = c.getZ() + (int) Math.round(Math.sin(ang) * r);
                        for (int y = room.min.getY() + 1; y < room.maxY(); y++) {
                            add(out, sel, new BlockPos(px, y, pz), theme.pillar());
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void lighting(List<Placement> out, SelectionShape sel, Room room, DungeonTheme theme, int spacing) {
        int s = Math.max(3, spacing);
        for (int x = room.min.getX() + 2; x < room.maxX(); x += s) {
            for (int z = room.min.getZ() + 2; z < room.maxZ(); z += s) {
                add(out, sel, new BlockPos(x, room.maxY() - 1, z), theme.light());
            }
        }
    }

    // ----- pasillos (de puerta a puerta; nunca atraviesan el interior de las salas) -----

    private static void carveCorridor(List<Placement> out, SelectionShape sel, Room a, Room b, DungeonTheme theme,
                                      RandomSource rnd, java.util.List<Room> rooms) {
        BlockPos ca = a.center();
        BlockPos cb = b.center();
        int ay = a.min.getY() + 1;
        int by = b.min.getY() + 1;
        int dx = cb.getX() - ca.getX();
        int dz = cb.getZ() - ca.getZ();

        BlockPos aOut;
        BlockPos bOut;
        if (Math.abs(dx) >= Math.abs(dz)) {
            int doorAz = clampi(cb.getZ(), a.min.getZ() + 1, a.maxZ() - 1);
            int doorBz = clampi(ca.getZ(), b.min.getZ() + 1, b.maxZ() - 1);
            if (dx >= 0) {
                punchDoorway(out, sel, a.maxX(), ay, doorAz, true);
                punchDoorway(out, sel, b.min.getX(), by, doorBz, true);
                aOut = new BlockPos(a.maxX() + 1, ay, doorAz);
                bOut = new BlockPos(b.min.getX() - 1, by, doorBz);
            } else {
                punchDoorway(out, sel, a.min.getX(), ay, doorAz, true);
                punchDoorway(out, sel, b.maxX(), by, doorBz, true);
                aOut = new BlockPos(a.min.getX() - 1, ay, doorAz);
                bOut = new BlockPos(b.maxX() + 1, by, doorBz);
            }
        } else {
            int doorAx = clampi(cb.getX(), a.min.getX() + 1, a.maxX() - 1);
            int doorBx = clampi(ca.getX(), b.min.getX() + 1, b.maxX() - 1);
            if (dz >= 0) {
                punchDoorway(out, sel, doorAx, ay, a.maxZ(), false);
                punchDoorway(out, sel, doorBx, by, b.min.getZ(), false);
                aOut = new BlockPos(doorAx, ay, a.maxZ() + 1);
                bOut = new BlockPos(doorBx, by, b.min.getZ() - 1);
            } else {
                punchDoorway(out, sel, doorAx, ay, a.min.getZ(), false);
                punchDoorway(out, sel, doorBx, by, b.maxZ(), false);
                aOut = new BlockPos(doorAx, ay, a.min.getZ() - 1);
                bOut = new BlockPos(doorBx, by, b.maxZ() + 1);
            }
        }

        // Tunel en L por el HUECO entre salas, a la altura de A.
        BlockPos corner = new BlockPos(bOut.getX(), ay, aOut.getZ());
        carveLine(out, sel, aOut, corner, theme, rooms);
        carveLine(out, sel, corner, new BlockPos(bOut.getX(), ay, bOut.getZ()), theme, rooms);

        // Multinivel: si B esta a otra altura, pozo vertical con escalera junto a su puerta.
        if (Math.abs(by - ay) > 1) {
            VerticalShaftBuilder.build(filteredList(out, sel), bOut.getX(), bOut.getZ(), ay, by, theme.wall());
        }
    }

    /** Perfora una puerta de 3 de ancho y 3 de alto en el muro de una sala. */
    private static void punchDoorway(List<Placement> out, SelectionShape sel, int wallX, int y, int wallZ, boolean alongX) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int w = -1; w <= 1; w++) {
            for (int dy = 0; dy <= 2; dy++) {
                int px = alongX ? wallX : wallX + w;
                int pz = alongX ? wallZ + w : wallZ;
                add(out, sel, new BlockPos(px, y + dy, pz), air);
            }
        }
    }

    private static void carveLine(List<Placement> out, SelectionShape sel, BlockPos from, BlockPos to,
                                  DungeonTheme theme, java.util.List<Room> rooms) {
        int y = from.getY();
        int x = from.getX();
        int z = from.getZ();
        int sx = Integer.signum(to.getX() - x);
        int sz = Integer.signum(to.getZ() - z);
        int qx = sz;
        int qz = sx;
        if (qx == 0 && qz == 0) {
            qz = 1;
        }
        int steps = Math.abs(to.getX() - x) + Math.abs(to.getZ() - z);
        for (int i = 0; i <= steps; i++) {
            carveCrossSection(out, sel, x, y, z, theme, qx, qz, rooms);
            if (x != to.getX()) {
                x += sx;
            } else if (z != to.getZ()) {
                z += sz;
            }
        }
    }

    /** Seccion de tunel 3x3 sellada; NO modifica el interior de ninguna sala. */
    private static void carveCrossSection(List<Placement> out, SelectionShape sel, int x, int y, int z,
                                          DungeonTheme theme, int qx, int qz, java.util.List<Room> rooms) {
        for (int w = -1; w <= 1; w++) {
            int lx = x + qx * w;
            int lz = z + qz * w;
            if (insideInterior(lx, y, lz, rooms)) {
                continue;
            }
            add(out, sel, new BlockPos(lx, y - 1, lz), theme.floor());
            add(out, sel, new BlockPos(lx, y + 3, lz), theme.ceiling());
            for (int dy = 0; dy <= 2; dy++) {
                add(out, sel, new BlockPos(lx, y + dy, lz), Blocks.AIR.defaultBlockState());
            }
        }
        for (int w : new int[] {-2, 2}) {
            int lx = x + qx * w;
            int lz = z + qz * w;
            if (insideInterior(lx, y, lz, rooms)) {
                continue;
            }
            for (int dy = -1; dy <= 3; dy++) {
                add(out, sel, new BlockPos(lx, y + dy, lz), theme.wall());
            }
        }
    }

    private static boolean insideInterior(int x, int y, int z, java.util.List<Room> rooms) {
        for (Room r : rooms) {
            if (x > r.min.getX() && x < r.maxX() && z > r.min.getZ() && z < r.maxZ()
                    && y > r.min.getY() && y < r.maxY()) {
                return true;
            }
        }
        return false;
    }

    private static int clampi(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ----- contenido -----

    private static void addChest(List<Placement> out, SelectionShape sel, BlockPos pos, String lootTable, long seed) {
        if (!sel.contains(pos)) {
            return;
        }
        out.add(new Placement(pos, Blocks.CHEST.defaultBlockState(), DungeonLootAssigner.chestNbt(lootTable, seed)));
    }

    private static void addSpawner(List<Placement> out, SelectionShape sel, BlockPos pos, DungeonTheme theme) {
        if (!sel.contains(pos)) {
            return;
        }
        EntityType<?> mob = theme.spawnerMobs().isEmpty() ? EntityType.ZOMBIE : theme.spawnerMobs().get(0);
        String id = ForgeRegistries.ENTITY_TYPES.getKey(mob) == null
                ? "minecraft:zombie" : ForgeRegistries.ENTITY_TYPES.getKey(mob).toString();
        out.add(new Placement(pos, Blocks.SPAWNER.defaultBlockState(), RedstoneCircuitBuilder.spawnerData(id)));
    }

    private static void addTrap(List<Placement> out, SelectionShape sel, ServerLevel level, Trap trap,
                                BlockPos walk, Direction facing, RandomSource rnd, DungeonTheme theme) {
        for (Placement p : trap.build(level, walk, facing, rnd, theme)) {
            if (sel.contains(p.pos)) {
                out.add(p);
            }
        }
    }

    private static List<Trap> enabledTraps(DungeonConfig cfg) {
        List<Trap> list = new ArrayList<>();
        if (cfg.trapTypes.length > 0 && cfg.trapTypes[0]) {
            list.add(new PressurePlateArrowTrap());
        }
        if (cfg.trapTypes.length > 1 && cfg.trapTypes[1]) {
            list.add(new HiddenPitTrap());
        }
        if (cfg.trapTypes.length > 2 && cfg.trapTypes[2]) {
            list.add(new HiddenLavaTrap());
        }
        if (cfg.trapTypes.length > 3 && cfg.trapTypes[3]) {
            list.add(new SpawnerAmbushTrap());
        }
        if (cfg.trapTypes.length > 4 && cfg.trapTypes[4]) {
            list.add(new WallArrowVolleyTrap());
        }
        return list;
    }

    // ----- helpers -----

    private static void add(List<Placement> out, SelectionShape sel, BlockPos pos, BlockState state) {
        if (sel.contains(pos)) {
            out.add(Placement.of(pos.immutable(), state));
        }
    }

    /** Wrapper para builders que reciben una lista directa pero deben respetar la seleccion. */
    private static List<Placement> filteredList(List<Placement> out, SelectionShape sel) {
        return new java.util.AbstractList<Placement>() {
            @Override
            public boolean add(Placement p) {
                if (sel.contains(p.pos)) {
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
