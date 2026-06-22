package com.fantasticpass.gui;

import com.fantasticpass.nametag.NametagBuilder;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared dark "Battle Pass" palette (Valorant/Apex inspired) and small drawing helpers
 * used by every Fantastic Pass screen and widget. Colors are stored as 0xRRGGBB; OR with
 * {@code 0xFF000000} when an opaque ARGB value is required.
 */
public final class GuiTheme {

    public static final int BACKGROUND = 0x0A0A0F;
    public static final int PANEL = 0x14141C;
    public static final int PANEL_LIGHT = 0x1E1E2A;
    public static final int BORDER = 0x33333F;

    public static final int ACCENT_CYAN = 0x00E5FF;
    public static final int ACCENT_CYAN_DIM = 0x0A3A44;
    public static final int ACCENT_GOLD = 0xFFD700;
    public static final int ACCENT_GOLD_DIM = 0x4A3D00;

    public static final int TEXT_PRIMARY = 0xFFFFFF;
    public static final int TEXT_SECONDARY = 0xAAAAAA;
    public static final int SILVER = 0xC0C0C8;
    public static final int LOCKED = 0x3A3A42;

    private GuiTheme() {
    }

    /** Fills the full screen with the deep background plus a subtle vertical gradient. */
    public static void drawBackground(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0xFF000000 | BACKGROUND);
        graphics.fillGradient(0, 0, width, height, 0x110A0A0F, 0x6600121A);
    }

    /** Draws a filled panel with a 1px border. */
    public static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000 | PANEL);
        graphics.renderOutline(x, y, w, h, 0xFF000000 | BORDER);
    }

    /** Draws a filled panel with a colored accent border. */
    public static void drawAccentPanel(GuiGraphics graphics, int x, int y, int w, int h, int accentRgb) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000 | PANEL_LIGHT);
        graphics.renderOutline(x, y, w, h, 0xFF000000 | accentRgb);
    }

    /**
     * A pulsing cyan suitable for "unlocked, unclaimed" tiers. Interpolates between a dim
     * and bright cyan based on wall-clock time so it animates client-side every frame.
     */
    public static int cyanPulse() {
        double phase = (System.currentTimeMillis() % 1400L) / 1400.0D;
        float t = (float) ((Math.sin(phase * Math.PI * 2.0D) + 1.0D) / 2.0D);
        return NametagBuilder.lerpColor(ACCENT_CYAN_DIM, ACCENT_CYAN, t);
    }

    /** A pulsing gold suitable for premium highlights. */
    public static int goldPulse() {
        double phase = (System.currentTimeMillis() % 1600L) / 1600.0D;
        float t = (float) ((Math.sin(phase * Math.PI * 2.0D) + 1.0D) / 2.0D);
        return NametagBuilder.lerpColor(ACCENT_GOLD_DIM, ACCENT_GOLD, t);
    }
}
