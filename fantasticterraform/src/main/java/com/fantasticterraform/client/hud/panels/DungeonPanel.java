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
 * Pestana de Dungeons: generar estructuras tematicas. Configuracion primero, accion
 * principal al final. Layout denso de 14px (estilo FantasticCrates).
 */
public final class DungeonPanel implements HudPanel {

    private static final String[][] THEMES = {
            {"catacombs", "Catacumbas"}, {"ruined_fortress", "Fortaleza"}, {"spider_cave", "Aracnidos"},
            {"abandoned_castle", "Castillo"}, {"ancient_crypt", "Cripta"}, {"mystic_elven", "Elfica"},
            {"custom", "Personalizado"}
    };
    private static final String[] TIERS = {"Pequena", "Mediana", "Grande", "Epica"};
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
        int half = (width - 4) / 2;
        int fifth = (width - 16) / 5;
        int row = y;

        // --- Tema y tamano ---
        screen.section(x, row, "TEMA Y TAMANO");
        row += 11;
        screen.addRow(x, row, half, "Tema", screen.addButton(x, row, half - 45, TerraformPanelScreen.RH,
                themeName(), this::cycleTheme, "Estilo de la estructura."));
        screen.addRow(x + half + 4, row, half, "Tier", screen.addButton(x + half + 4, row, half - 45, TerraformPanelScreen.RH,
                TIERS[clamp(ClientToolState.genTier, 4)], () -> {
                    ClientToolState.genTier = (ClientToolState.genTier + 1) % 4;
                    PacketHandler.sendToServer(new ValidateDungeonSelectionPacket(ClientToolState.genTier));
                }, "Tamano requerido. Al cambiarlo se valida tu seleccion (ver pie del panel)."));
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Multinivel: " + onOff(ClientToolState.genMultiLevel),
                () -> ClientToolState.genMultiLevel = !ClientToolState.genMultiLevel, "Varios pisos conectados.");
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Pisos", 1, 5, ClientToolState.genLevels, true,
                "Numero de pisos si multinivel esta activo.", v -> ClientToolState.genLevels = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "Validar tamano de seleccion",
                () -> PacketHandler.sendToServer(new ValidateDungeonSelectionPacket(ClientToolState.genTier)),
                "Comprueba si tu seleccion cabe.");
        row += TerraformPanelScreen.RS + 2;

        // --- Trampas ---
        screen.section(x, row, "TRAMPAS");
        row += 11;
        screen.addRow(x, row, width, "Densidad", screen.addButton(x, row, 160, TerraformPanelScreen.RH,
                TRAP_DENSITY[clamp(ClientToolState.genTrapDensity, 4)],
                () -> ClientToolState.genTrapDensity = (ClientToolState.genTrapDensity + 1) % 4, "Cuantas trampas por sala."));
        row += TerraformPanelScreen.RS;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            String lbl = (ClientToolState.genTrapTypes[i] ? "\u25b6 " : "") + TRAP_LABELS[i];
            screen.addButton(x + i * (fifth + 4), row, fifth, TerraformPanelScreen.RH, lbl,
                    () -> ClientToolState.genTrapTypes[idx] = !ClientToolState.genTrapTypes[idx],
                    "Tipo de trampa: " + TRAP_LABELS[idx] + ".");
        }
        row += TerraformPanelScreen.RS + 2;

        // --- Jefe y botin ---
        screen.section(x, row, "JEFE Y BOTIN");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Jefe: " + onOff(ClientToolState.genBoss),
                () -> ClientToolState.genBoss = !ClientToolState.genBoss, "Encuentro de jefe en la sala mas lejana.");
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Cantidad", 1, 8, ClientToolState.genBossCount, true,
                "Cuantos mobs de jefe.", v -> ClientToolState.genBossCount = v.intValue());
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, width, "Entidad jefe", screen.addPicker(x, row, 200, TerraformPanelScreen.RH,
                () -> ClientToolState.genBossEntity, RegistryLists.entities(), false,
                "Entidad del jefe (vanilla o de mod).", s -> ClientToolState.genBossEntity = s));
        row += TerraformPanelScreen.RS;
        screen.addRow(x, row, half, "Botin tesoro", screen.addPicker(x, row, half - 75, TerraformPanelScreen.RH,
                () -> ClientToolState.genTreasureLoot, LOOT_TABLES, false,
                "Loot table de cofres de tesoro.", s -> ClientToolState.genTreasureLoot = s));
        screen.addRow(x + half + 4, row, half, "Botin jefe", screen.addPicker(x + half + 4, row, half - 65, TerraformPanelScreen.RH,
                () -> ClientToolState.genBossLoot, LOOT_TABLES, false,
                "Loot table del cofre del jefe.", s -> ClientToolState.genBossLoot = s));
        row += TerraformPanelScreen.RS + 2;

        // --- Avanzado ---
        screen.section(x, row, "AVANZADO");
        row += 11;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Bucles %", 0, 100, ClientToolState.genLoopDensity, true,
                "Pasillos extra que crean bucles.", v -> ClientToolState.genLoopDensity = v.intValue());
        screen.addRow(x + half + 4, row, half, "Semilla", screen.addEditBox(x + half + 4, row, half - 55, TerraformPanelScreen.RH,
                String.valueOf(ClientToolState.genSeed), "Semilla (0 = aleatoria).", s -> {
                    try {
                        ClientToolState.genSeed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.genSeed = 0L;
                    }
                }));
        row += TerraformPanelScreen.RS + 2;

        // --- Accion principal (al final, ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7a\u00a7l\u25b6 GENERAR DUNGEON", DungeonPanel::generate,
                "Genera la estructura completa. Se rechaza si la seleccion no cumple el tamano del tier.");
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
        return b ? "Si" : "No";
    }

    @Override
    public String status() {
        return ClientToolState.genValidationMsg;
    }
}
