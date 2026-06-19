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
        Font font = Minecraft.getInstance().font;
        int x = 6;
        int y = 6;
        int w = 188;

        g.fill(x - 3, y - 3, x + w, y + 92, 0xF7101018);
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
            float frac = Math.max(0F, Math.min(1F, ClientEditorState.progressProcessed() / (float) total));
            int barW = w - 6;
            g.fill(x, y, x + barW, y + 6, 0xFF303040);
            g.fill(x, y, x + (int) (barW * frac), y + 6, ClientEditorState.progressDone() ? 0xFF40C040 : 0xFF40A0FF);
            g.drawString(font, "\u00a77" + ClientEditorState.progressName() + " "
                    + ClientEditorState.progressProcessed() + "/" + total, x, y + 8, 0xFFFFFF, false);
        }
    }
}
