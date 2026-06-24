package com.fspawner.integration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogue of Infernal Mobs modifiers. The keys are the EXACT internal names
 * expected by {@code InfernalMobsCore.addEntityModifiersByString} (extracted
 * from infernalmobs-1.20.1.11.jar), the values are friendly Spanish labels for
 * the GUI/tooltip.
 */
public final class InfernalModifiers {

    private InfernalModifiers() {}

    /** internalName -> Spanish friendly label. Order preserved for the GUI. */
    public static final Map<String, String> FRIENDLY = new LinkedHashMap<>();

    static {
        FRIENDLY.put("Berserk", "Berserker");
        FRIENDLY.put("LifeSteal", "Robo de Vida");
        FRIENDLY.put("Storm", "Tormenta");
        FRIENDLY.put("Ninja", "Ninja");
        FRIENDLY.put("Vengeance", "Venganza");
        FRIENDLY.put("Fiery", "Fuego");
        FRIENDLY.put("Ender", "Teletransportador");
        FRIENDLY.put("Blastoff", "Lanzamiento");
        FRIENDLY.put("Sticky", "Pegajoso");
        FRIENDLY.put("Sprint", "Velocidad Extrema");
        FRIENDLY.put("Regen", "Regeneracion");
        FRIENDLY.put("1UP", "Resurreccion");
        FRIENDLY.put("Alchemist", "Alquimista");
        FRIENDLY.put("Bulwark", "Bastion");
        FRIENDLY.put("Choke", "Asfixia");
        FRIENDLY.put("Cloaking", "Invisibilidad");
        FRIENDLY.put("Darkness", "Oscuridad");
        FRIENDLY.put("Exhaust", "Agotamiento");
        FRIENDLY.put("Ghastly", "Espectral");
        FRIENDLY.put("Gravity", "Gravedad");
        FRIENDLY.put("Poisonous", "Veneno");
        FRIENDLY.put("Quicksand", "Arenas Movedizas");
        FRIENDLY.put("Rust", "Oxido");
        FRIENDLY.put("Sapper", "Zapador");
        FRIENDLY.put("Unyielding", "Inquebrantable");
        FRIENDLY.put("Weakness", "Debilidad");
        FRIENDLY.put("Webber", "Telaranas");
        FRIENDLY.put("Wither", "Marchitamiento");
    }

    /** Returns the friendly Spanish label or the raw internal name if unknown. */
    public static String friendly(String internal) {
        return FRIENDLY.getOrDefault(internal, internal);
    }
}
