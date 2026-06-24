package com.fantasticpass.gui.widgets;

import com.fantasticpass.gui.GuiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Flat dark button matching the Fantastic Pass Valorant/Apex theme: a {@link GuiTheme}
 * panel fill with a colored accent border that brightens on hover and dims when disabled.
 * Replaces the vanilla gray buttons (which clash badly with the dark premium GUI).
 *
 * <p>The accent color is configurable per button (cyan for navigation, gold for the
 * primary claim action). Text is centered and uses the button's own {@code §}-formatted
 * message so callers keep full control over the label.</p>
 */
public class ThemedButton extends AbstractButton {

    /** Click callback (mirrors vanilla {@code Button.OnPress} but framework-agnostic). */
    public interface OnClick {
        void onClick(ThemedButton button);
    }

    private final OnClick onClick;
    private int accentRgb;

    public ThemedButton(int x, int y, int w, int h, Component message, int accentRgb, OnClick onClick) {
        super(x, y, w, h, message);
        this.accentRgb = accentRgb;
        this.onClick = onClick;
    }

    public void setAccent(int rgb) {
        this.accentRgb = rgb;
    }

    @Override
    public void onPress() {
        if (onClick != null) {
            onClick.onClick(this);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hovered = isHoveredOrFocused() && active;

        // base panel
        int fill = active ? (hovered ? GuiTheme.PANEL_LIGHT : GuiTheme.PANEL) : GuiTheme.BACKGROUND;
        g.fill(x, y, x + w, y + h, 0xFF000000 | fill);

        // hover accent wash
        if (hovered) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, (0x22000000) | (accentRgb & 0xFFFFFF));
            // top highlight line
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x55000000 | (accentRgb & 0xFFFFFF));
        }

        // border (brighter when hovered, muted when disabled)
        int borderAlpha = active ? (hovered ? 0xFF : 0xCC) : 0x66;
        int border = (borderAlpha << 24) | (active ? (accentRgb & 0xFFFFFF) : GuiTheme.BORDER);
        g.renderOutline(x, y, w, h, border);

        // label
        int textColor = active ? GuiTheme.TEXT_PRIMARY : GuiTheme.LOCKED;
        int tx = x + w / 2;
        int ty = y + (h - 8) / 2;
        g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                getMessage(), tx, ty, 0xFF000000 | textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
