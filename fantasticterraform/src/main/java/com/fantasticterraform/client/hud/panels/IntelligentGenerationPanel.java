package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.intelligent.population.PopulationManager;
import com.fantasticterraform.network.GenerateBiomeTerrainPacket;
import com.fantasticterraform.network.GenerateDungeonPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.PopulateSelectionPacket;
import com.fantasticterraform.network.ValidateDungeonSelectionPacket;

import java.util.Arrays;
import java.util.List;

/** Panel de Generación Inteligente: biomas personalizables, poblamiento y dungeons por grafos. */
public final class IntelligentGenerationPanel implements HudPanel {

    private static final String[] BIOME_STYLES = {"Llano", "Colinas", "Montañas", "Cañón", "Islas", "Meseta", "Dunas", "Volcánico"};
    private static final String[][] THEMES = {
            {"catacombs", "Catacumbas"}, {"ruined_fortress", "Fortaleza"}, {"spider_cave", "Arácnidos"},
            {"abandoned_castle", "Castillo"}, {"ancient_crypt", "Cripta"}, {"mystic_elven", "Élfica"},
            {"custom", "Personalizado"}
    };
    private static final String[] TIERS = {"Pequena", "Mediana", "Grande", "Epica"};
    private static final String[] TRAP_DENSITY = {"Ninguna", "Baja", "Media", "Alta"};
    private static final String[] TRAP_LABELS = {"Flec", "Foso", "Lava", "Spwn", "Desc"};
    private static final List<String> LOOT_TABLES = Arrays.asList(
            "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/stronghold_corridor", "minecraft:chests/stronghold_library",
            "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple",
            "minecraft:chests/end_city_treasure", "minecraft:chests/nether_bridge",
            "minecraft:chests/buried_treasure", "minecraft:chests/bastion_treasure",
            "minecraft:chests/ruined_portal", "minecraft:chests/shipwreck_treasure");

