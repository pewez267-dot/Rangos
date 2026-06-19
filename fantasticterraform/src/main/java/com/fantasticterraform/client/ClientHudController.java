package com.fantasticterraform.client;

import com.fantasticterraform.client.hud.TerraformPanelScreen;
import net.minecraft.client.Minecraft;

/**
 * Coordina el HUD del lado cliente. El HUD es un overlay en tiempo real (ver
 * {@code TerraformHudOverlay}) que siempre muestra los indicadores. Para INTERACTUAR
 * con los paneles (botones/sliders) se abre una pantalla transparente que no pausa el
 * juego ({@link TerraformPanelScreen}); el mundo sigue renderizandose detras.
 *
 * <p>Nota tecnica: en Forge 1.20.1 no es posible tener simultaneamente el cursor libre
 * para clicar paneles y el control de camara de vuelo. Por eso se alterna entre "modo
 * vuelo" (camara libre, varita activa) y "modo paneles" (cursor libre) con una tecla,
 * sin pausar nunca el mundo. Esta es la unica via viable y se documenta como tal.</p>
 */
public final class ClientHudController {

    private ClientHudController() {
    }

    public static void onEditorOpened() {
        // El overlay aparece automaticamente cuando ClientEditorState.isActive() es true.
    }

    public static void onEditorClosed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerraformPanelScreen) {
            mc.setScreen(null);
        }
    }

    public static boolean isPanelMode() {
        return Minecraft.getInstance().screen instanceof TerraformPanelScreen;
    }

    public static void togglePanelMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerraformPanelScreen) {
            mc.setScreen(null);
        } else if (mc.screen == null && ClientWand.hudAvailable()) {
            mc.setScreen(new TerraformPanelScreen());
        }
    }
}
