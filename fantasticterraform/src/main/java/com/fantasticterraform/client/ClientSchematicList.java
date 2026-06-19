package com.fantasticterraform.client;

import java.util.ArrayList;
import java.util.List;

/** Cache client-side de los nombres de schematic disponibles, recibidos del servidor. */
public final class ClientSchematicList {

    private static volatile List<String> files = new ArrayList<>();

    private ClientSchematicList() {
    }

    public static void set(List<String> list) {
        files = new ArrayList<>(list);
    }

    public static List<String> files() {
        return files;
    }
}
