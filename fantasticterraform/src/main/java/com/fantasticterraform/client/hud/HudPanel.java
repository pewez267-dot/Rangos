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

    /** Dibuja texto o indicadores extra del panel. */
    default void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
