package com.fantasticterraform.client;

/**
 * Configuracion de herramientas del lado cliente, editada por los paneles del HUD y
 * leida por el manejador de input de la varita. Los cambios relevantes se envian al
 * servidor mediante packets.
 */
public final class ClientToolState {

    /** Que hace el click de la varita: marcar seleccion o aplicar brush. */
    public enum WandMode {
        SELECT, BRUSH
    }

    public static volatile WandMode wandMode = WandMode.SELECT;

    // --- Edicion ---
    public static volatile String primaryBlock = "minecraft:stone";
    public static volatile String replaceFrom = "minecraft:dirt";
    public static volatile String replaceTo = "minecraft:stone";
    public static volatile int shapeRadius = 5;
    public static volatile int shapeHeight = 8;
    public static volatile int shapeSize = 6;
    public static volatile boolean pyramidInverted;
    public static volatile int moveX;
    public static volatile int moveY = 5;
    public static volatile int moveZ;
    public static volatile int pasteRotation;

    // --- Brushes ---
    public static volatile String brushId = "sphere";
    public static volatile int brushRadius = 5;
    public static volatile double brushIntensity = 0.5D;
    public static volatile int brushHeight = 5;
    public static volatile String brushBlock = "minecraft:stone";

    // --- Terreno ---
    public static volatile int smoothKernel = 1;
    public static volatile int smoothPasses = 2;
    public static volatile double smoothIntensity = 0.5D;
    public static volatile int deformCurve;
    public static volatile double deformAmplitude = 8.0D;
    public static volatile int naturalizeLayers = 3;
    public static volatile String surfaceBlock = "minecraft:grass_block";
    public static volatile String dirtBlock = "minecraft:dirt";
    public static volatile String stoneBlock = "minecraft:stone";
    public static volatile double caveThreshold = 0.3D;
    public static volatile double caveScale = 0.06D;
    public static volatile double mountainAmplitude = 24.0D;
    public static volatile double mountainFrequency = 0.05D;
    public static volatile int mountainOctaves = 4;
    public static volatile int erosionPasses = 4;
    public static volatile double erosionTalus = 1.0D;
    public static volatile double erosionFactor = 0.5D;
    public static volatile long seed = 1337L;

    // --- Schematics ---
    public static volatile String schematicName = "mi_estructura";
    public static volatile int schematicFormat; // 0=Sponge,1=Litematica,2=Vanilla

    // --- Particulas ---
    public static volatile String particleType = "minecraft:flame";
    public static volatile double particleRate = 10.0D;
    public static volatile float particleR = 1.0F;
    public static volatile float particleG = 0.5F;
    public static volatile float particleB = 0.1F;
    public static volatile double particleRadius = 32.0D;
    public static volatile long particleDuration = -1L;

    // --- Ambiente ---
    public static volatile String ambienceSound = "minecraft:ambient.cave";
    public static volatile float ambienceVolume = 1.0F;
    public static volatile float ambiencePitch = 1.0F;
    public static volatile boolean ambienceLoop = true;
    public static volatile double ambienceFade = 2.0D;

    // --- Generacion inteligente: biomas ---
    public static volatile double biomeContScale = 0.004D;
    public static volatile double biomeEroScale = 0.010D;
    public static volatile double biomeMoistScale = 0.020D;
    public static volatile double biomeTempScale = 0.020D;

    // --- Generacion inteligente: poblamiento ---
    public static volatile boolean popTrees = true;
    public static volatile boolean popRocks = true;
    public static volatile boolean popVegetation = true;
    public static volatile boolean popCrystals = false;

    // --- Generacion inteligente: dungeon ---
    public static volatile String genTheme = "catacombs";
    public static volatile int genTier;           // 0..3
    public static volatile boolean genMultiLevel;
    public static volatile int genLevels = 2;
    public static volatile int genTrapDensity = 2; // 0 none,1 low,2 med,3 high
    public static volatile boolean[] genTrapTypes = new boolean[] {true, true, true, true, true};
    public static volatile boolean genBoss = true;
    public static volatile String genBossEntity = "minecraft:zombie";
    public static volatile int genBossCount = 1;
    public static volatile String genTreasureLoot = "minecraft:chests/simple_dungeon";
    public static volatile String genBossLoot = "minecraft:chests/end_city_treasure";
    public static volatile String genNormalLoot = "minecraft:chests/abandoned_mineshaft";
    public static volatile long genSeed;          // 0 = aleatoria
    public static volatile int genLoopDensity = 20;
    // tema personalizado
    public static volatile String customWall = "minecraft:stone_bricks";
    public static volatile String customFloor = "minecraft:stone";
    public static volatile String customCeiling = "minecraft:cobblestone";
    public static volatile String customPillar = "minecraft:chiseled_stone_bricks";
    public static volatile String customLight = "minecraft:lantern";
    public static volatile String customAccent = "minecraft:cobweb";
    public static volatile String customMob = "minecraft:zombie";
    // resultado de validacion mostrado en el HUD
    public static volatile boolean genValidationOk;
    public static volatile String genValidationMsg = "Pulsa 'Validar' para comprobar el tamano.";

    private ClientToolState() {
    }
}
