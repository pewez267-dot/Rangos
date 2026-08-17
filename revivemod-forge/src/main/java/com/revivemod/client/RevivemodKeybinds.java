package com.revivemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas DEDICADAS del mod. Antes el mod leia directamente la tecla de
 * inventario (E) y la de intercambiar mano (F), por lo que al re-asignar el
 * inventario las acciones de revivir dejaban de funcionar. Ahora son teclas
 * propias, independientes y re-asignables (en el juego o en Opciones>Controles).
 *
 * Por defecto se mantienen E (rendirse) y F (auto-revivir) para conservar la
 * sensacion original, pero el jugador puede cambiarlas libremente.
 */
public final class RevivemodKeybinds {
    public static final String CATEGORY = "key.categories.revivemod";

    public static final int DEFAULT_SURRENDER_KEY = GLFW.GLFW_KEY_E;
    public static final int DEFAULT_SELF_REVIVE_KEY = GLFW.GLFW_KEY_F;

    public static final KeyMapping SURRENDER = new KeyMapping(
            "key.revivemod.surrender",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            DEFAULT_SURRENDER_KEY,
            CATEGORY);

    public static final KeyMapping SELF_REVIVE = new KeyMapping(
            "key.revivemod.self_revive",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            DEFAULT_SELF_REVIVE_KEY,
            CATEGORY);

    private RevivemodKeybinds() {
    }
}
