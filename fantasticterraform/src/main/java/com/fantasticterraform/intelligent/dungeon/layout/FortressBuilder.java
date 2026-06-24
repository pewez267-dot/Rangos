package com.fantasticterraform.intelligent.dungeon.layout;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * FORTALEZA EN RUINAS: grandes SALONES ABIERTOS con hileras de PILARES macizos que
 * sostienen una boveda alta. No es un dedalo ni una caja pequena: es un espacio
 * monumental, despejado y simetrico, con aire de ruina (almenas rotas, claraboyas en la
 * boveda, escombros).
 *
 * <p>Sellado por construccion: una unica cascara exterior hueca envuelve TODO el salon;
 * los pilares conectan piso y techo (nada flota); una galeria superior (mezzanine) rodea
 * el salon cuando la altura lo permite, accesible por escaleras reales.</p>
 */
public final class FortressBuilder {

    private FortressBuilder() {
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
        int hallH = BuildUtil.clamp(availH - 1, 6, 10);   // altura interior libre del salon
        boolean mezzanine = availH >= 13;

        BlockState wall = theme.wall();
        BlockState floor = theme.floor();
        BlockState ceiling = theme.ceiling();
        BlockState pillarB = theme.pillar();
        BlockState light = theme.light();
        BlockState accent = theme.accent();

        // 1) Cascara exterior hueca: piso, boveda y cuatro muros macizos sellados.
        BuildUtil.hollowRoom(out, sel, x0, gy, z0, x1, z1, hallH, wall, floor, ceiling);
        int ceilY = gy + hallH + 1;

        // 1.5) Detalle arquitectonico: cornisa bajo la boveda, pilastras adosadas y zocalo.
        BlockState slab = Blocks.STONE_BRICK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP);
        BuildUtil.cornice(out, sel, x0, z0, x1, z1, ceilY - 1, slab);
        BuildUtil.floorBorder(out, sel, x0, z0, x1, z1, gy, accent);
        for (int x = x0 + 4; x <= x1 - 4; x += 6) {
            BuildUtil.pilaster(out, sel, x, z0 + 1, gy + 1, ceilY - 1, pillarB, accent);
            BuildUtil.pilaster(out, sel, x, z1 - 1, gy + 1, ceilY - 1, pillarB, accent);
        }
        for (int z = z0 + 4; z <= z1 - 4; z += 6) {
            BuildUtil.pilaster(out, sel, x0 + 1, z, gy + 1, ceilY - 1, pillarB, accent);
            BuildUtil.pilaster(out, sel, x1 - 1, z, gy + 1, ceilY - 1, pillarB, accent);
        }

        // 2) Almenas rotas sobre el perimetro exterior (silueta de ruina).
        BlockState merlon = wall;
        for (int x = x0; x <= x1; x++) {
            if (rnd.nextDouble() < 0.5) {
                BuildUtil.set(out, sel, x, ceilY + 1, z0, merlon);
            }
            if (rnd.nextDouble() < 0.5) {
                BuildUtil.set(out, sel, x, ceilY + 1, z1, merlon);
            }
        }
        for (int z = z0; z <= z1; z++) {
            if (rnd.nextDouble() < 0.5) {
                BuildUtil.set(out, sel, x0, ceilY + 1, z, merlon);
            }
            if (rnd.nextDouble() < 0.5) {
                BuildUtil.set(out, sel, x1, ceilY + 1, z, merlon);
            }
        }

        // 3) Hileras de pilares macizos (2x2) en rejilla regular, sosteniendo la boveda.
        int innerX0 = x0 + 3;
        int innerX1 = x1 - 3;
        int innerZ0 = z0 + 3;
        int innerZ1 = z1 - 3;
        int spacing = 6;
        for (int px = innerX0; px <= innerX1; px += spacing) {
            for (int pz = innerZ0; pz <= innerZ1; pz += spacing) {
                buildPillar(out, sel, px, pz, gy + 1, ceilY - 1, pillarB, accent, rnd);
                // Brasero al pie de algunos pilares.
                if (rnd.nextDouble() < 0.4) {
                    BuildUtil.set(out, sel, px, gy + 1, pz - 1, light);
                }
            }
        }

