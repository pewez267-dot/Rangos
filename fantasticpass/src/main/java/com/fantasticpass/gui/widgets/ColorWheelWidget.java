package com.fantasticpass.gui.widgets;

import com.fantasticpass.data.PassSerializer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * HSB color picker: a saturation/brightness square plus a hue strip. Clicking or
 * dragging on either region updates the selected color and notifies the listener.
 * Bidirectionally synced via {@link #setColor(int)}.
 */
public class ColorWheelWidget extends AbstractWidget {

    private static final int HUE_STRIP_WIDTH = 12;
    private static final int GAP = 4;

    private float hue;
    private float saturation;
    private float brightness = 1.0F;

    private final IntConsumer onColorChanged;
    private boolean draggingSquare;
    private boolean draggingHue;

    public ColorWheelWidget(int x, int y, int width, int height, IntConsumer onColorChanged) {
        super(x, y, width, height, Component.literal("Color Picker"));
        this.onColorChanged = onColorChanged;
    }

    private int squareWidth() {
        return this.width - HUE_STRIP_WIDTH - GAP;
    }

    private int hueX() {
        return getX() + this.width - HUE_STRIP_WIDTH;
    }

    /** @return the currently selected packed 0xRRGGBB color */
    public int getColor() {
        return PassSerializer.hsbToRgb(hue, saturation, brightness);
    }

    /** Syncs the picker to an external color without firing the listener. */
    public void setColor(int rgb) {
        float[] hsb = PassSerializer.rgbToHsb(rgb);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int sqX = getX();
        int sqY = getY();
        int sqW = squareWidth();
        int sqH = this.height;

        // Saturation/brightness square: each 1px column is a vertical gradient from the
        // fully-bright hue/saturation color down to black.
        for (int i = 0; i < sqW; i++) {
            float s = sqW <= 1 ? 0.0F : (float) i / (float) (sqW - 1);
            int top = 0xFF000000 | PassSerializer.hsbToRgb(hue, s, 1.0F);
            graphics.fillGradient(sqX + i, sqY, sqX + i + 1, sqY + sqH, top, 0xFF000000);
        }
        graphics.renderOutline(sqX - 1, sqY - 1, sqW + 2, sqH + 2, 0xFF000000);

        // Hue strip.
        int hx = hueX();
        for (int j = 0; j < sqH; j++) {
            float h = sqH <= 1 ? 0.0F : (float) j / (float) (sqH - 1);
            int c = 0xFF000000 | PassSerializer.hsbToRgb(h, 1.0F, 1.0F);
            graphics.fill(hx, sqY + j, hx + HUE_STRIP_WIDTH, sqY + j + 1, c);
        }
        graphics.renderOutline(hx - 1, sqY - 1, HUE_STRIP_WIDTH + 2, sqH + 2, 0xFF000000);

        // Selection markers.
        int markerX = sqX + Math.round(saturation * (sqW - 1));
        int markerY = sqY + Math.round((1.0F - brightness) * (sqH - 1));
        int marker = (saturation + (1.0F - brightness)) > 1.0F ? 0xFFFFFFFF : 0xFF000000;
        graphics.renderOutline(markerX - 2, markerY - 2, 5, 5, marker);

        int hueMarkerY = sqY + Math.round(hue * (sqH - 1));
        graphics.fill(hx - 1, hueMarkerY - 1, hx + HUE_STRIP_WIDTH + 1, hueMarkerY + 2, 0xFFFFFFFF);
        graphics.fill(hx, hueMarkerY, hx + HUE_STRIP_WIDTH, hueMarkerY + 1, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.active || !this.visible) {
            return false;
        }
        if (inSquare(mouseX, mouseY)) {
            draggingSquare = true;
            applySquare(mouseX, mouseY);
            return true;
        }
        if (inHue(mouseX, mouseY)) {
            draggingHue = true;
            applyHue(mouseY);
            return true;
        }
        return false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (draggingSquare) {
            applySquare(mouseX, mouseY);
        } else if (draggingHue) {
            applyHue(mouseY);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSquare = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean inSquare(double mx, double my) {
        return mx >= getX() && mx < getX() + squareWidth() && my >= getY() && my < getY() + this.height;
    }

    private boolean inHue(double mx, double my) {
        return mx >= hueX() && mx < hueX() + HUE_STRIP_WIDTH && my >= getY() && my < getY() + this.height;
    }

    private void applySquare(double mx, double my) {
        int sqW = squareWidth();
        float s = (float) ((mx - getX()) / Math.max(1, sqW - 1));
        float v = 1.0F - (float) ((my - getY()) / Math.max(1, this.height - 1));
        this.saturation = clamp01(s);
        this.brightness = clamp01(v);
        fire();
    }

    private void applyHue(double my) {
        float h = (float) ((my - getY()) / Math.max(1, this.height - 1));
        this.hue = clamp01(h);
        fire();
    }

    private void fire() {
        if (onColorChanged != null) {
            onColorChanged.accept(getColor());
        }
    }

    private static float clamp01(float v) {
        if (v < 0.0F) {
            return 0.0F;
        }
        return Math.min(v, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
