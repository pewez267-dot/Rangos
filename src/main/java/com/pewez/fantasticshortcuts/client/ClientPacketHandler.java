package com.pewez.fantasticshortcuts.client;

import com.pewez.fantasticshortcuts.gui.GuiTab;
import com.pewez.fantasticshortcuts.gui.ShortcutEditorScreen;
import com.pewez.fantasticshortcuts.network.OpenEditorPacket;
import net.minecraft.client.Minecraft;

/**
 * Manejador de paquetes del lado cliente.
 *
 * <p>Solo se carga en el cliente (se invoca a través de {@code DistExecutor.unsafeRunWhenOn} desde
 * los paquetes), por lo que es seguro referenciar clases de cliente como {@code Minecraft} o la
 * pantalla del editor.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    /** Abre o refresca el editor de atajos con el contenido del paquete recibido. */
    public static void openEditor(OpenEditorPacket msg) {
        Minecraft.getInstance().setScreen(new ShortcutEditorScreen(
                msg.shortcuts(),
                GuiTab.byOrdinal(msg.tab()),
                msg.enableReplaceMode(),
                msg.shortcutPriority(),
                msg.auditEnabled()));
    }
}
