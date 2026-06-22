package com.fantasticranks.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * Three stacked R/G/B sliders (0-255) editing a single color. Updates are pushed to the
 * listener on drag/click; {@link #setColor(int)} keeps it in sync with the wheel/hex.
 */
public class RgbSliderWidget extends AbstractWidget {

    private static final int CHANNELS = 3;
    private static final int LABEL_WIDTH = 14;

    private int red;
    private int green;
    private int blue;

    private final IntConsumer onColorChanged;
    private int draggingChannel = -1;

    public RgbSliderWidget(int x, int y, int width, int height, IntConsumer onColorChanged) {
        super(x, y, width, height, Component.literal("RGB"));
        this.onColorChanged = onColorChanged;
    }

    public int getColor() {
        return (red << 16) | (green << 8) | blue;
    }

    public void setColor(int rgb) {
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
    }

    private int rowHeight() {
        return this.height / CHANNELS;
    }

    private int trackX() {
        return getX() + LABEL_WIDTH;
    }

    private int trackWidth() {
        return this.width - LABEL_WIDTH;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int rowH = rowHeight();
        int[] values = {red, green, blue};
        String[] labels = {"R", "G", "B"};
        int[] baseColors = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF};

        for (int k = 0; k < CHANNELS; k++) {
            int rowY = getY() + k * rowH;
            int midY = rowY + rowH / 2;

            graphics.drawString(Minecraft.getInstance().font, labels[k], getX(), midY - 4, baseColors[k], false);

            graphics.fill(trackX(), midY - 2, trackX() + trackWidth(), midY + 2, 0xFF202028);
            graphics.renderOutline(trackX(), midY - 2, trackWidth(), 4, 0xFF000000);

            int knobX = trackX() + Math.round(values[k] / 255.0F * (trackWidth() - 1));
            graphics.fill(knobX - 2, rowY + 2, knobX + 3, rowY + rowH - 2, baseColors[k]);
            graphics.renderOutline(knobX - 2, rowY + 2, 5, rowH - 4, 0xFF000000);

            graphics.drawString(Minecraft.getInstance().font, String.valueOf(values[k]),
                    trackX() + trackWidth() - 22, rowY + 1, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.active || !this.visible) {
            return false;
        }
        if (mouseX < trackX() || mouseX > trackX() + trackWidth() || mouseY < getY() || mouseY > getY() + this.height) {
            return false;
        }
        int channel = (int) ((mouseY - getY()) / rowHeight());
        if (channel < 0 || channel >= CHANNELS) {
            return false;
        }
        draggingChannel = channel;
        applyChannel(channel, mouseX);
        return true;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (draggingChannel >= 0) {
            applyChannel(draggingChannel, mouseX);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingChannel = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applyChannel(int channel, double mouseX) {
        float ratio = (float) ((mouseX - trackX()) / Math.max(1, trackWidth() - 1));
        int value = Math.max(0, Math.min(255, Math.round(ratio * 255.0F)));
        switch (channel) {
            case 0 -> red = value;
            case 1 -> green = value;
            default -> blue = value;
        }
        if (onColorChanged != null) {
            onColorChanged.accept(getColor());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
