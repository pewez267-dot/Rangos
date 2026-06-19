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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * CRIPTA ANTIGUA: una camara CEREMONIAL y estrictamente SIMETRICA con planta de
 * basilica (nave central + dos naves laterales), filas de SARCOFAGOS enfrentados y un
 * ABSIDE elevado al fondo con la tumba principal (el jefe).
 *
 * <p>La simetria es absoluta: cada elemento se coloca en pares espejados respecto al eje
 * longitudinal central. Es deliberadamente UNA sola gran sala (no multinivel): una cripta
 * ceremonial es un espacio unico y solemne; con mas altura disponible se eleva la boveda,
 * no se apilan plantas.</p>
 *
 * <p>Sellada por una unica cascara exterior hueca; los pilares conectan piso y boveda.</p>
 */
public final class CryptBuilder {

    private CryptBuilder() {
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
        if (x1 - x0 < 8 || z1 - z0 < 10) {
            return;
        }

        int availH = max.getY() - gy;
        int hallH = BuildUtil.clamp(availH - 1, 6, 11);

        BlockState wall = theme.wall();
        BlockState floor = theme.floor();
        BlockState ceiling = theme.ceiling();
        BlockState pillarB = theme.pillar();
        BlockState light = theme.light();
        BlockState accent = theme.accent();

        // 1) Cascara exterior sellada.
        BuildUtil.hollowRoom(out, sel, x0, gy, z0, x1, z1, hallH, wall, floor, ceiling);
        int ceilY = gy + hallH + 1;

        int cx = (x0 + x1) / 2;            // eje de simetria (X)
        int floorY = gy;                   // plano de piso (hollowRoom puso floor en gy)
        int walkY = gy + 1;                // nivel de caminado

        // 2) Alfombra ceremonial: pasillo central de acento, desde la entrada al absil.
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            BuildUtil.set(out, sel, cx, floorY, z, accent);
        }

        // 3) Naves laterales: dos filas de pilares simetricas que separan nave y naves.
        int aisle = Math.max(2, (x1 - x0) / 5);
        int colL = cx - aisle;
        int colR = cx + aisle;
        int pStartZ = z0 + 3;
        int pEndZ = z1 - 4;
        int pSpacing = 4;
        for (int z = pStartZ; z <= pEndZ; z += pSpacing) {
            ceremonialColumn(out, sel, colL, z, walkY, ceilY - 1, pillarB, accent);
            ceremonialColumn(out, sel, colR, z, walkY, ceilY - 1, pillarB, accent);
            // Arquitrabe entre columnas enfrentadas (viga de techo simetrica).
            for (int x = colL; x <= colR; x++) {
                BuildUtil.set(out, sel, x, ceilY - 1, z, wall);
            }
        }

        // 4) Sarcofagos en pares espejados, en las naves laterales.
        String mobId = mobId(theme);
        long lootSeed = cfg.seed;
        for (int z = pStartZ; z <= pEndZ - 1; z += pSpacing) {
            int leftX = (x0 + colL) / 2;       // centro de la nave lateral izquierda
            int rightX = 2 * cx - leftX;       // espejo exacto
            sarcophagus(out, sel, leftX, walkY, z, theme, light);
            sarcophagus(out, sel, rightX, walkY, z, theme, light);
            // Ofrenda ocasional (cofre a los pies de un sarcofago), en pares.
            if (rnd.nextDouble() < 0.30) {
                BuildUtil.chest(out, sel, new BlockPos(leftX, walkY, z + 2), cfg.normalLootTable, lootSeed++);
                BuildUtil.chest(out, sel, new BlockPos(rightX, walkY, z + 2), cfg.treasureLootTable, lootSeed++);
            }
            // Spawner velado tras una columna (par).
            if (rnd.nextDouble() < 0.22) {
                BuildUtil.spawner(out, sel, new BlockPos(leftX, walkY, z), mobId);
                BuildUtil.spawner(out, sel, new BlockPos(rightX, walkY, z), mobId);
            }
        }

        // 5) Candelabros simetricos a lo largo de la nave central.
        for (int z = z0 + 4; z <= z1 - 4; z += 6) {
            candelabrum(out, sel, cx - 1, walkY, z, pillarB, light);
            candelabrum(out, sel, cx + 1, walkY, z, pillarB, light);
        }