        // 4) Ventanales altos rotos en los muros largos (entra luz, refuerza la ruina).
        int winY = gy + hallH - 2;
        for (int x = x0 + 4; x <= x1 - 4; x += 5) {
            for (int dy = 0; dy < 2; dy++) {
                BuildUtil.air(out, sel, x, winY + dy, z0);
                BuildUtil.air(out, sel, x, winY + dy, z1);
            }
        }

        // 5) Claraboyas: huecos en la boveda (techo roto) con escombros debajo.
        int holes = Math.max(2, (x1 - x0) * (z1 - z0) / 400);
        for (int i = 0; i < holes; i++) {
            int hx = x0 + 2 + rnd.nextInt(Math.max(1, x1 - x0 - 3));
            int hz = z0 + 2 + rnd.nextInt(Math.max(1, z1 - z0 - 3));
            BuildUtil.carveBox(out, sel, hx, ceilY, hz, hx + 1, ceilY, hz + 1);
            // Escombros en el piso bajo la claraboya.
            BuildUtil.set(out, sel, hx, gy + 1, hz, Blocks.COBBLESTONE.defaultBlockState());
            if (rnd.nextBoolean()) {
                BuildUtil.set(out, sel, hx + 1, gy + 1, hz, accent);
            }
        }

        // 6) Puerta monumental (arco) en el muro frontal (z0), centrada.
        int gateX = (x0 + x1) / 2;
        for (int dx = -2; dx <= 2; dx++) {
            int h = (Math.abs(dx) == 2) ? 3 : (Math.abs(dx) == 1 ? 4 : 5);
            for (int dy = 0; dy < h; dy++) {
                BuildUtil.air(out, sel, gateX + dx, gy + 1 + dy, z0);
            }
        }

