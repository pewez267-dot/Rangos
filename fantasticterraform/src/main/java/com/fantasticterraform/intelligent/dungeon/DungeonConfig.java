package com.fantasticterraform.intelligent.dungeon;

import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;

/**
 * Parametros de una generacion de dungeon concreta, armados desde el HUD/packet.
 */
public final class DungeonConfig {

    public DungeonTheme theme;
    public DungeonTier tier;
    public boolean multiLevel;
    public int levels = 1;
    /** Densidad de trampas por sala (0..1). */
    public double trapDensity;
    /** Tipos de trampa habilitados (indices: 0 placa-flecha,1 foso,2 lava,3 spawner,4 descarga). */
    public boolean[] trapTypes = new boolean[] {true, true, true, true, true};

    public boolean bossEnabled = true;
    public String bossEntityId = "minecraft:zombie";
    public int bossCount = 1;

    public String treasureLootTable = "minecraft:chests/simple_dungeon";
    public String bossLootTable = "minecraft:chests/end_city_treasure";
    public String normalLootTable = "minecraft:chests/abandoned_mineshaft";

    public long seed;
    public int loopDensityPercent = 20;
    public int lightSpacing = 6;
}