    @Override
    public String title() {
        return "Generación";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int fifth = (width - 8) / 5;
        int row = y;

        // ===== BIOMAS =====
        screen.addButton(x, row, width, 14, "\u00a7eBIOMA: " + BIOME_STYLES[ClientToolState.biomeStyle],
                () -> ClientToolState.biomeStyle = (ClientToolState.biomeStyle + 1) % BIOME_STYLES.length,
                "Estilo de relieve: Llano, Colinas, Montañas, Canon o Islas. Cambia drasticamente el terreno.");
        row += 16;
        screen.addButton(x, row, width, 14, "Tipo: " + biomeTypeName(),
                () -> ClientToolState.biomeForced = ClientToolState.biomeForced + 1 >= com.fantasticterraform.intelligent.biome.BiomeType.values().length ? -1 : ClientToolState.biomeForced + 1,
                "Bioma a generar. 'Auto (multi)' mezcla biomas por clima; o elige uno fijo (define su suelo y poblacion).");
        row += 16;
        screen.addButton(x, row, width, 14, "Auto-poblar: " + on(ClientToolState.biomeAutoPopulate),
                () -> ClientToolState.biomeAutoPopulate = !ClientToolState.biomeAutoPopulate,
                "Si está activo, al generar el terreno se puebla solo según el bioma (árboles, flores, etc.).");
        row += 16;
        screen.addButton(x, row, width, 14, "Rios reales: " + on(ClientToolState.biomeRivers),
                () -> ClientToolState.biomeRivers = !ClientToolState.biomeRivers,
                "Talla una red de rios con cauce en U (valle suave + agua + orillas de arena/grava) en cualquier estilo.");
        row += 16;
        screen.addButton(x, row, width, 14, "Modo: " + (ClientToolState.biomeMode == 1 ? "Sobrescribir terreno" : "Generar relieve nuevo"),
                () -> ClientToolState.biomeMode = ClientToolState.biomeMode == 1 ? 0 : 1,
                "Generar = crea relieve nuevo por ruido y lo funde con los bordes del terreno existente. "
                        + "Sobrescribir = mantiene el relieve actual de la selección y solo le aplica el bioma (repinta y puebla).");
        row += 16;
        screen.addSlider(x, row, half, 14, "Relieve", 0, 1, ClientToolState.biomeAmplitude, false,
                "Fuerza del relieve (0 = casi plano, 1 = muy montanoso).", v -> ClientToolState.biomeAmplitude = v);
        screen.addSlider(x + half + 4, row, half, 14, "Mar", 0.05, 0.9, ClientToolState.biomeSea, false,
                "Altura del nivel del mar (fraccion de la selección).", v -> ClientToolState.biomeSea = v);
        row += 16;
        screen.addSlider(x, row, width, 14, "Tamaño formas", 0.001, 0.02, ClientToolState.biomeFeatureScale, false,
                "Tamaño de montanas/colinas: menor = formas mas grandes y separadas.", v -> ClientToolState.biomeFeatureScale = v);
        row += 16;
        screen.addButton(x, row, width, 14, "Suelo: " + (ClientToolState.biomeUseCustom ? "Personalizado" : "Automático"),
                () -> ClientToolState.biomeUseCustom = !ClientToolState.biomeUseCustom,
                "Automático = el suelo se decide por clima (cesped/arena/nieve...). Personalizado = usa TUS bloques.");
        row += 16;
        screen.addPicker(x, row, width, 14, "Superficie", () -> ClientToolState.biomeSurface,
                RegistryLists.blocks(), true, "Bloque de superficie (si 'Suelo' es Personalizado).",
                s -> ClientToolState.biomeSurface = s);
        row += 16;
        screen.addPicker(x, row, half, 14, "Subsuelo", () -> ClientToolState.biomeSub,
                RegistryLists.blocks(), true, "Bloque bajo la superficie.", s -> ClientToolState.biomeSub = s);
        screen.addPicker(x + half + 4, row, half, 14, "Roca", () -> ClientToolState.biomeStone,
                RegistryLists.blocks(), true, "Bloque de relleno profundo.", s -> ClientToolState.biomeStone = s);
        row += 16;
        screen.addButton(x, row, width, 16, "Generar terreno (bioma)", IntelligentGenerationPanel::generateBiome,
                "Genera el terreno con el estilo y bloques elegidos. Semilla 0 = distinto cada vez.");
        row += 22;

        // ===== POBLAMIENTO =====
        screen.addButton(x, row, fifth * 2, 14, "Árboles: " + on(ClientToolState.popTrees),
                () -> ClientToolState.popTrees = !ClientToolState.popTrees, "Árboles según clima (roble/abedul/pino/jungla/acacia).");
        screen.addButton(x + fifth * 2 + 2, row, fifth * 2, 14, "Flores: " + on(ClientToolState.popFlowers),
                () -> ClientToolState.popFlowers = !ClientToolState.popFlowers, "Muchos tipos de flores (incluidas dobles).");
        row += 16;
        screen.addButton(x, row, fifth * 2, 14, "Hierba: " + on(ClientToolState.popGrass),
                () -> ClientToolState.popGrass = !ClientToolState.popGrass, "Hierba alta, helechos y helechos grandes.");
        screen.addButton(x + fifth * 2 + 2, row, fifth * 2, 14, "Setas: " + on(ClientToolState.popMushrooms),
                () -> ClientToolState.popMushrooms = !ClientToolState.popMushrooms, "Setas rojas y marrones.");
        row += 16;
        screen.addButton(x, row, fifth * 2, 14, "Desierto: " + on(ClientToolState.popDesert),
                () -> ClientToolState.popDesert = !ClientToolState.popDesert, "Cactus y arbustos secos sobre arena.");
        screen.addButton(x + fifth * 2 + 2, row, fifth * 2, 14, "Agua: " + on(ClientToolState.popWater),
                () -> ClientToolState.popWater = !ClientToolState.popWater, "Cana de azucar y nenufares junto al agua.");
        row += 16;
        screen.addButton(x, row, fifth * 2, 14, "Rocas: " + on(ClientToolState.popRocks),
                () -> ClientToolState.popRocks = !ClientToolState.popRocks, "Cantos rodados musgosos en superficie.");
        screen.addButton(x + fifth * 2 + 2, row, fifth * 2, 14, "Cristales: " + on(ClientToolState.popCrystals),
                () -> ClientToolState.popCrystals = !ClientToolState.popCrystals, "Cristales de amatista sobre piedra.");
        row += 16;
        screen.addButton(x, row, width, 14, "Vetas de mineral: " + on(ClientToolState.popOres),
                () -> ClientToolState.popOres = !ClientToolState.popOres,
                "Esparce vetas de mineral en la roca por profundidad (carbon/hierro/oro/diamante/esmeralda, con deepslate).");
        row += 16;
        screen.addButton(x, row, width, 16, "Poblar selección", IntelligentGenerationPanel::populate,
                "Aplica todas las categorías activas sobre el terreno existente, según clima y densidad.");
        row += 22;

        // ===== DUNGEON =====
        screen.addButton(x, row, half, 14, "Tema: " + themeName(), this::cycleTheme,
                "Tema (paleta/mobs/decoración). 'Personalizado' usa tus bloques de la pestana de tema.");
        screen.addButton(x + half + 4, row, half, 14, "Tier: " + TIERS[ClientToolState.genTier], () -> {
            ClientToolState.genTier = (ClientToolState.genTier + 1) % 4;
            PacketHandler.sendToServer(new ValidateDungeonSelectionPacket(ClientToolState.genTier));
        }, "Tamaño. Al cambiarlo se válida tu selección (ver pie del panel).");
        row += 16;
        screen.addButton(x, row, half, 14, "Multinivel: " + on(ClientToolState.genMultiLevel),
                () -> ClientToolState.genMultiLevel = !ClientToolState.genMultiLevel, "Varios pisos conectados (Grande/Epica).");
        screen.addSlider(x + half + 4, row, half, 14, "Pisos", 1, 5, ClientToolState.genLevels, true,
                "Número de pisos si multinivel está activo.", v -> ClientToolState.genLevels = v.intValue());
        row += 16;
        screen.addButton(x, row, width, 14, "Trampas: " + TRAP_DENSITY[ClientToolState.genTrapDensity],
                () -> ClientToolState.genTrapDensity = (ClientToolState.genTrapDensity + 1) % 4,
                "Densidad de trampas por sala.");
        row += 16;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            screen.addButton(x + i * (fifth + 1), row, fifth, 14,
                    TRAP_LABELS[i] + (ClientToolState.genTrapTypes[i] ? "+" : "-"),
                    () -> ClientToolState.genTrapTypes[idx] = !ClientToolState.genTrapTypes[idx],
                    "Tipo de trampa: Flechas/Foso/Lava/Spawner/Descarga.");
        }
        row += 16;
        screen.addButton(x, row, half, 14, "Jefe: " + on(ClientToolState.genBoss),
                () -> ClientToolState.genBoss = !ClientToolState.genBoss, "Encuentro de jefe en la sala mas lejana.");
        screen.addSlider(x + half + 4, row, half, 14, "Cant.", 1, 8, ClientToolState.genBossCount, true,
                "Cuantos mobs de jefe.", v -> ClientToolState.genBossCount = v.intValue());
        row += 16;
        screen.addPicker(x, row, width, 14, "Jefe", () -> ClientToolState.genBossEntity,
                RegistryLists.entities(), false, "Entidad del jefe (vanilla o de mod).", s -> ClientToolState.genBossEntity = s);
        row += 16;
        screen.addPicker(x, row, width, 14, "Loot tesoro", () -> ClientToolState.genTreasureLoot,
                LOOT_TABLES, false, "Loot table de cofres de tesoro.", s -> ClientToolState.genTreasureLoot = s);
        row += 16;
        screen.addPicker(x, row, width, 14, "Loot jefe", () -> ClientToolState.genBossLoot,
                LOOT_TABLES, false, "Loot table del cofre del jefe.", s -> ClientToolState.genBossLoot = s);
        row += 16;
        screen.addSlider(x, row, half, 14, "Loops", 0, 100, ClientToolState.genLoopDensity, true,
                "Porcentaje de pasillos extra que crean bucles.", v -> ClientToolState.genLoopDensity = v.intValue());
        screen.addEditBox(x + half + 4, row, half, 14, String.valueOf(ClientToolState.genSeed),
                "Semilla (0 = aleatoria, distinta cada vez).", s -> {
                    try {
                        ClientToolState.genSeed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.genSeed = 0L;
                    }
                });
        row += 16;
        screen.addButton(x, row, half, 16, "Validar", () -> PacketHandler.sendToServer(
                new ValidateDungeonSelectionPacket(ClientToolState.genTier)), "Comprueba el tamaño de la selección.");
        screen.addButton(x + half + 4, row, half, 16, "Generar Dungeon", IntelligentGenerationPanel::generateDungeon,
                "Genera la dungeon completa. Se rechaza si la selección no cumple el tamaño.");
    }

    private static void generateBiome() {
        PacketHandler.sendToServer(new GenerateBiomeTerrainPacket(
                ClientToolState.biomeStyle, ClientToolState.biomeFeatureScale, ClientToolState.biomeAmplitude,
                ClientToolState.biomeSea, ClientToolState.biomeUseCustom, ClientToolState.biomeSurface,
                ClientToolState.biomeSub, ClientToolState.biomeStone, ClientToolState.genSeed,
                ClientToolState.biomeForced, ClientToolState.biomeAutoPopulate, ClientToolState.biomeRivers,
                ClientToolState.biomeMode));
    }

    private static String biomeTypeName() {
        if (ClientToolState.biomeForced < 0) {
            return "Auto (multi)";
        }
        com.fantasticterraform.intelligent.biome.BiomeType[] v = com.fantasticterraform.intelligent.biome.BiomeType.values();
        int i = ClientToolState.biomeForced;
        return i < v.length ? v[i].displayName() : "Auto (multi)";
    }

    private static void populate() {
        int mask = 0;
        mask |= ClientToolState.popTrees ? PopulationManager.TREES : 0;
        mask |= ClientToolState.popFlowers ? PopulationManager.FLOWERS : 0;
        mask |= ClientToolState.popGrass ? PopulationManager.GRASS : 0;
        mask |= ClientToolState.popMushrooms ? PopulationManager.MUSHROOMS : 0;
        mask |= ClientToolState.popDesert ? PopulationManager.DESERT : 0;
        mask |= ClientToolState.popWater ? PopulationManager.WATER : 0;
        mask |= ClientToolState.popRocks ? PopulationManager.ROCKS : 0;
        mask |= ClientToolState.popCrystals ? PopulationManager.CRYSTALS : 0;
        mask |= ClientToolState.popOres ? PopulationManager.ORES : 0;
        PacketHandler.sendToServer(new PopulateSelectionPacket(mask, ClientToolState.genSeed));
    }

    private static void generateDungeon() {
        String[] palette = {
                ClientToolState.customWall, ClientToolState.customFloor, ClientToolState.customCeiling,
                ClientToolState.customPillar, ClientToolState.customLight, ClientToolState.customAccent
        };
        PacketHandler.sendToServer(new GenerateDungeonPacket(
                ClientToolState.genTheme, ClientToolState.genTier, ClientToolState.genMultiLevel,
                ClientToolState.genLevels, ClientToolState.genTrapDensity, ClientToolState.genTrapTypes.clone(),
                ClientToolState.genBoss, ClientToolState.genBossEntity, ClientToolState.genBossCount,
                ClientToolState.genTreasureLoot, ClientToolState.genBossLoot, ClientToolState.genNormalLoot,
                ClientToolState.genSeed, ClientToolState.genLoopDensity, palette, ClientToolState.customMob));
    }

    private void cycleTheme() {
        int idx = 0;
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i][0].equals(ClientToolState.genTheme)) {
                idx = i;
                break;
            }
        }
        ClientToolState.genTheme = THEMES[(idx + 1) % THEMES.length][0];
    }

    private static String themeName() {
        for (String[] t : THEMES) {
            if (t[0].equals(ClientToolState.genTheme)) {
                return t[1];
            }
        }
        return THEMES[0][1];
    }

    private static String on(boolean b) {
        return b ? "\u00a7aSi" : "\u00a77No";
    }

    @Override
    public String status() {
        return ClientToolState.genValidationMsg;
    }
}
