package com.fspawner.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of entity attributes FSpawner lets admins edit, with friendly Spanish
 * labels. The id is the registry name written into the entity NBT Attributes
 * list ("Name" field).
 */
public final class FSAttributes {

    private FSAttributes() {}

    public static final class Attr {
        public final String id;
        public final String label;
        public final double defaultValue;

        public Attr(String id, String label, double defaultValue) {
            this.id = id;
            this.label = label;
            this.defaultValue = defaultValue;
        }
    }

    public static final List<Attr> ALL = new ArrayList<>();

    static {
        ALL.add(new Attr("minecraft:generic.max_health", "Vida", 20.0));
        ALL.add(new Attr("minecraft:generic.attack_damage", "Da\u00f1o", 3.0));
        ALL.add(new Attr("minecraft:generic.armor", "Armadura", 0.0));
        ALL.add(new Attr("minecraft:generic.armor_toughness", "Dureza", 0.0));
        ALL.add(new Attr("minecraft:generic.movement_speed", "Velocidad", 0.25));
        ALL.add(new Attr("minecraft:generic.knockback_resistance", "Resistencia de Empuje", 0.0));
        ALL.add(new Attr("minecraft:generic.follow_range", "Rango de Detecci\u00f3n", 16.0));
    }

    public static String labelFor(String id) {
        for (Attr a : ALL) {
            if (a.id.equals(id)) {
                return a.label;
            }
        }
        return id;
    }
}
