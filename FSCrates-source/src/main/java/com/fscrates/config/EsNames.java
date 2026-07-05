package com.fscrates.config;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class EsNames {
    private static final Map<String, String> ENCH = new HashMap<String, String>();
    private static final Map<String, String> EFFECT = new HashMap<String, String>();
    private static final Map<String, String> ATTR = new HashMap<String, String>();

    private EsNames() {
    }

    private static void e(String id, String es) {
        ENCH.put("minecraft:" + id, es);
    }

    private static void f(String id, String es) {
        EFFECT.put("minecraft:" + id, es);
    }

    private static void a(String id, String es) {
        ATTR.put("minecraft:" + id, es);
    }

    public static String enchant(ResourceLocation rl) {
        if (rl == null) {
            return "(encantamiento)";
        }
        String s = ENCH.get(rl.toString());
        return s != null ? s : EsNames.prettify(rl.getPath());
    }

    public static String effect(ResourceLocation rl) {
        if (rl == null) {
            return "(efecto)";
        }
        String s = EFFECT.get(rl.toString());
        return s != null ? s : EsNames.prettify(rl.getPath());
    }

    public static String attribute(ResourceLocation rl) {
        if (rl == null) {
            return "(atributo)";
        }
        String s = ATTR.get(rl.toString());
        return s != null ? s : EsNames.prettify(rl.getPath().replace('.', '_'));
    }

    public static String attributeByRawId(String rawId) {
        if (rawId == null || rawId.isEmpty()) {
            return "(atributo)";
        }
        String s = ATTR.get(rawId.contains(":") ? rawId : "minecraft:" + rawId);
        return s != null ? s : EsNames.prettify(rawId.substring(rawId.indexOf(58) + 1).replace('.', '_'));
    }

    public static String prettify(String path) {
        if (path == null || path.isEmpty()) {
            return "(?)";
        }
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == '_' || c == '.') {
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
        EsNames.e("protection", "Protecci\u00f3n");
        EsNames.e("fire_protection", "Protecci\u00f3n contra el fuego");
        EsNames.e("feather_falling", "Ca\u00edda de pluma");
        EsNames.e("blast_protection", "Protecci\u00f3n contra explosiones");
        EsNames.e("projectile_protection", "Protecci\u00f3n contra proyectiles");
        EsNames.e("respiration", "Respiraci\u00f3n");
        EsNames.e("aqua_affinity", "Afinidad acu\u00e1tica");
        EsNames.e("thorns", "Espinas");
        EsNames.e("depth_strider", "Agilidad acu\u00e1tica");
        EsNames.e("frost_walker", "Paso helado");
        EsNames.e("binding_curse", "Maldici\u00f3n de vinculaci\u00f3n");
        EsNames.e("soul_speed", "Velocidad del alma");
        EsNames.e("swift_sneak", "Sigilo veloz");
        EsNames.e("sharpness", "Filo");
        EsNames.e("smite", "Golpeo");
        EsNames.e("bane_of_arthropods", "Perdici\u00f3n de los artr\u00f3podos");
        EsNames.e("knockback", "Empuje");
        EsNames.e("fire_aspect", "Aspecto \u00edgneo");
        EsNames.e("looting", "Bot\u00edn");
        EsNames.e("sweeping", "Filo de barrido");
        EsNames.e("sweeping_edge", "Filo de barrido");
        EsNames.e("efficiency", "Eficiencia");
        EsNames.e("silk_touch", "Toque de seda");
        EsNames.e("unbreaking", "Irrompibilidad");
        EsNames.e("fortune", "Fortuna");
        EsNames.e("power", "Poder");
        EsNames.e("punch", "Retroceso");
        EsNames.e("flame", "Fuego");
        EsNames.e("infinity", "Infinidad");
        EsNames.e("luck_of_the_sea", "Suerte marina");
        EsNames.e("lure", "Atracci\u00f3n");
        EsNames.e("loyalty", "Lealtad");
        EsNames.e("impaling", "Empalamiento");
        EsNames.e("riptide", "Propulsi\u00f3n acu\u00e1tica");
        EsNames.e("channeling", "Canalizaci\u00f3n");
        EsNames.e("multishot", "Disparo m\u00faltiple");
        EsNames.e("quick_charge", "Carga r\u00e1pida");
        EsNames.e("piercing", "Perforaci\u00f3n");
        EsNames.e("mending", "Reparaci\u00f3n");
        EsNames.e("vanishing_curse", "Maldici\u00f3n de desaparici\u00f3n");
        EsNames.f("speed", "Velocidad");
        EsNames.f("slowness", "Lentitud");
        EsNames.f("haste", "Prisa");
        EsNames.f("mining_fatigue", "Fatiga minera");
        EsNames.f("strength", "Fuerza");
        EsNames.f("instant_health", "Curaci\u00f3n instant\u00e1nea");
        EsNames.f("instant_damage", "Da\u00f1o instant\u00e1neo");
        EsNames.f("jump_boost", "Salto");
        EsNames.f("nausea", "N\u00e1useas");
        EsNames.f("regeneration", "Regeneraci\u00f3n");
        EsNames.f("resistance", "Resistencia");
        EsNames.f("fire_resistance", "Resistencia al fuego");
        EsNames.f("water_breathing", "Respiraci\u00f3n acu\u00e1tica");
        EsNames.f("invisibility", "Invisibilidad");
        EsNames.f("blindness", "Ceguera");
        EsNames.f("night_vision", "Visi\u00f3n nocturna");
        EsNames.f("hunger", "Hambre");
        EsNames.f("weakness", "Debilidad");
        EsNames.f("poison", "Veneno");
        EsNames.f("wither", "Marchitamiento");
        EsNames.f("health_boost", "Impulso de salud");
        EsNames.f("absorption", "Absorci\u00f3n");
        EsNames.f("saturation", "Saturaci\u00f3n");
        EsNames.f("glowing", "Brillo");
        EsNames.f("levitation", "Levitaci\u00f3n");
        EsNames.f("luck", "Suerte");
        EsNames.f("unluck", "Mala suerte");
        EsNames.f("slow_falling", "Ca\u00edda lenta");
        EsNames.f("conduit_power", "Poder del conducto");
        EsNames.f("dolphins_grace", "Gracia del delf\u00edn");
        EsNames.f("bad_omen", "Mal presagio");
        EsNames.f("hero_of_the_village", "H\u00e9roe de la aldea");
        EsNames.f("darkness", "Oscuridad");
        EsNames.a("generic.max_health", "Vida m\u00e1xima");
        EsNames.a("generic.follow_range", "Rango de seguimiento");
        EsNames.a("generic.knockback_resistance", "Resistencia al empuje");
        EsNames.a("generic.movement_speed", "Velocidad de movimiento");
        EsNames.a("generic.flying_speed", "Velocidad de vuelo");
        EsNames.a("generic.attack_damage", "Da\u00f1o de ataque");
        EsNames.a("generic.attack_knockback", "Empuje de ataque");
        EsNames.a("generic.attack_speed", "Velocidad de ataque");
        EsNames.a("generic.armor", "Armadura");
        EsNames.a("generic.armor_toughness", "Dureza de armadura");
        EsNames.a("generic.luck", "Suerte");
        EsNames.a("horse.jump_strength", "Fuerza de salto (caballo)");
        EsNames.a("zombie.spawn_reinforcements", "Refuerzos de zombi");
        EsNames.a("generic.scale", "Escala (tama\u00f1o)");
        EsNames.a("generic.step_height", "Altura de paso");
        EsNames.a("generic.gravity", "Gravedad");
        EsNames.a("generic.safe_fall_distance", "Distancia de ca\u00edda segura");
        EsNames.a("generic.fall_damage_multiplier", "Multiplicador de da\u00f1o de ca\u00edda");
        EsNames.a("generic.jump_strength", "Fuerza de salto");
        EsNames.a("generic.oxygen_bonus", "Bono de ox\u00edgeno");
        EsNames.a("generic.burning_time", "Tiempo ardiendo");
        EsNames.a("generic.explosion_knockback_resistance", "Resistencia a empuje de explosi\u00f3n");
        EsNames.a("generic.water_movement_efficiency", "Eficiencia de movimiento en agua");
        EsNames.a("generic.movement_efficiency", "Eficiencia de movimiento");
        EsNames.a("generic.attack_damage", "Da\u00f1o de ataque");
        EsNames.a("player.entity_interaction_range", "Alcance de interacci\u00f3n con entidades");
        EsNames.a("player.block_interaction_range", "Alcance de interacci\u00f3n con bloques");
        EsNames.a("player.block_break_speed", "Velocidad de romper bloques");
        EsNames.a("player.mining_efficiency", "Eficiencia de minado");
        EsNames.a("player.sneaking_speed", "Velocidad al agacharse");
        EsNames.a("player.submerged_mining_speed", "Velocidad de minado sumergido");
        EsNames.a("player.sweeping_damage_ratio", "Proporci\u00f3n de da\u00f1o en barrido");
    }
}

