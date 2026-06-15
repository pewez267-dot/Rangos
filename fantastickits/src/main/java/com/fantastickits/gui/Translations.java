package com.fantastickits.gui;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Nombres en espanol (con acentos) para encantamientos y atributos vanilla, usados por
 * el editor de NBT para que toda la interfaz este en espanol. Las entradas de mods (no
 * presentes aqui) usan su ruta de registro. Los comandos NO se traducen.
 */
final class Translations {

    private static final Map<String, String> ENCHANTMENTS = new HashMap<>();
    private static final Map<String, String> ATTRIBUTES = new HashMap<>();

    static {
        // --- Encantamientos (1.20.1) ---
        ENCHANTMENTS.put("minecraft:protection", "Protección");
        ENCHANTMENTS.put("minecraft:fire_protection", "Protección contra fuego");
        ENCHANTMENTS.put("minecraft:feather_falling", "Caída de pluma");
        ENCHANTMENTS.put("minecraft:blast_protection", "Protección contra explosiones");
        ENCHANTMENTS.put("minecraft:projectile_protection", "Protección contra proyectiles");
        ENCHANTMENTS.put("minecraft:respiration", "Respiración");
        ENCHANTMENTS.put("minecraft:aqua_affinity", "Afinidad acuática");
        ENCHANTMENTS.put("minecraft:thorns", "Espinas");
        ENCHANTMENTS.put("minecraft:depth_strider", "Agilidad acuática");
        ENCHANTMENTS.put("minecraft:frost_walker", "Paso helado");
        ENCHANTMENTS.put("minecraft:binding_curse", "Maldición de vinculación");
        ENCHANTMENTS.put("minecraft:soul_speed", "Velocidad de almas");
        ENCHANTMENTS.put("minecraft:swift_sneak", "Sigilo veloz");
        ENCHANTMENTS.put("minecraft:sharpness", "Filo");
        ENCHANTMENTS.put("minecraft:smite", "Castigo");
        ENCHANTMENTS.put("minecraft:bane_of_arthropods", "Perdición de los artrópodos");
        ENCHANTMENTS.put("minecraft:knockback", "Empuje");
        ENCHANTMENTS.put("minecraft:fire_aspect", "Aspecto ígneo");
        ENCHANTMENTS.put("minecraft:looting", "Botín");
        ENCHANTMENTS.put("minecraft:sweeping", "Filo de barrido");
        ENCHANTMENTS.put("minecraft:sweeping_edge", "Filo de barrido");
        ENCHANTMENTS.put("minecraft:efficiency", "Eficiencia");
        ENCHANTMENTS.put("minecraft:silk_touch", "Toque de seda");
        ENCHANTMENTS.put("minecraft:unbreaking", "Irrompibilidad");
        ENCHANTMENTS.put("minecraft:fortune", "Fortuna");
        ENCHANTMENTS.put("minecraft:power", "Poder");
        ENCHANTMENTS.put("minecraft:punch", "Retroceso");
        ENCHANTMENTS.put("minecraft:flame", "Fuego");
        ENCHANTMENTS.put("minecraft:infinity", "Infinidad");
        ENCHANTMENTS.put("minecraft:luck_of_the_sea", "Suerte marina");
        ENCHANTMENTS.put("minecraft:lure", "Atracción");
        ENCHANTMENTS.put("minecraft:loyalty", "Lealtad");
        ENCHANTMENTS.put("minecraft:impaling", "Empalamiento");
        ENCHANTMENTS.put("minecraft:riptide", "Propulsión acuática");
        ENCHANTMENTS.put("minecraft:channeling", "Canalización");
        ENCHANTMENTS.put("minecraft:multishot", "Disparo múltiple");
        ENCHANTMENTS.put("minecraft:quick_charge", "Carga rápida");
        ENCHANTMENTS.put("minecraft:piercing", "Perforación");
        ENCHANTMENTS.put("minecraft:mending", "Reparación");
        ENCHANTMENTS.put("minecraft:vanishing_curse", "Maldición de desaparición");

        // --- Atributos (1.20.1) ---
        ATTRIBUTES.put("minecraft:generic.max_health", "Salud máxima");
        ATTRIBUTES.put("minecraft:generic.follow_range", "Rango de seguimiento");
        ATTRIBUTES.put("minecraft:generic.knockback_resistance", "Resistencia al empuje");
        ATTRIBUTES.put("minecraft:generic.movement_speed", "Velocidad de movimiento");
        ATTRIBUTES.put("minecraft:generic.flying_speed", "Velocidad de vuelo");
        ATTRIBUTES.put("minecraft:generic.attack_damage", "Daño de ataque");
        ATTRIBUTES.put("minecraft:generic.attack_knockback", "Empuje de ataque");
        ATTRIBUTES.put("minecraft:generic.attack_speed", "Velocidad de ataque");
        ATTRIBUTES.put("minecraft:generic.armor", "Armadura");
        ATTRIBUTES.put("minecraft:generic.armor_toughness", "Dureza de armadura");
        ATTRIBUTES.put("minecraft:generic.luck", "Suerte");
        ATTRIBUTES.put("minecraft:generic.max_absorption", "Absorción máxima");
        ATTRIBUTES.put("minecraft:generic.jump_strength", "Fuerza de salto");
        ATTRIBUTES.put("minecraft:horse.jump_strength", "Fuerza de salto (caballo)");
    }

    private Translations() {
    }

    static String enchantment(final ResourceLocation rl) {
        if (rl == null) {
            return "";
        }
        final String name = ENCHANTMENTS.get(rl.toString());
        return name != null ? name : prettify(rl.getPath());
    }

    static String attribute(final ResourceLocation rl) {
        if (rl == null) {
            return "";
        }
        final String name = ATTRIBUTES.get(rl.toString());
        if (name != null) {
            return name;
        }
        String path = rl.getPath();
        final int dot = path.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < path.length()) {
            path = path.substring(dot + 1);
        }
        return prettify(path);
    }

    private static String prettify(final String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        final String spaced = raw.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
