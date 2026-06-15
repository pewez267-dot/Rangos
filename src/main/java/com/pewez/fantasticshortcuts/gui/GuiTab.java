package com.pewez.fantasticshortcuts.gui;

/**
 * Pestañas del editor de Fantastic Shortcuts. Es un enum neutro (sin código de cliente) para poder
 * referenciar el ordinal tanto en el servidor (paquetes) como en el cliente (GUI).
 */
public enum GuiTab {
    LIST("Lista"),
    CREATE("Crear"),
    SETTINGS("Ajustes");

    public final String label;

    GuiTab(String label) {
        this.label = label;
    }

    public static GuiTab byOrdinal(int ordinal) {
        final GuiTab[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return LIST;
        }
        return values[ordinal];
    }
}
