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

    private ClientToolState() {
    }
}
