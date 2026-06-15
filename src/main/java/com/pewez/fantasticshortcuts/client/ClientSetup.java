package com.pewez.fantasticshortcuts.client;

/**
 * Punto de inicialización del cliente. La GUI se abre bajo demanda (vía paquete), por lo que no hay
 * registros de cliente que realizar aquí; se mantiene como gancho del ciclo de vida
 * {@code FMLClientSetupEvent} para futuras necesidades de cliente.
 */
public final class ClientSetup {

    private ClientSetup() {}

    public static void init() {
        // Sin registros de cliente necesarios por ahora.
    }
}