        // 6) Entrada ceremonial: arco centrado en el muro frontal (z0).
        for (int dx = -1; dx <= 1; dx++) {
            int h = (dx == 0) ? 4 : 3;
            for (int dy = 0; dy < h; dy++) {
                BuildUtil.air(out, sel, cx + dx, walkY + dy, z0);
            }
        }

        // 7) Absil: tarima elevada al fondo (z1) con la tumba principal y el jefe.
        int apseZ = z1 - 2;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BuildUtil.set(out, sel, cx + dx, walkY, apseZ + dz, accent);          // plataforma
                BuildUtil.set(out, sel, cx + dx, walkY + 1, apseZ + dz, dx == 0 ? accent : floor);
            }
        }
        // Tumba maestra (sarcofago monumental) centrada y elevada.
        masterTomb(out, sel, cx, walkY + 2, apseZ, theme, light);
        // Columnas-estandarte que flanquean el absil (simetricas).
        ceremonialColumn(out, sel, cx - 3, apseZ, walkY, ceilY - 1, pillarB, accent);
        ceremonialColumn(out, sel, cx + 3, apseZ, walkY, ceilY - 1, pillarB, accent);
        BuildUtil.set(out, sel, cx - 3, ceilY - 2, apseZ, light);
        BuildUtil.set(out, sel, cx + 3, ceilY - 2, apseZ, light);

        // Cofre del jefe y spawn.
        BuildUtil.chest(out, sel, new BlockPos(cx, walkY + 2, apseZ - 1), cfg.bossLootTable, lootSeed++);
        if (cfg.bossEnabled) {
            bossSpawns.add(new BlockPos(cx, walkY + 2, apseZ));
        }

        // 8) Luces de boveda simetricas (sea lanterns alineadas con la nave central).
        for (int z = z0 + 4; z <= z1 - 4; z += 5) {
            BuildUtil.set(out, sel, cx, ceilY, z, light);
        }
    }

    /** Columna ceremonial: cuerpo de pilar con base y capitel de acento (sellada). */
    private static void ceremonialColumn(List<Placement> out, SelectionShape sel, int x, int z, int y0, int y1,
                                         BlockState body, BlockState cap) {
        BuildUtil.set(out, sel, x, y0, z, cap);
        BuildUtil.pillar(out, sel, x, z, y0 + 1, y1 - 1, body);
        BuildUtil.set(out, sel, x, y1, z, cap);
    }

    /** Candelabro: pilar bajo rematado con luz. */
    private static void candelabrum(List<Placement> out, SelectionShape sel, int x, int y0, int z,
                                    BlockState body, BlockState light) {
        BuildUtil.set(out, sel, x, y0, z, body);
        BuildUtil.set(out, sel, x, y0 + 1, z, light);
    }

    /** Sarcofago orientado a lo largo de Z (2 de largo) con lapida y tapa de losa. */
    private static void sarcophagus(List<Placement> out, SelectionShape sel, int x, int y, int z,
                                    DungeonTheme theme, BlockState light) {
        BlockState body = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState lid = Blocks.STONE_BRICK_SLAB.defaultBlockState();
        // Cuerpo (2 bloques de largo).
        BuildUtil.set(out, sel, x, y, z, body);
        BuildUtil.set(out, sel, x, y, z + 1, body);
        // Tapa (losas encima).
        BuildUtil.set(out, sel, x, y + 1, z, lid);
        BuildUtil.set(out, sel, x, y + 1, z + 1, lid);
        // Lapida a la cabecera.
        BuildUtil.set(out, sel, x, y, z - 1, theme.pillar());
        if ((x + z) % 3 == 0) {
            BuildUtil.set(out, sel, x, y + 1, z - 1, light);
        }
    }

    /** Tumba maestra del absil: monumento central elevado coronado por una luz. */
    private static void masterTomb(List<Placement> out, SelectionShape sel, int cx, int y, int z,
                                   DungeonTheme theme, BlockState light) {
        BlockState body = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState lid = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BuildUtil.set(out, sel, cx + dx, y, z + dz, body);
            }
        }
        BuildUtil.set(out, sel, cx, y + 1, z, lid);
        BuildUtil.set(out, sel, cx - 1, y + 1, z, light);
        BuildUtil.set(out, sel, cx + 1, y + 1, z, light);
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
