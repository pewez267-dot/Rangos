package com.fantasticterraform.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Listas (cacheadas) de identificadores de bloques, tipos de particula y sonidos
 * registrados, para poblar los menus desplegables del HUD. Incluye contenido vanilla
 * y de cualquier mod instalado (las particulas/sonidos de mods de musica aparecen
 * automaticamente aqui).
 */
public final class RegistryLists {

    private static List<String> blocks;
    private static List<String> particles;
    private static List<String> sounds;
    private static List<String> entities;

    private RegistryLists() {
    }

    public static List<String> blocks() {
        if (blocks == null) {
            blocks = collect(ForgeRegistries.BLOCKS.getKeys());
        }
        return blocks;
    }

    public static List<String> particles() {
        if (particles == null) {
            particles = collect(ForgeRegistries.PARTICLE_TYPES.getKeys());
        }
        return particles;
    }

    public static List<String> sounds() {
        if (sounds == null) {
            sounds = collect(ForgeRegistries.SOUND_EVENTS.getKeys());
        }
        return sounds;
    }

    public static List<String> entities() {
        if (entities == null) {
            entities = collect(ForgeRegistries.ENTITY_TYPES.getKeys());
        }
        return entities;
    }

    /** Limpia la cache (por si cambian los registros entre conexiones a servidores). */
    public static void invalidate() {
        blocks = null;
        particles = null;
        sounds = null;
        entities = null;
    }

    private static List<String> collect(Iterable<ResourceLocation> keys) {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : keys) {
            out.add(id.toString());
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }
}
