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
        }
    }
}
