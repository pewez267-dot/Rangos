package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.GenerateDungeonPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.ValidateDungeonSelectionPacket;

import java.util.Arrays;
import java.util.List;

/**
 * Pestaña de Dungeons: generar estructuras temáticas. Diseño limpio en secciones
 * (Tema, Estructura, Jefe y botín, Avanzado) con la acción principal destacada.
 */
public final class DungeonPanel implements HudPanel {

    private static final String[][] THEMES = {
            {"catacombs", "Catacumbas"}, {"ruined_fortress", "Fortaleza"}, {"spider_cave", "Arácnidos"},
            {"abandoned_castle", "Castillo"}, {"ancient_crypt", "Cripta"}, {"mystic_elven", "Élfica"},
            {"custom", "Personalizado"}
    };
    private static final String[] TIERS = {"Pequeña", "Mediana", "Grande", "Épica"};
    private static final String[] TRAP_DENSITY = {"Ninguna", "Baja", "Media", "Alta"};
    private static final String[] TRAP_LABELS = {"Flechas", "Foso", "Lava", "Spawner", "Descarga"};
    private static final List<String> LOOT_TABLES = Arrays.asList(
            "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/stronghold_corridor", "minecraft:chests/stronghold_library",
            "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple",
            "minecraft:chests/end_city_treasure", "minecraft:chests/nether_bridge",
            "minecraft:chests/buried_treasure", "minecraft:chests/bastion_treasure",
            "minecraft:chests/ruined_portal", "minecraft:chests/shipwreck_treasure");

    @Override
    public String title() {
        return "Dungeons";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 6) / 2;
        int fifth = (width - 8) / 5;
        int row = y;

        screen.addButton(x, row, width, 20, "\u00a7a\u00a7l\u25b6 GENERAR DUNGEON", DungeonPanel::generate,
                "Genera la estructura completa. Se rechaza si la selección no cumple el tamaño del tier.");
        row += 26;

        screen.addHeader(x, row, width, "TEMA Y TAMAÑO");
        row += 13;
        screen.addButton(x, row, half, 18, "Tema: \u00a7f" + themeName(), this::cycleTheme,
                "Estilo de la estructura (cada tema tiene arquitectura propia).");
        screen.addButton(x + half + 6, row, half, 18, "Tier: \u00a7f" + TIERS[clamp(ClientToolState.genTier, 4)], () -> {
            ClientToolState.genTier = (ClientToolState.genTier + 1) % 4;
            PacketHandler.sendToServer(new ValidateDungeonSelectionPacket(ClientToolState.genTier));
        }, "Tamaño requerido. Al cambiarlo se valida tu selección (ver pie del panel).");
        row += 20;
        screen.addButton(x, row, half, 18, "Validar tamaño", () -> PacketHandler.sendToServer(
                new ValidateDungeonSelectionPacket(ClientToolState.genTier)), "Comprueba si tu selección cabe.");
        screen.addButton(x + half + 6, row, half, 18, "Multinivel: " + onOff(ClientToolState.genMultiLevel),
                () -> ClientToolState.genMultiLevel = !ClientToolState.genMultiLevel, "Varios pisos conectados.");
        row += 20;
        screen.addSlider(x, row, width, 16, "Pisos", 1, 5, ClientToolState.genLevels, true,
                "Número de pisos si multinivel está activo.", v -> ClientToolState.genLevels = v.intValue());
        row += 22;

        screen.addHeader(x, row, width, "TRAMPAS");
        row += 13;
        screen.addButton(x, row, width, 18, "Densidad: \u00a7f" + TRAP_DENSITY[clamp(ClientToolState.genTrapDensity, 4)],
                () -> ClientToolState.genTrapDensity = (ClientToolState.genTrapDensity + 1) % 4, "Cuántas trampas por sala.");
        row += 20;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            screen.addButton(x + i * (fifth + 1), row, fifth, 16,
                    (ClientToolState.genTrapTypes[i] ? "\u00a7a" : "\u00a77") + TRAP_LABELS[i],
                    () -> ClientToolState.genTrapTypes[idx] = !ClientToolState.genTrapTypes[idx],
                    "Tipo de trampa: " + TRAP_LABELS[idx] + ".");
        }
        row += 22;

        screen.addHeader(x, row, width, "JEFE Y BOTÍN");
        row += 13;
        screen.addButton(x, row, half, 18, "Jefe: " + onOff(ClientToolState.genBoss),
                () -> ClientToolState.genBoss = !ClientToolState.genBoss, "Encuentro de jefe en la sala más lejana.");
        screen.addSlider(x + half + 6, row, half, 16, "Cantidad", 1, 8, ClientToolState.genBossCount, true,
                "Cuántos mobs de jefe.", v -> ClientToolState.genBossCount = v.intValue());
        row += 20;
        screen.addPicker(x, row, width, 18, "Entidad del jefe", () -> ClientToolState.genBossEntity,
                RegistryLists.entities(), false, "Entidad del jefe (vanilla o de mod).", s -> ClientToolState.genBossEntity = s);
        row += 20;
        screen.addPicker(x, row, half, 18, "Botín tesoro", () -> ClientToolState.genTreasureLoot,
                LOOT_TABLES, false, "Loot table de cofres de tesoro.", s -> ClientToolState.genTreasureLoot = s);
        screen.addPicker(x + half + 6, row, half, 18, "Botín jefe", () -> ClientToolState.genBossLoot,
                LOOT_TABLES, false, "Loot table del cofre del jefe.", s -> ClientToolState.genBossLoot = s);
        row += 22;

        screen.addHeader(x, row, width, "AVANZADO");
        row += 13;
        screen.addSlider(x, row, half, 16, "Bucles %", 0, 100, ClientToolState.genLoopDensity, true,
                "Pasillos extra que crean bucles.", v -> ClientToolState.genLoopDensity = v.intValue());
        screen.addEditBox(x + half + 6, row, half, 16, String.valueOf(ClientToolState.genSeed),
                "Semilla (0 = aleatoria, distinta cada vez).", s -> {
                    try {
                        ClientToolState.genSeed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.genSeed = 0L;
                    }
                });
    }

    private static void generate() {
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

    private static int clamp(int v, int len) {
        return (v >= 0 && v < len) ? v : 0;
    }

    private static String onOff(boolean b) {
        return b ? "\u00a7aSí" : "\u00a77No";
    }

    @Override
    public String status() {
        return ClientToolState.genValidationMsg;
    }
}
