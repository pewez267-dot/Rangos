package com.fsrecipes.client;

import com.fsrecipes.client.screen.RecipeBanScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/** Puente cliente: cachea el set de baneados y abre/refresca la GUI. Solo se carga en el cliente. */
public final class ClientHooks {

    private ClientHooks() {}

    private static final Set<ResourceLocation> CLIENT_BANS = new HashSet<>();

    public static Set<ResourceLocation> bans() {
        return CLIENT_BANS;
    }

    public static void openScreen(Set<ResourceLocation> bans) {
        CLIENT_BANS.clear();
        CLIENT_BANS.addAll(bans);
        Minecraft.getInstance().setScreen(new RecipeBanScreen());
    }

    public static void updateBans(Set<ResourceLocation> bans) {
        CLIENT_BANS.clear();
        CLIENT_BANS.addAll(bans);
        Screen s = Minecraft.getInstance().screen;
        if (s instanceof RecipeBanScreen screen) {
            screen.onBansUpdated();
        }
    }
}
