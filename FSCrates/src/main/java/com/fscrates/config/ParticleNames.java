package com.fscrates.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Friendly Spanish names for vanilla particle ids. Keyed by the particle path
 * (without the {@code minecraft:} namespace). Returns the original key if no
 * translation exists so modded particles still display sensibly.
 */
public final class ParticleNames {

    private ParticleNames() {}

    private static final Map<String, String> ES = new HashMap<>();

    static {
        // Visual / decorative
        put("dust", "Polvo de color");
        put("dust_color_transition", "Polvo bicolor");
        put("enchant", "Encantamiento");
        put("enchanted_hit", "Golpe encantado");
        put("end_rod", "Vara del End");
        put("firework", "Fuego artificial");
        put("flame", "Llama");
        put("soul_fire_flame", "Llama de alma");
        put("soul", "Alma");
        put("smoke", "Humo");
        put("large_smoke", "Humo grande");
        put("campfire_cosy_smoke", "Humo de fogata");
        put("campfire_signal_smoke", "Humo de fogata grande");
        put("ash", "Ceniza");
        put("white_ash", "Ceniza blanca");
        put("crimson_spore", "Espora carmes\u00ed");
        put("warped_spore", "Espora distorsionada");
        put("cherry_leaves", "P\u00e9talos de cerezo");
        put("spore_blossom_air", "Esporas de flor");
        put("falling_dust", "Polvo cayendo");
        put("composter", "Compostador");
        put("dragon_breath", "Aliento de drag\u00f3n");
        put("dolphin", "Estela de delf\u00edn");
        put("totem_of_undying", "T\u00f3tem de la inmortalidad");
        put("witch", "Bruja");
        put("happy_villager", "Aldeano feliz");
        put("angry_villager", "Aldeano enfadado");
        put("heart", "Coraz\u00f3n");
        put("note", "Nota musical");
        put("portal", "Portal");
        put("reverse_portal", "Portal inverso");
        put("nautilus", "Nautilus");
        put("crit", "Cr\u00edtico");
        put("electric_spark", "Chispa el\u00e9ctrica");
        put("glow", "Brillo");
        put("glow_squid_ink", "Tinta luminosa");
        put("squid_ink", "Tinta de calamar");
        put("scrape", "Raspado");
        put("wax_on", "Aplicar cera");
        put("wax_off", "Retirar cera");
        put("sneeze", "Estornudo");
        put("sculk_charge", "Carga de sculk");
        put("sculk_charge_pop", "Pop de sculk");
        put("sculk_soul", "Alma de sculk");
        put("vibration", "Vibraci\u00f3n");
        put("shriek", "Chillido");
        put("egg_crack", "Cascar\u00f3n roto");
        put("trial_spawner_detection", "Detecci\u00f3n de bestia");
        put("ambient_entity_effect", "Efecto de entidad ambiental");
        put("elder_guardian", "Guardi\u00e1n anciano");
        put("falling_nectar", "N\u00e9ctar cayendo");
        put("falling_spore_blossom", "Esporas de flor cayendo");
        put("flash", "Destello");
        put("lava", "Lava");
        put("mycelium", "Micelio");
        put("sonic_boom", "Estruendo s\u00f3nico");
        put("spit", "Escupitajo");
        put("sweep_attack", "Barrido de espada");
        put("snowflake", "Copo de nieve");
        put("small_flame", "Llama peque\u00f1a");
        put("soul_fire_flame", "Llama de alma");
        put("nautilus", "N\u00e1utilus");
        put("crimson_spore", "Espora carmes\u00ed");
        put("warped_spore", "Espora distorsionada");

        // Liquids / drops
        put("dripping_water", "Goteo de agua");
        put("falling_water", "Agua cayendo");
        put("dripping_lava", "Goteo de lava");
        put("falling_lava", "Lava cayendo");
        put("landing_lava", "Impacto de lava");
        put("dripping_honey", "Goteo de miel");
        put("falling_honey", "Miel cayendo");
        put("landing_honey", "Impacto de miel");
        put("dripping_obsidian_tear", "L\u00e1grima de obsidiana");
        put("falling_obsidian_tear", "L\u00e1grima cayendo");
        put("landing_obsidian_tear", "Impacto de l\u00e1grima");
        put("dripping_dripstone_lava", "Lava de estalactita");
        put("dripping_dripstone_water", "Agua de estalactita");
        put("falling_dripstone_lava", "Estalactita - lava");
        put("falling_dripstone_water", "Estalactita - agua");

        // Gameplay
        put("bubble", "Burbuja");
        put("bubble_column_up", "Columna de burbujas");
        put("bubble_pop", "Burbuja explotando");
        put("splash", "Salpicadura");
        put("rain", "Lluvia");
        put("underwater", "Bajo el agua");
        put("current_down", "Corriente descendente");
        put("fishing", "Pesca");
        put("explosion", "Explosi\u00f3n");
        put("explosion_emitter", "Emisor de explosi\u00f3n");
        put("poof", "Bocanada");
        put("cloud", "Nube");
        put("effect", "Efecto");
        put("entity_effect", "Efecto de entidad");
        put("instant_effect", "Efecto instant\u00e1neo");
        put("damage_indicator", "Indicador de da\u00f1o");
        put("item", "Item");
        put("item_slime", "Slime");
        put("item_snowball", "Bola de nieve");
        put("block", "Bloque");
        put("block_marker", "Marcador de bloque");
        put("falling_dust_minecraft", "Polvo cayendo");
        put("end_rod_minecraft", "Vara del End");
    }

    private static void put(String id, String es) {
        ES.put(id, es);
    }

    /**
     * Returns the Spanish display name for a particle path (e.g. "dust" → "Polvo de color").
     * Falls back to a prettied version of the id (snake_case → "Snake Case") when unknown.
     */
    public static String spanish(String path) {
        if (path == null) {
            return "(?)";
        }
        String name = ES.get(path);
        if (name != null) {
            return name;
        }
        // pretty-print unknown ids
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == '_') { sb.append(' '); cap = true; continue; }
            sb.append(cap ? Character.toUpperCase(c) : c);
            cap = false;
        }
        return sb.toString();
    }
}
