// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FSAttributes
{
    public static final List<Attr> ALL;
    
    private FSAttributes() {
    }
    
    public static String labelFor(final String id) {
        for (final Attr a : FSAttributes.ALL) {
            if (a.id.equals(id)) {
                return a.label;
            }
        }
        return id;
    }
    
    static {
        (ALL = new ArrayList<Attr>()).add(new Attr("minecraft:generic.max_health", "Vida", 20.0));
        FSAttributes.ALL.add(new Attr("minecraft:generic.attack_damage", "Da\u00f1o", 3.0));
        FSAttributes.ALL.add(new Attr("minecraft:generic.armor", "Armadura", 0.0));
        FSAttributes.ALL.add(new Attr("minecraft:generic.armor_toughness", "Dureza", 0.0));
        FSAttributes.ALL.add(new Attr("minecraft:generic.movement_speed", "Velocidad", 0.25));
        FSAttributes.ALL.add(new Attr("minecraft:generic.knockback_resistance", "Resistencia de Empuje", 0.0));
        FSAttributes.ALL.add(new Attr("minecraft:generic.follow_range", "Rango de Detecci\u00f3n", 16.0));
    }
    
    public static final class Attr
    {
        public final String id;
        public final String label;
        public final double defaultValue;
        
        public Attr(final String id, final String label, final double defaultValue) {
            this.id = id;
            this.label = label;
            this.defaultValue = defaultValue;
        }
    }
}
