package com.fantasticchest.gui;

import com.fantasticchest.gui.admin.ChestAdminScreen;
import com.fantasticchest.gui.terminal.ChestTerminalScreen;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client-only binding of menu types to their screens. */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void register() {
        MenuScreens.register(ModMenus.ADMIN_MENU.get(), ChestAdminScreen::new);
        MenuScreens.register(ModMenus.TERMINAL_MENU.get(), ChestTerminalScreen::new);
    }
}
