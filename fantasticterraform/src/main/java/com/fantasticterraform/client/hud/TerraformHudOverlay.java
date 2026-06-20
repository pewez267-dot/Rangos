package com.fantasticterraform.client.hud;

import com.fantasticterraform.client.ClientEditorState;
import com.fantasticterraform.client.ClientSelectionState;
import com.fantasticterraform.client.ClientToolState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Overlay del HUD en tiempo real (IGuiOverlay). Se dibuja encima de la vista del juego
 * sin pausar ni tapar el mundo: muestra siempre el estado de la seleccion, la
 * herramienta activa, una pista de teclas y la barra de progreso de operaciones.
 */
public final class TerraformHudOverlay implements IGuiOverlay {

    public static final TerraformHudOverlay INSTANCE = new TerraformHudOverlay();

    private TerraformHudOverlay() {
    }

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics g,
                       float partialTick, int screenWidth, int screenHeight) {
        if (!ClientEditorState.isActive() && !com.fantasticterraform.client.ClientWand.holding()) {
            return;
        }
        // Si el panel de control está abierto, no dibujar el overlay (evita solaparse).
        if (Minecraft.getInstance().screen instanceof com.fantasticterraform.client.hud.TerraformPanelScreen) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int x = 6;
        int y = 6;
        int w = 188;

        g.fill(x - 3, y - 3, x + w, y + 104, 0xF7101018);
        g.drawString(font, "\u00a7d\u2726 \u00a7fFantastic Terraform", x, y, 0xFFFFFF, false);
        y += 12;
        g.drawString(font, "\u00a77Modo: \u00a7f" + ClientSelectionState.type().displayName(), x, y, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "\u00a77Puntos: \u00a7f" + ClientSelectionState.points().size()
                + (ClientSelectionState.closed() ? " \u00a7a(cerrada)" : ""), x, y, 0xFFFFFF, false);
        y += 10;
        String vol = "\u00a77Volumen: \u00a7f" + ClientSelectionState.volume();
        g.drawString(font, vol, x, y, 0xFFFFFF, false);
        y += 10;
        boolean valid = ClientSelectionState.valid();
        g.drawString(font, "\u00a77Seleccion: " + (valid ? "\u00a7avalida" : "\u00a7cincompleta"), x, y, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "\u00a77Varita: \u00a7f" + ClientToolState.wandMode
                + "  \u00a77Brush: \u00a7f" + ClientToolState.brushId, x, y, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "\u00a78[G] Paneles  [V] Modo varita", x, y, 0xFFFFFF, false);
        y += 12;

        if (ClientEditorState.hasRecentProgress()) {
            int total = Math.max(1, ClientEditorState.progressTotal());
            int processed = ClientEditorState.progressProcessed();
            float frac = Math.max(0F, Math.min(1F, processed / (float) total));
            int barW = w - 6;
            // Marco + relleno con gradiente segun estado.
            g.fill(x - 1, y - 1, x + barW + 1, y + 8, 0xFF000000);
            g.fill(x, y, x + barW, y + 7, 0xFF24242E);
            int fillColor = ClientEditorState.progressDone() ? 0xFF45D045 : 0xFF45A6FF;
            g.fill(x, y, x + (int) (barW * frac), y + 7, fillColor);
            int pct = (int) (frac * 100);
            g.drawString(font, "\u00a7f" + ClientEditorState.progressName() + " \u00a77" + pct + "%", x + 2, y, 0xFFFFFF, false);
            y += 10;
            String line = "\u00a77" + processed + "/" + total;
            int rate = ClientEditorState.progressRate();
            if (rate > 0 && !ClientEditorState.progressDone()) {
                line += " \u00a78| \u00a7f" + formatRate(rate) + "\u00a77/s";
                int eta = ClientEditorState.progressEtaSeconds();
                if (eta >= 0) {
                    line += " \u00a78| \u00a7fETA " + formatTime(eta);
                }
            } else if (ClientEditorState.progressDone()) {
                line += " \u00a7a\u2714 completado";
            }
            g.drawString(font, line, x, y, 0xFFFFFF, false);
        }
    }

    private static String formatRate(int perSec) {
        if (perSec >= 1_000_000) {
            return String.format("%.1fM", perSec / 1_000_000.0);
        }
        if (perSec >= 1000) {
            return String.format("%.1fk", perSec / 1000.0);
        }
        return Integer.toString(perSec);
    }

    private static String formatTime(int seconds) {
        if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }
}
