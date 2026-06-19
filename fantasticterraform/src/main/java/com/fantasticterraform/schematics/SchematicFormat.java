package com.fantasticterraform.schematics;

import java.util.Locale;

/**
 * Formatos de schematic soportados, con su extension de archivo.
 */
public enum SchematicFormat {

    SPONGE("schem", "Sponge (.schem)"),
    LITEMATICA("litematic", "Litematica (.litematic)"),
    VANILLA("nbt", "Estructura vanilla (.nbt)");

    private final String extension;
    private final String displayName;

    SchematicFormat(String extension, String displayName) {
        this.extension = extension;
        this.displayName = displayName;
    }

    public String extension() {
        return extension;
    }

    public String displayName() {
        return displayName;
    }

    public static SchematicFormat fromFileName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".litematic")) {
            return LITEMATICA;
        }
        if (lower.endsWith(".nbt")) {
            return VANILLA;
        }
        return SPONGE;
    }
}
