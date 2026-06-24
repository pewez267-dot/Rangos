package com.fantasticterraform.client;

import com.fantasticterraform.selection.SelectionWand;
import net.minecraft.client.Minecraft;

/**
 * Utilidades de cliente para saber si el jugador esta sosteniendo la varita y si el
 * HUD debe estar disponible. El HUD/varita funcionan tanto en el modo editor
 * (espectador) como sosteniendo la varita en creativo (donde si es visible).
 */
public final class ClientWand {

    private ClientWand() {
    }

    public static boolean holding() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && SelectionWand.isWand(mc.player.getMainHandItem());
    }

    public static boolean hudAvailable() {
        return ClientEditorState.isActive() || holding();
    }
}
