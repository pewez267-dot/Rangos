package com.fantasticshortcuts.gui;

import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Client-only registration of {@code MenuType} &rarr; {@code Screen} bindings. Invoked from
 * the client setup lifecycle event, so no client classes are referenced on a dedicated
 * server.
 */
public final class ClientScreens {

    private ClientScreens() {
    }

    public static void register() {
        MenuScreens.register(ModMenus.SHORTCUTS.get(), ShortcutsScreen::new);
        MenuScreens.register(ModMenus.EDITOR.get(), ShortcutEditorScreen::new);
    }
}
