package com.fscrates.config;

import java.util.HashMap;
import java.util.Map;

public final class ParticleNames {
    private static final Map<String, String> ES = new HashMap<String, String>();

    private ParticleNames() {
    }

    private static void put(String id, String es) {
        ES.put(id, es);
    }

    public static String spanish(String path) {
        if (path == null) {
            return "(?)";
        }
        String name = ES.get(path);
        if (name != null) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                cap = true;
                continue;
            }
            sb.append(cap ? Character.toUpperCase(c) : c);
            cap = false;
        }
        return sb.toString();
    }

    static {
        ParticleNames.put("dust", "Polvo de color");
        ParticleNames.put("dust_color_transition", "Polvo bicolor");
        ParticleNames.put("enchant", "Encantamiento");
        ParticleNames.put("enchanted_hit", "Golpe encantado");
        ParticleNames.put("end_rod", "Vara del End");
        ParticleNames.put("firework", "Fuego artificial");
        ParticleNames.put("flame", "Llama");
        ParticleNames.put("soul_fire_flame", "Llama de alma");
        ParticleNames.put("soul", "Alma");
        ParticleNames.put("smoke", "Humo");
        ParticleNames.put("large_smoke", "Humo grande");
        ParticleNames.put("campfire_cosy_smoke", "Humo de fogata");
        ParticleNames.put("campfire_signal_smoke", "Humo de fogata grande");
        ParticleNames.put("ash", "Ceniza");
        ParticleNames.put("white_ash", "Ceniza blanca");
        ParticleNames.put("crimson_spore", "Espora carmes\u00ed");
        ParticleNames.put("warped_spore", "Espora distorsionada");
        ParticleNames.put("cherry_leaves", "P\u00e9talos de cerezo");
        ParticleNames.put("spore_blossom_air", "Esporas de flor");
        ParticleNames.put("falling_dust", "Polvo cayendo");
        ParticleNames.put("composter", "Compostador");
        ParticleNames.put("dragon_breath", "Aliento de drag\u00f3n");
        ParticleNames.put("dolphin", "Estela de delf\u00edn");
        ParticleNames.put("totem_of_undying", "T\u00f3tem de la inmortalidad");
        ParticleNames.put("witch", "Bruja");
        ParticleNames.put("happy_villager", "Aldeano feliz");
        ParticleNames.put("angry_villager", "Aldeano enfadado");
        ParticleNames.put("heart", "Coraz\u00f3n");
        ParticleNames.put("note", "Nota musical");
        ParticleNames.put("portal", "Portal");
        ParticleNames.put("reverse_portal", "Portal inverso");
        ParticleNames.put("nautilus", "Nautilus");
        ParticleNames.put("crit", "Cr\u00edtico");
        ParticleNames.put("electric_spark", "Chispa el\u00e9ctrica");
        ParticleNames.put("glow", "Brillo");
        ParticleNames.put("glow_squid_ink", "Tinta luminosa");
        ParticleNames.put("squid_ink", "Tinta de calamar");
        ParticleNames.put("scrape", "Raspado");
        ParticleNames.put("wax_on", "Aplicar cera");
        ParticleNames.put("wax_off", "Retirar cera");
        ParticleNames.put("sneeze", "Estornudo");
        ParticleNames.put("sculk_charge", "Carga de sculk");
        ParticleNames.put("sculk_charge_pop", "Pop de sculk");
        ParticleNames.put("sculk_soul", "Alma de sculk");
        ParticleNames.put("vibration", "Vibraci\u00f3n");
        ParticleNames.put("shriek", "Chillido");
        ParticleNames.put("egg_crack", "Cascar\u00f3n roto");
        ParticleNames.put("trial_spawner_detection", "Detecci\u00f3n de bestia");
        ParticleNames.put("ambient_entity_effect", "Efecto de entidad ambiental");
        ParticleNames.put("elder_guardian", "Guardi\u00e1n anciano");
        ParticleNames.put("falling_nectar", "N\u00e9ctar cayendo");
        ParticleNames.put("falling_spore_blossom", "Esporas de flor cayendo");
        ParticleNames.put("flash", "Destello");
        ParticleNames.put("lava", "Lava");
        ParticleNames.put("mycelium", "Micelio");
        ParticleNames.put("sonic_boom", "Estruendo s\u00f3nico");
        ParticleNames.put("spit", "Escupitajo");
        ParticleNames.put("sweep_attack", "Barrido de espada");
        ParticleNames.put("snowflake", "Copo de nieve");
        ParticleNames.put("small_flame", "Llama peque\u00f1a");
        ParticleNames.put("soul_fire_flame", "Llama de alma");
        ParticleNames.put("nautilus", "N\u00e1utilus");
        ParticleNames.put("crimson_spore", "Espora carmes\u00ed");
        ParticleNames.put("warped_spore", "Espora distorsionada");
        ParticleNames.put("dripping_water", "Goteo de agua");
        ParticleNames.put("falling_water", "Agua cayendo");
        ParticleNames.put("dripping_lava", "Goteo de lava");
        ParticleNames.put("falling_lava", "Lava cayendo");
        ParticleNames.put("landing_lava", "Impacto de lava");
        ParticleNames.put("dripping_honey", "Goteo de miel");
        ParticleNames.put("falling_honey", "Miel cayendo");
        ParticleNames.put("landing_honey", "Impacto de miel");
        ParticleNames.put("dripping_obsidian_tear", "L\u00e1grima de obsidiana");
        ParticleNames.put("falling_obsidian_tear", "L\u00e1grima cayendo");
        ParticleNames.put("landing_obsidian_tear", "Impacto de l\u00e1grima");
        ParticleNames.put("dripping_dripstone_lava", "Lava de estalactita");
        ParticleNames.put("dripping_dripstone_water", "Agua de estalactita");
        ParticleNames.put("falling_dripstone_lava", "Estalactita - lava");
        ParticleNames.put("falling_dripstone_water", "Estalactita - agua");
        ParticleNames.put("bubble", "Burbuja");
        ParticleNames.put("bubble_column_up", "Columna de burbujas");
        ParticleNames.put("bubble_pop", "Burbuja explotando");
        ParticleNames.put("splash", "Salpicadura");
        ParticleNames.put("rain", "Lluvia");
        ParticleNames.put("underwater", "Bajo el agua");
        ParticleNames.put("current_down", "Corriente descendente");
        ParticleNames.put("fishing", "Pesca");
        ParticleNames.put("explosion", "Explosi\u00f3n");
        ParticleNames.put("explosion_emitter", "Emisor de explosi\u00f3n");
        ParticleNames.put("poof", "Bocanada");
        ParticleNames.put("cloud", "Nube");
        ParticleNames.put("effect", "Efecto");
        ParticleNames.put("entity_effect", "Efecto de entidad");
        ParticleNames.put("instant_effect", "Efecto instant\u00e1neo");
        ParticleNames.put("damage_indicator", "Indicador de da\u00f1o");
        ParticleNames.put("item", "Item");
        ParticleNames.put("item_slime", "Slime");
        ParticleNames.put("item_snowball", "Bola de nieve");
        ParticleNames.put("block", "Bloque");
        ParticleNames.put("block_marker", "Marcador de bloque");
        ParticleNames.put("falling_dust_minecraft", "Polvo cayendo");
        ParticleNames.put("end_rod_minecraft", "Vara del End");
    }
}

