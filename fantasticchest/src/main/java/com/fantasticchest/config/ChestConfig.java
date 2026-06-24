package com.fantasticchest.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * General configuration (Forge Config API), written to
 * {@code config/fantasticchest/config.toml}. Values are mirrored into baked fields so the
 * rest of the mod reads them cheaply and safely from any thread/context.
 */
public final class ChestConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.LongValue DEFAULT_QUANTITY;
    private static final ForgeConfigSpec.BooleanValue HIDE_EMPTY_ITEMS;
    private static final ForgeConfigSpec.BooleanValue REQUIRE_PICKUP_BEFORE_DELETE;
    private static final ForgeConfigSpec.DoubleValue MAX_INTERACTION_DISTANCE;
    private static final ForgeConfigSpec.LongValue COMPACT_THRESHOLD;
    private static final ForgeConfigSpec.ConfigValue<String> COMPACT_FORMAT;
    private static final ForgeConfigSpec.IntValue PAGE_SIZE;
    private static final ForgeConfigSpec.BooleanValue ASYNC_SAVE;

    private static volatile long defaultQuantity = 1000L;
    private static volatile boolean hideEmptyItems = true;
    private static volatile boolean requirePickupBeforeDelete = false;
    private static volatile double maxInteractionDistance = 8.0;
    private static volatile long compactThreshold = 1_000_000L;
    private static volatile String compactFormat = "M";
    private static volatile int pageSize = 45;
    private static volatile boolean asyncSave = true;

    static {
        final ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Ajustes generales").push("general");
        DEFAULT_QUANTITY = b.comment("Cantidad por defecto usada por 'Anadir todos' cuando el campo esta vacio.")
                .defineInRange("default_quantity", 1000L, 1L, Long.MAX_VALUE);
        HIDE_EMPTY_ITEMS = b.comment("Ocultar filas de items con cantidad 0 en la terminal.")
                .define("hide_empty_items", true);
        REQUIRE_PICKUP_BEFORE_DELETE = b.comment("Requerir vaciar el cofre antes de poder eliminarlo.")
                .define("require_pickup_before_delete", false);
        MAX_INTERACTION_DISTANCE = b.comment("Distancia maxima (bloques) para interactuar con un cofre.")
                .defineInRange("max_interaction_distance", 8.0, 1.0, 64.0);
        b.pop();

        b.comment("Formato de cantidades").push("display");
        COMPACT_THRESHOLD = b.comment("A partir de esta cantidad se usa formato compacto (1.0M, 2.5B).")
                .defineInRange("compact_threshold", 1_000_000L, 1L, Long.MAX_VALUE);
        COMPACT_FORMAT = b.comment("Sufijo base del formato compacto (informativo).")
                .define("compact_format", "M");
        b.pop();

        b.comment("Rendimiento").push("performance");
        PAGE_SIZE = b.comment("Numero de items enviados por pagina a la terminal.")
                .defineInRange("page_size", 45, 1, 1000);
        ASYNC_SAVE = b.comment("Guardar chests.json de forma asincrona en un hilo dedicado.")
                .define("async_save", true);
        b.pop();

        SPEC = b.build();
    }

    private ChestConfig() {
    }

    public static void bake() {
        defaultQuantity = DEFAULT_QUANTITY.get();
        hideEmptyItems = HIDE_EMPTY_ITEMS.get();
        requirePickupBeforeDelete = REQUIRE_PICKUP_BEFORE_DELETE.get();
        maxInteractionDistance = MAX_INTERACTION_DISTANCE.get();
        compactThreshold = COMPACT_THRESHOLD.get();
        compactFormat = COMPACT_FORMAT.get();
        pageSize = PAGE_SIZE.get();
        asyncSave = ASYNC_SAVE.get();
    }

    public static long defaultQuantity() {
        return defaultQuantity;
    }

    public static boolean hideEmptyItems() {
        return hideEmptyItems;
    }

    public static boolean requirePickupBeforeDelete() {
        return requirePickupBeforeDelete;
    }

    public static double maxInteractionDistance() {
        return maxInteractionDistance;
    }

    public static long compactThreshold() {
        return compactThreshold;
    }

    public static String compactFormat() {
        return compactFormat;
    }

    public static int pageSize() {
        return pageSize;
    }

    public static boolean asyncSave() {
        return asyncSave;
    }
}
