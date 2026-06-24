package com.fantasticterraform.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuracion del mod via Forge Config API. Genera
 * {@code config/fantasticterraform/config.toml}.
 *
 * <p>Todos los limites de rendimiento, seleccion, brushes y terreno descritos
 * en la especificacion viven aqui y se consultan en tiempo de ejecucion.</p>
 */
public final class TerraformConfig {

    public static final ForgeConfigSpec SPEC;
    public static final General GENERAL;

    private TerraformConfig() {
    }

    static {
        Pair<General, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(General::new);
        GENERAL = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static final class General {

        // [general]
        public final ForgeConfigSpec.IntValue maxSelectionVolume;
        public final ForgeConfigSpec.IntValue maxBlocksPerTick;
        public final ForgeConfigSpec.IntValue historyStackSize;
        public final ForgeConfigSpec.IntValue maxUndoBlocksPerOperation;

        // [performance]
        public final ForgeConfigSpec.BooleanValue ambienceCheckOnChunkChangeOnly;
        public final ForgeConfigSpec.BooleanValue asyncSchematicIo;

        // [selection]
        public final ForgeConfigSpec.IntValue maxPolygonVertices;
        public final ForgeConfigSpec.IntValue maxFreehandPoints;

        // [brushes]
        public final ForgeConfigSpec.IntValue maxBrushRadius;

        // [terrain]
        public final ForgeConfigSpec.IntValue maxSmoothPasses;
        public final ForgeConfigSpec.IntValue maxErosionPasses;

        // [particles]
        public final ForgeConfigSpec.IntValue defaultEmissionRate;
        public final ForgeConfigSpec.IntValue maxActiveEmitters;

        // [ambience]
        public final ForgeConfigSpec.DoubleValue defaultFadeSeconds;

        // [intelligent_generation]
        public final ForgeConfigSpec.LongValue intelligentSeed;
        public final ForgeConfigSpec.DoubleValue populationDensityScale;
        public final ForgeConfigSpec.IntValue populationMinDistance;
        public final ForgeConfigSpec.LongValue dungeonSeed;
        public final ForgeConfigSpec.IntValue dungeonMaxLevels;
        public final ForgeConfigSpec.IntValue dungeonMaxPackingAttempts;
        public final ForgeConfigSpec.DoubleValue trapDensityLow;
        public final ForgeConfigSpec.DoubleValue trapDensityMedium;
        public final ForgeConfigSpec.DoubleValue trapDensityHigh;
        public final ForgeConfigSpec.IntValue[] tierMinRooms;
        public final ForgeConfigSpec.IntValue[] tierMaxRooms;
        public final ForgeConfigSpec.IntValue[] tierMinVolume;
        public final ForgeConfigSpec.IntValue[] tierMinWidth;
        public final ForgeConfigSpec.IntValue[] tierMinHeight;
        public final ForgeConfigSpec.IntValue[] tierMinLength;

        General(ForgeConfigSpec.Builder builder) {
            builder.comment("Limites generales de seguridad y rendimiento.").push("general");
            maxSelectionVolume = builder
                    .comment("Volumen maximo (en bloques) que una seleccion puede abarcar antes de rechazar operaciones.")
                    .defineInRange("max_selection_volume", 10_000_000, 1, Integer.MAX_VALUE);
            maxBlocksPerTick = builder
                    .comment("Maximo de bloques procesados por tick del servidor en cualquier operacion masiva. Innegociable.")
                    .defineInRange("max_blocks_per_tick", 5_000, 1, 1_000_000);
            historyStackSize = builder
                    .comment("Numero de operaciones de edicion guardadas por jugador para deshacer/rehacer.")
                    .defineInRange("history_stack_size", 50, 1, 10_000);
            maxUndoBlocksPerOperation = builder
                    .comment("Maximo de cambios de bloque que una sola operacion puede guardar en el historial.")
                    .defineInRange("max_undo_blocks_per_operation", 1_000_000, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.comment("Ajustes de rendimiento transversales.").push("performance");
            ambienceCheckOnChunkChangeOnly = builder
                    .comment("Si es true, las zonas de ambiente solo se evaluan al cambiar de chunk, no cada tick.")
                    .define("ambience_check_on_chunk_change_only", true);
            asyncSchematicIo = builder
                    .comment("Si es true, la lectura/escritura de schematics se hace en un ExecutorService dedicado.")
                    .define("async_schematic_io", true);
            builder.pop();

            builder.comment("Limites del sistema de seleccion.").push("selection");
            maxPolygonVertices = builder
                    .comment("Maximo de vertices que un poligono puede tener.")
                    .defineInRange("max_polygon_vertices", 200, 3, 100_000);
            maxFreehandPoints = builder
                    .comment("Maximo de puntos que una seleccion freehand/convex-hull puede tener.")
                    .defineInRange("max_freehand_points", 500, 4, 100_000);
            builder.pop();

            builder.comment("Limites de los brushes.").push("brushes");
            maxBrushRadius = builder
                    .comment("Radio maximo permitido para cualquier brush.")
                    .defineInRange("max_brush_radius", 50, 1, 256);
            builder.pop();

            builder.comment("Limites de las operaciones de terreno.").push("terrain");
            maxSmoothPasses = builder
                    .comment("Numero maximo de pasadas de suavizado en una sola operacion.")
                    .defineInRange("max_smooth_passes", 10, 1, 100);
            maxErosionPasses = builder
                    .comment("Numero maximo de pasadas de erosion en una sola operacion.")
                    .defineInRange("max_erosion_passes", 10, 1, 100);
            builder.pop();

            builder.comment("Sistema de particulas.").push("particles");
            defaultEmissionRate = builder
                    .comment("Tasa de emision por defecto (particulas por segundo) de un emisor nuevo.")
                    .defineInRange("default_emission_rate", 10, 1, 10_000);
            maxActiveEmitters = builder
                    .comment("Maximo de emisores activos por dimension.")
                    .defineInRange("max_active_emitters", 200, 1, 100_000);
            builder.pop();

            builder.comment("Sistema de ambiente sonoro.").push("ambience");
            defaultFadeSeconds = builder
                    .comment("Duracion por defecto del fade in/out al entrar/salir de una zona de ambiente.")
                    .defineInRange("default_fade_seconds", 2.0D, 0.0D, 60.0D);
            builder.pop();

            builder.comment("Generacion inteligente: biomas por ruido, poblamiento y dungeons.").push("intelligent_generation");
            intelligentSeed = builder
                    .comment("Semilla global de generacion inteligente. -1 = aleatoria cada vez.")
                    .defineInRange("seed", -1L, Long.MIN_VALUE, Long.MAX_VALUE);

            builder.push("population");
            populationDensityScale = builder
                    .comment("Escala del ruido de densidad del poblamiento (mas pequeno = manchas mas grandes).")
                    .defineInRange("density_noise_scale", 0.05D, 0.001D, 1.0D);
            populationMinDistance = builder
                    .comment("Distancia minima entre instancias del mismo tipo para evitar amontonamiento.")
                    .defineInRange("min_distance_between_same_type", 3, 0, 64);
            builder.pop();

            builder.push("dungeon");
            dungeonSeed = builder
                    .comment("Semilla de dungeons. -1 = aleatoria cada vez.")
                    .defineInRange("seed", -1L, Long.MIN_VALUE, Long.MAX_VALUE);
            dungeonMaxLevels = builder
                    .comment("Maximo de niveles verticales en dungeons multi-nivel.")
                    .defineInRange("max_levels", 5, 1, 16);
            dungeonMaxPackingAttempts = builder
                    .comment("Intentos maximos de colocar habitaciones sin solaparse antes de reducir el numero.")
                    .defineInRange("max_packing_attempts", 500, 10, 10000);

            builder.push("traps");
            trapDensityLow = builder.defineInRange("density_low", 0.10D, 0.0D, 1.0D);
            trapDensityMedium = builder.defineInRange("density_medium", 0.25D, 0.0D, 1.0D);
            trapDensityHigh = builder.defineInRange("density_high", 0.45D, 0.0D, 1.0D);
            builder.pop();

            String[] tierKeys = {"pequena", "mediana", "grande", "epica"};
            int[] defMinRooms = {4, 8, 15, 25};
            int[] defMaxRooms = {8, 15, 25, 40};
            int[] defMinVolume = {30000, 100000, 300000, 800000};
            int[] defMinW = {30, 50, 80, 120};
            int[] defMinH = {20, 25, 30, 40};
            int[] defMinL = {30, 50, 80, 120};
            tierMinRooms = new ForgeConfigSpec.IntValue[4];
            tierMaxRooms = new ForgeConfigSpec.IntValue[4];
            tierMinVolume = new ForgeConfigSpec.IntValue[4];
            tierMinWidth = new ForgeConfigSpec.IntValue[4];
            tierMinHeight = new ForgeConfigSpec.IntValue[4];
            tierMinLength = new ForgeConfigSpec.IntValue[4];
            builder.push("tiers");
            for (int i = 0; i < 4; i++) {
                builder.push(tierKeys[i]);
                tierMinRooms[i] = builder.defineInRange("min_rooms", defMinRooms[i], 1, 1000);
                tierMaxRooms[i] = builder.defineInRange("max_rooms", defMaxRooms[i], 1, 1000);
                tierMinVolume[i] = builder.defineInRange("min_volume", defMinVolume[i], 1, Integer.MAX_VALUE);
                tierMinWidth[i] = builder.defineInRange("min_width", defMinW[i], 1, 10000);
                tierMinHeight[i] = builder.defineInRange("min_height", defMinH[i], 1, 1000);
                tierMinLength[i] = builder.defineInRange("min_length", defMinL[i], 1, 10000);
                builder.pop();
            }
            builder.pop();
            builder.pop();
        }
    }
}
