package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassSerializer;
import com.fantasticpass.gui.widgets.ColorWheelWidget;
import com.fantasticpass.gui.widgets.GradientToggleWidget;
import com.fantasticpass.gui.widgets.HexInputWidget;
import com.fantasticpass.gui.widgets.NametagPreviewWidget;
import com.fantasticpass.gui.widgets.RgbSliderWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinator for the complete nametag color editor. Wires the HSB wheel, RGB sliders, hex
 * input, format toggles, gradient switch with start/end targets, predefined palette, and a
 * live preview, kept bidirectionally synced. The compact two-column layout fits common GUI
 * sizes without overlap. The owning {@link ColorEditorScreen} registers the produced
 * widgets and forwards palette clicks.
 */
public final class ColorEditorWidget {

    public enum Target {
        SOLID,
        GRADIENT_START,
        GRADIENT_END
    }

    /** Mirror of {@code Screen#addRenderableWidget}. */
    public interface WidgetSink {
        <T extends GuiEventListener & Renderable & NarratableEntry> T accept(T widget);
    }

    private static final int SWATCH = 13;
    private static final int SWATCHES_PER_ROW = 8;

    private final NametagStyle style;
    private String rankText;
    private int previewLevel = 100;
    private String previewName = "Player";

    private Target target = Target.SOLID;

    private ColorWheelWidget wheel;
    private RgbSliderWidget rgb;
    private HexInputWidget hex;
    private NametagPreviewWidget preview;
    private Button startButton;
    private Button endButton;

    private int paletteX;
    private int paletteY;
    private final List<Integer> palette = new ArrayList<>();

    public ColorEditorWidget(NametagStyle style, String rankText) {
        this.style = style == null ? new NametagStyle() : style.copy();
        this.rankText = rankText == null ? "" : rankText;
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.isColor() && formatting.getColor() != null) {
                palette.add(formatting.getColor() & 0xFFFFFF);
            }
        }
    }

    public NametagStyle getStyle() {
        return style;
    }

    public void setRankText(String rankText) {
        this.rankText = rankText == null ? "" : rankText;
        refresh();
    }

    public void setPreviewContext(String name, int level) {
        this.previewName = name;
        this.previewLevel = level;
        refresh();
    }

    /** Total height consumed below {@code y}, so the screen can size itself. */
    public int totalHeight() {
        return 174;
    }

    public void build(WidgetSink sink, Font font, int x, int y) {
        // Left column.
        wheel = sink.accept(new ColorWheelWidget(x, y, 120, 56, this::setActiveColor));
        rgb = sink.accept(new RgbSliderWidget(x, y + 60, 120, 32, this::setActiveColor));

        int ftY = y + 96;
        sink.accept(new GradientToggleWidget(x, ftY, 27, 16, Component.literal("B"), style.isBold(), v -> {
            style.setBold(v);
            refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 31, ftY, 27, 16, Component.literal("I"), style.isItalic(), v -> {
            style.setItalic(v);
            refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 62, ftY, 27, 16, Component.literal("U"), style.isUnderline(), v -> {
            style.setUnderline(v);
            refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 93, ftY, 27, 16, Component.literal("S"), style.isStrikethrough(), v -> {
            style.setStrikethrough(v);
            refresh();
        }));

        paletteX = x;
        paletteY = y + 116;

        // Right column.
        int rx = x + 130;
        hex = sink.accept(new HexInputWidget(font, rx, y, 100, 16, this::setActiveColor));
        sink.accept(new GradientToggleWidget(rx, y + 20, 100, 16,
                Component.translatable("fantasticpass.gui.gradient"), style.isGradient(), this::onGradientToggle));
        startButton = sink.accept(Button.builder(Component.literal("Start"), b -> selectTarget(Target.GRADIENT_START))
                .bounds(rx, y + 38, 48, 16).build());
        endButton = sink.accept(Button.builder(Component.literal("End"), b -> selectTarget(Target.GRADIENT_END))
                .bounds(rx + 52, y + 38, 48, 16).build());
        sink.accept(Button.builder(Component.translatable("fantasticpass.gui.copy"), b -> copyCode())
                .bounds(rx, y + 56, 100, 16).build());

        // Preview spanning the bottom.
        preview = sink.accept(new NametagPreviewWidget(x, y + 146, 240, 28));

        target = style.isGradient() ? Target.GRADIENT_START : Target.SOLID;
        syncControls(activeColor());
        updateTargetVisibility();
        refresh();
    }

    public void renderPalette(GuiGraphics graphics) {
        for (int i = 0; i < palette.size(); i++) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx = paletteX + col * SWATCH;
            int sy = paletteY + row * SWATCH;
            graphics.fill(sx, sy, sx + SWATCH - 1, sy + SWATCH - 1, 0xFF000000 | palette.get(i));
            graphics.renderOutline(sx, sy, SWATCH - 1, SWATCH - 1, 0xFF000000);
        }
    }

    public boolean handlePaletteClick(double mouseX, double mouseY) {
        for (int i = 0; i < palette.size(); i++) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx = paletteX + col * SWATCH;
            int sy = paletteY + row * SWATCH;
            if (mouseX >= sx && mouseX < sx + SWATCH - 1 && mouseY >= sy && mouseY < sy + SWATCH - 1) {
                setActiveColor(palette.get(i));
                return true;
            }
        }
        return false;
    }

    private int activeColor() {
        return switch (target) {
            case SOLID -> style.getColor();
            case GRADIENT_START -> style.getGradientStart();
            case GRADIENT_END -> style.getGradientEnd();
        };
    }

    private void setActiveColor(int color) {
        switch (target) {
            case SOLID -> style.setColor(color);
            case GRADIENT_START -> style.setGradientStart(color);
            case GRADIENT_END -> style.setGradientEnd(color);
        }
        syncControls(color);
        refresh();
    }

    private void selectTarget(Target newTarget) {
        this.target = newTarget;
        syncControls(activeColor());
    }

    private void onGradientToggle(boolean on) {
        style.setGradient(on);
        target = on ? Target.GRADIENT_START : Target.SOLID;
        syncControls(activeColor());
        updateTargetVisibility();
        refresh();
    }

    private void updateTargetVisibility() {
        boolean gradient = style.isGradient();
        if (startButton != null) {
            startButton.visible = gradient;
            startButton.active = gradient;
        }
        if (endButton != null) {
            endButton.visible = gradient;
            endButton.active = gradient;
        }
    }

    private void syncControls(int color) {
        if (wheel != null) {
            wheel.setColor(color);
        }
        if (rgb != null) {
            rgb.setColor(color);
        }
        if (hex != null) {
            hex.setColorSilently(color);
        }
    }

    private void refresh() {
        if (preview != null) {
            preview.setStyle(style);
            preview.setRankText(rankText);
            preview.setLevel(previewLevel);
            preview.setPlayerName(previewName);
        }
    }

    private void copyCode() {
        Minecraft.getInstance().keyboardHandler.setClipboard(PassSerializer.toFormatCodeString(style, rankText));
    }
}
