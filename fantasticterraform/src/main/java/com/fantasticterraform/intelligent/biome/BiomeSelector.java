package com.fantasticterraform.intelligent.biome;

/**
 * Selecciona el bioma por el espacio climatico temperatura x humedad (diagrama de
 * Whittaker): el frio/calor en un eje y la aridez/humedad en el otro. Es el mismo
 * principio que usa la generacion moderna de Minecraft para decidir el bioma de
 * superficie. La altura (continentalidad/erosion) la aporta el generador de terreno.
 */
public final class BiomeSelector {

    // Matriz [temperatura][humedad] -> bioma. Filas: frio..calido. Columnas: arido..humedo.
    private static final BiomeType[][] MATRIX = {
            // muy frio
            {BiomeType.SNOWY_PLAINS, BiomeType.SNOWY_PLAINS, BiomeType.TAIGA, BiomeType.TAIGA},
            // templado-frio
            {BiomeType.PLAINS, BiomeType.MEADOW, BiomeType.FOREST, BiomeType.SWAMP},
            // templado-calido
            {BiomeType.SAVANNA, BiomeType.CHERRY_GROVE, BiomeType.FOREST, BiomeType.JUNGLE},
            // calido
            {BiomeType.BADLANDS, BiomeType.DESERT, BiomeType.SAVANNA, BiomeType.JUNGLE}
    };

    private BiomeSelector() {
    }

    /**
     * @param temperature 0..1 (0 = muy frio, 1 = muy calido)
     * @param humidity    0..1 (0 = arido, 1 = muy humedo)
     */
    public static BiomeType pick(double temperature, double humidity) {
        int t = bucket(temperature);
        int h = bucket(humidity);
        return MATRIX[t][h];
    }

    private static int bucket(double v) {
        if (v < 0.25D) {
            return 0;
        }
        if (v < 0.5D) {
            return 1;
        }
        if (v < 0.75D) {
            return 2;
        }
        return 3;
    }
}
