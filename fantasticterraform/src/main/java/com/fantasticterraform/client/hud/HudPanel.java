package com.fantasticterraform.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Un panel de herramientas del HUD (una pestana). Construye sus controles dentro del
 * area de contenido y puede dibujar texto/indicadores adicionales.
 */
public interface HudPanel {

    String title();

    /** Anade los controles (botones, sliders, campos) al area de contenido. */
    void build(TerraformPanelScreen screen, int x, int y, int width, int height);

    /** Linea de estado fija (pie del panel), o {@code null}. No se desplaza con el scroll. */
    default String status() {
        return null;
    }

    /** Dibuja texto o indicadores extra del panel (opcional, dentro del area desplazable). */
    default void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
