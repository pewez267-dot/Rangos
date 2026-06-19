package com.fantasticterraform.intelligent.biome;

import com.fantasticterraform.intelligent.population.PopulationManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Catalogo de biomas. Cada bioma define su paleta de superficie/subsuelo y un perfil de
 * poblamiento (que categorias de flora/decoracion le corresponden). La seleccion
 * automatica se hace por el espacio climatico temperatura x humedad (diagrama de
 * Whittaker), el mismo principio que usa el generador de Minecraft 1.18 (temperatura,
 * humedad, continentalidad, erosion). Contenido reformulado de fuentes para cumplir
 * licencias.
 */
public enum BiomeType {

    OCEAN("Oceano", Blocks.GRAVEL, Blocks.DIRT, 0),
    BEACH("Playa", Blocks.SAND, Blocks.SANDSTONE, PopulationManager.WATER),
    PLAINS("Llanura", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.GRASS | PopulationManager.FLOWERS | PopulationManager.ROCKS),
    MEADOW("Pradera", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.FLOWERS | PopulationManager.GRASS),
    CHERRY_GROVE("Arboleda de cerezos", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.TREES | PopulationManager.FLOWERS | PopulationManager.GRASS),
    FOREST("Bosque", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.TREES | PopulationManager.FLOWERS | PopulationManager.GRASS | PopulationManager.MUSHROOMS | PopulationManager.ROCKS),
    TAIGA("Taiga", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.TREES | PopulationManager.GRASS | PopulationManager.ROCKS | PopulationManager.MUSHROOMS),
    SNOWY_PLAINS("Nevada", Blocks.SNOW_BLOCK, Blocks.DIRT,
            PopulationManager.TREES | PopulationManager.ROCKS),
    SNOWY_PEAKS("Cumbres nevadas", Blocks.SNOW_BLOCK, Blocks.STONE, 0),
    STONY_PEAKS("Cumbres rocosas", Blocks.STONE, Blocks.STONE, PopulationManager.CRYSTALS),
    DESERT("Desierto", Blocks.SAND, Blocks.SANDSTONE, PopulationManager.DESERT),
    BADLANDS("Tierras aridas", Blocks.RED_SAND, Blocks.TERRACOTTA, PopulationManager.DESERT),
    SAVANNA("Sabana", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.TREES | PopulationManager.GRASS | PopulationManager.DESERT),
    JUNGLE("Jungla", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.TREES | PopulationManager.GRASS | PopulationManager.FLOWERS | PopulationManager.MUSHROOMS),
    SWAMP("Pantano", Blocks.GRASS_BLOCK, Blocks.DIRT,
            PopulationManager.WATER | PopulationManager.MUSHROOMS | PopulationManager.GRASS);

    private final String displayName;
    private final net.minecraft.world.level.block.Block surface;
    private final net.minecraft.world.level.block.Block sub;
    private final int populationMask;

    BiomeType(String displayName, net.minecraft.world.level.block.Block surface,
              net.minecraft.world.level.block.Block sub, int populationMask) {
        this.displayName = displayName;
        this.surface = surface;
        this.sub = sub;
        this.populationMask = populationMask;
    }

    public String displayName() {
        return displayName;
    }

    public BlockState surface() {
        return surface.defaultBlockState();
    }

    public BlockState sub() {
        return sub.defaultBlockState();
    }

    public int populationMask() {
        return populationMask;
    }
}
