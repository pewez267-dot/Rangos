package com.fantasticterraform.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas del editor: abrir/cerrar paneles y alternar el modo de la varita
 * (seleccionar / brush). Toda la edicion se controla desde el HUD, no por comandos.
 */
public final class Keybinds {

    public static final String CATEGORY = "key.categories.fantasticterraform";

    public static final KeyMapping OPEN_PANELS = new KeyMapping(
            "key.fantasticterraform.panels", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    public static final KeyMapping TOGGLE_WAND = new KeyMapping(
            "key.fantasticterraform.wand_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    private Keybinds() {
    }
}
