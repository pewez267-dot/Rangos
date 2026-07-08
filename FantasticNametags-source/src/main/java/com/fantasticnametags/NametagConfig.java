package com.fantasticnametags;

import net.minecraftforge.common.ForgeConfigSpec;

public final class NametagConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue HEIGHT_OFFSET;
    public static final ForgeConfigSpec.BooleanValue PLAYERS_ONLY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Fantastic Nametags - ajustes de altura de las lineas del nametag.",
                  "Config de tipo SERVER: se sincroniza a todos los clientes conectados.").push("nametag");
        HEIGHT_OFFSET = b
            .comment("Altura extra (en bloques) para SUBIR todas las lineas del nametag (nombre + rangos)",
                     "por encima de la cabeza. Positivo = mas arriba, negativo = mas abajo. 0 = sin cambios.")
            .defineInRange("height_offset", 0.35, -2.0, 4.0);
        PLAYERS_ONLY = b
            .comment("Si es true, solo afecta a jugadores. Si es false, tambien a otras entidades con nombre.")
            .define("players_only", true);
        b.pop();
        SPEC = b.build();
    }

    private NametagConfig() {
    }
}
