package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.GenerateBiomeTerrainPacket;
import com.fantasticterraform.network.GenerateDungeonPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.PopulateSelectionPacket;
import com.fantasticterraform.network.ValidateDungeonSelectionPacket;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Arrays;
import java.util.List;

/** Panel de Generacion Inteligente: biomas por ruido, poblamiento y dungeons por grafos. */
public final class IntelligentGenerationPanel implements HudPanel {

    private static final String[][] THEMES = {
            {"catacombs", "Catacumbas"},
            {"ruined_fortress", "Fortaleza"},
            {"spider_cave", "Aracnidos"},
            {"abandoned_castle", "Castillo"},
            {"ancient_crypt", "Cripta"},
            {"mystic_elven", "Elfica"},
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
        return "Generacion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int fifth = (width - 8) / 5;
        int row = y;

        // --- BIOMAS ---
        screen.addSlider(x, row, width, 14, "Relieve", 0.001, 0.02, ClientToolState.biomeContScale, false,
                "Escala del ruido de continentalidad: menor = montanas mas grandes y separadas.",
                v -> ClientToolState.biomeContScale = v);
        row += 16;
        screen.addButton(x, row, width, 16, "Generar terreno (biomas)", () -> PacketHandler.sendToServer(
                        new GenerateBiomeTerrainPacket(ClientToolState.biomeContScale, ClientToolState.biomeEroScale,
                                ClientToolState.biomeMoistScale, ClientToolState.biomeTempScale, ClientToolState.genSeed)),
                "Rellena la seleccion con terreno natural (altura + superficie por 4 capas de ruido).");
        row += 20;

        // --- POBLAMIENTO ---
        screen.addButton(x, row, half, 14, "Arboles: " + on(ClientToolState.popTrees),
                () -> ClientToolState.popTrees = !ClientToolState.popTrees, "Coloca arboles en cesped con poca pendiente.");
        screen.addButton(x + half + 4, row, half, 14, "Rocas: " + on(ClientToolState.popRocks),
                () -> ClientToolState.popRocks = !ClientToolState.popRocks, "Coloca rocas sueltas en superficie.");
        row += 16;
        screen.addButton(x, row, half, 14, "Plantas: " + on(ClientToolState.popVegetation),
                () -> ClientToolState.popVegetation = !ClientToolState.popVegetation, "Coloca hierba/flores segun humedad.");
        screen.addButton(x + half + 4, row, half, 14, "Cristales: " + on(ClientToolState.popCrystals),
                () -> ClientToolState.popCrystals = !ClientToolState.popCrystals, "Coloca cristales sobre piedra expuesta.");
        row += 16;
        screen.addButton(x, row, width, 16, "Poblar seleccion", () -> PacketHandler.sendToServer(
                        new PopulateSelectionPacket(ClientToolState.popTrees, ClientToolState.popRocks,
                                ClientToolState.popVegetation, ClientToolState.popCrystals, ClientToolState.genSeed)),
                "Aplica las reglas de poblamiento activas sobre el terreno existente.");
        row += 20;

        // --- DUNGEON ---
        screen.addButton(x, row, half, 14, "Tema: " + themeName(), this::cycleTheme,
                "Cambia el tema (paleta/mobs/decoracion). 'Personalizado' usa tus bloques.");
        screen.addButton(x + half + 4, row, half, 14, "Tier: " + TIERS[ClientToolState.genTier], () -> {
            ClientToolState.genTier = (ClientToolState.genTier + 1) % 4;
            PacketHandler.sendToServer(new ValidateDungeonSelectionPacket(ClientToolState.genTier));
        }, "Tamano de la dungeon. Al cambiarlo se valida tu seleccion automaticamente.");
        row += 16;
        screen.addButton(x, row, half, 14, "Multinivel: " + on(ClientToolState.genMultiLevel),
                () -> ClientToolState.genMultiLevel = !ClientToolState.genMultiLevel,
                "Reparte las salas en varios pisos conectados (solo Grande/Epica).");
        screen.addSlider(x + half + 4, row, half, 14, "Pisos", 1, 5, ClientToolState.genLevels, true,
                "Numero de pisos deseado si multinivel esta activo.", v -> ClientToolState.genLevels = v.intValue());
        row += 16;
        screen.addButton(x, row, width, 14, "Trampas: " + TRAP_DENSITY[ClientToolState.genTrapDensity],
                () -> ClientToolState.genTrapDensity = (ClientToolState.genTrapDensity + 1) % 4,
                "Densidad de trampas por sala (ninguna/baja/media/alta).");
        row += 16;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            screen.addButton(x + i * (fifth + 1), row, fifth, 14,
                    TRAP_LABELS[i] + (ClientToolState.genTrapTypes[i] ? "+" : "-"),
                    () -> ClientToolState.genTrapTypes[idx] = !ClientToolState.genTrapTypes[idx],
                    "Habilita/inhabilita este tipo de trampa: Flechas/Foso/Lava/Spawner/Descarga.");
        }
        row += 16;
        screen.addButton(x, row, half, 14, "Jefe: " + on(ClientToolState.genBoss),
                () -> ClientToolState.genBoss = !ClientToolState.genBoss, "Coloca un encuentro de jefe en la sala mas lejana.");
        screen.addSlider(x + half + 4, row, half, 14, "Cant.", 1, 8, ClientToolState.genBossCount, true,
                "Cuantos mobs de jefe aparecen.", v -> ClientToolState.genBossCount = v.intValue());
        row += 16;
        screen.addPicker(x, row, width, 14, "Jefe", () -> ClientToolState.genBossEntity,
                RegistryLists.entities(), false, "Entidad del jefe (vanilla o de mod).",
                s -> ClientToolState.genBossEntity = s);
        row += 16;
        screen.addPicker(x, row, width, 14, "Loot tesoro", () -> ClientToolState.genTreasureLoot,
                LOOT_TABLES, false, "Loot table de los cofres de tesoro.", s -> ClientToolState.genTreasureLoot = s);
        row += 16;
        screen.addPicker(x, row, width, 14, "Loot jefe", () -> ClientToolState.genBossLoot,
                LOOT_TABLES, false, "Loot table del cofre del jefe.", s -> ClientToolState.genBossLoot = s);
        row += 16;
        screen.addSlider(x, row, half, 14, "Loops", 0, 100, ClientToolState.genLoopDensity, true,
                "Porcentaje de pasillos extra que crean bucles entre salas.", v -> ClientToolState.genLoopDensity = v.intValue());
        screen.addEditBox(x + half + 4, row, half, 14, String.valueOf(ClientToolState.genSeed),
                "Semilla (0 = aleatoria). Misma semilla + params = misma dungeon.", s -> {
                    try {
                        ClientToolState.genSeed = Long.parseLong(s.trim());
                    } catch (NumberFormatException ignored) {
                        ClientToolState.genSeed = 0L;
                    }
                });
        row += 16;
        screen.addButton(x, row, half, 16, "Validar", () -> PacketHandler.sendToServer(
                new ValidateDungeonSelectionPacket(ClientToolState.genTier)),
                "Comprueba si tu seleccion cumple el tamano minimo del tier.");
        screen.addButton(x + half + 4, row, half, 16, "Generar", IntelligentGenerationPanel::generate,
                "Genera la dungeon completa. Se rechaza si la seleccion no cumple el tamano.");
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

    private static String on(boolean b) {
        return b ? "\u00a7aSi" : "\u00a77No";
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        // El mensaje de validacion se muestra como ayuda; el texto largo se recorta.
        String msg = ClientToolState.genValidationMsg;
        if (msg != null && msg.length() > 38) {
            msg = msg.substring(0, 37) + "\u2026";
        }
        String color = ClientToolState.genValidationOk ? "\u00a7a" : "\u00a7e";
        screen.drawLabel(g, color + msg, x, y + height - 60);
    }
}
