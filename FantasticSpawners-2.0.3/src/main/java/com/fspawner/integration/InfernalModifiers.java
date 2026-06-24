// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.integration;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InfernalModifiers
{
    public static final Map<String, String> FRIENDLY;
    
    private InfernalModifiers() {
    }
    
    public static String friendly(final String internal) {
        return InfernalModifiers.FRIENDLY.getOrDefault(internal, internal);
    }
    
    static {
        (FRIENDLY = new LinkedHashMap<String, String>()).put("Berserk", "Berserker");
        InfernalModifiers.FRIENDLY.put("LifeSteal", "Robo de Vida");
        InfernalModifiers.FRIENDLY.put("Storm", "Tormenta");
        InfernalModifiers.FRIENDLY.put("Ninja", "Ninja");
        InfernalModifiers.FRIENDLY.put("Vengeance", "Venganza");
        InfernalModifiers.FRIENDLY.put("Fiery", "Fuego");
        InfernalModifiers.FRIENDLY.put("Ender", "Teletransportador");
        InfernalModifiers.FRIENDLY.put("Blastoff", "Lanzamiento");
        InfernalModifiers.FRIENDLY.put("Sticky", "Pegajoso");
        InfernalModifiers.FRIENDLY.put("Sprint", "Velocidad Extrema");
        InfernalModifiers.FRIENDLY.put("Regen", "Regeneracion");
        InfernalModifiers.FRIENDLY.put("1UP", "Resurreccion");
        InfernalModifiers.FRIENDLY.put("Alchemist", "Alquimista");
        InfernalModifiers.FRIENDLY.put("Bulwark", "Bastion");
        InfernalModifiers.FRIENDLY.put("Choke", "Asfixia");
        InfernalModifiers.FRIENDLY.put("Cloaking", "Invisibilidad");
        InfernalModifiers.FRIENDLY.put("Darkness", "Oscuridad");
        InfernalModifiers.FRIENDLY.put("Exhaust", "Agotamiento");
        InfernalModifiers.FRIENDLY.put("Ghastly", "Espectral");
        InfernalModifiers.FRIENDLY.put("Gravity", "Gravedad");
        InfernalModifiers.FRIENDLY.put("Poisonous", "Veneno");
        InfernalModifiers.FRIENDLY.put("Quicksand", "Arenas Movedizas");
        InfernalModifiers.FRIENDLY.put("Rust", "Oxido");
        InfernalModifiers.FRIENDLY.put("Sapper", "Zapador");
        InfernalModifiers.FRIENDLY.put("Unyielding", "Inquebrantable");
        InfernalModifiers.FRIENDLY.put("Weakness", "Debilidad");
        InfernalModifiers.FRIENDLY.put("Webber", "Telaranas");
        InfernalModifiers.FRIENDLY.put("Wither", "Marchitamiento");
    }
}