        // 7) Tarima/trono del jefe al fondo (z1), elevada y flanqueada por pilares-estandarte.
        int daisZ = z1 - 2;
        int daisX = gateX;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BuildUtil.set(out, sel, daisX + dx, gy + 1, daisZ + dz, accent);
            }
        }
        // Trono (escalera mirando a la entrada).
        BlockState throne = throneBlock();
        BuildUtil.set(out, sel, daisX, gy + 2, daisZ, throne);
        BuildUtil.pillar(out, sel, daisX - 2, daisZ, gy + 2, ceilY - 1, pillarB);
        BuildUtil.pillar(out, sel, daisX + 2, daisZ, gy + 2, ceilY - 1, pillarB);
        BuildUtil.set(out, sel, daisX - 2, ceilY - 1, daisZ, light);
        BuildUtil.set(out, sel, daisX + 2, ceilY - 1, daisZ, light);

        // 8) Mezzanine: galeria perimetral a media altura con escaleras (si hay altura).
        if (mezzanine) {
            int galY = gy + hallH / 2 + 1;
            buildGallery(out, sel, x0, z0, x1, z1, galY, floor, wall, light);
            // Escaleras de acceso desde el piso a la galeria, en dos esquinas.
            BlockState step = floor;
            com.fantasticterraform.intelligent.dungeon.multilevel.StaircaseBuilder.build(
                    BuildUtil.selFilteredList(out, sel),
                    new BlockPos(x0 + 2, gy + 1, z0 + 2), new BlockPos(x0 + 2 + (galY - gy), galY, z0 + 2), step);
            com.fantasticterraform.intelligent.dungeon.multilevel.StaircaseBuilder.build(
                    BuildUtil.selFilteredList(out, sel),
                    new BlockPos(x1 - 2, gy + 1, z1 - 2), new BlockPos(x1 - 2 - (galY - gy), galY, z1 - 2), step);
        }

        // 9) Contenido: cofres tras pilares, spawners, cofre/jefe en la tarima.
        String mobId = mobId(theme);
        long lootSeed = cfg.seed;
        for (int px = innerX0; px <= innerX1; px += spacing) {
            for (int pz = innerZ0; pz <= innerZ1; pz += spacing) {
                double k = rnd.nextDouble();
                if (k < 0.18) {
                    BuildUtil.chest(out, sel, new BlockPos(px + 1, gy + 1, pz),
                            cfg.normalLootTable, lootSeed++);
                } else if (k < 0.40) {
                    BuildUtil.spawner(out, sel, new BlockPos(px - 1, gy + 1, pz), mobId);
                }
            }
        }
        BuildUtil.chest(out, sel, new BlockPos(daisX - 1, gy + 2, daisZ), cfg.bossLootTable, lootSeed++);
        BuildUtil.chest(out, sel, new BlockPos(daisX + 1, gy + 2, daisZ), cfg.treasureLootTable, lootSeed++);
        if (cfg.bossEnabled) {
            bossSpawns.add(new BlockPos(daisX, gy + 2, daisZ));
        }
    }

    /** Pilar macizo 2x2 con base ensanchada, fuste y capitel; algunos quedan rotos (ruina). */
    private static void buildPillar(List<Placement> out, SelectionShape sel, int x, int z, int y0, int y1,
                                    BlockState body, BlockState cap, RandomSource rnd) {
        int top = y1;
        // Ruina: ~25% de los pilares estan quebrados a media altura.
        if (rnd.nextDouble() < 0.25) {
            top = y0 + (y1 - y0) / 2 + rnd.nextInt(Math.max(1, (y1 - y0) / 3));
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BuildUtil.pillar(out, sel, x + dx, z + dz, y0, top, body);
            }
        }
        // Base ensanchada (plinto 4x4 de acento al pie).
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                if (dx == -1 || dx == 2 || dz == -1 || dz == 2) {
                    BuildUtil.set(out, sel, x + dx, y0, z + dz, cap);
                }
            }
        }
        if (top >= y1) {
            // Capitel: anillo 4x4 de acento bajo la boveda + remate 2x2.
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    if (dx == -1 || dx == 2 || dz == -1 || dz == 2) {
                        BuildUtil.set(out, sel, x + dx, y1 - 1, z + dz, cap);
                    }
                }
            }
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BuildUtil.set(out, sel, x + dx, y1, z + dz, cap);
                }
            }
        }
    }

    /** Galeria perimetral (ring de piso a media altura) con barandilla y luces. */
    private static void buildGallery(List<Placement> out, SelectionShape sel, int x0, int z0, int x1, int z1,
                                     int galY, BlockState floor, BlockState rail, BlockState light) {
        int depth = 2; // ancho del pasillo de galeria
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int d = 1; d <= depth; d++) {
                BuildUtil.set(out, sel, x, galY, z0 + d, floor);
                BuildUtil.set(out, sel, x, galY, z1 - d, floor);
            }
            BuildUtil.set(out, sel, x, galY + 1, z0 + depth, rail);
            BuildUtil.set(out, sel, x, galY + 1, z1 - depth, rail);
        }
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            for (int d = 1; d <= depth; d++) {
                BuildUtil.set(out, sel, x0 + d, galY, z, floor);
                BuildUtil.set(out, sel, x1 - d, galY, z, floor);
            }
            BuildUtil.set(out, sel, x0 + depth, galY + 1, z, rail);
            BuildUtil.set(out, sel, x1 - depth, galY + 1, z, rail);
        }
        // Luces en la barandilla.
        for (int x = x0 + 3; x <= x1 - 3; x += 7) {
            BuildUtil.set(out, sel, x, galY + 1, z0 + 1, light);
            BuildUtil.set(out, sel, x, galY + 1, z1 - 1, light);
        }
    }

    private static BlockState throneBlock() {
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
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
