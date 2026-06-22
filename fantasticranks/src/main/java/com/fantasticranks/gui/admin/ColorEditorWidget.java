package com.fantasticranks.gui.admin;

import com.fantasticranks.data.NametagStyle;
import com.fantasticranks.data.RanksSerializer;
import com.fantasticranks.gui.widgets.ColorWheelWidget;
import com.fantasticranks.gui.widgets.GradientToggleWidget;
import com.fantasticranks.gui.widgets.HexInputWidget;
import com.fantasticranks.gui.widgets.NametagPreviewWidget;
import com.fantasticranks.gui.widgets.RgbSliderWidget;
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
 * Coordinator for the complete nametag color editor (independent copy for Fantastic
 * Ranks). Wires the HSB wheel, RGB sliders, hex input, format toggles, gradient switch
 * with start/end targets, predefined palette, and the live preview, keeping every control
 * bidirectionally synced. The owning {@link ColorEditorScreen} registers the widgets and
 * forwards palette clicks.
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

    private final NametagStyle style;
    private String rankText;
    private int previewLevel = 1;
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
    private static final int SWATCH = 14;
    private static final int SWATCHES_PER_ROW = 8;

    public ColorEditorWidget(NametagStyle style, String rankText) {
        this.style = style == null ? new NametagStyle() : style.copy();
        this.rankText = rankText == null ? "" : rankText;
        buildPalette();
    }

    private void buildPalette() {
        palette.clear();
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

    public void build(WidgetSink sink, Font font, int x, int y) {
        wheel = sink.accept(new ColorWheelWidget(x, y, 150, 90, this::setActiveColor));
        hex = sink.accept(new HexInputWidget(font, x + 160, y, 78, 18, this::setActiveColor));

        sink.accept(new GradientToggleWidget(x + 160, y + 24, 78, 16,
                Component.translatable("fantasticranks.gui.gradient"), style.isGradient(), this::onGradientToggle));

        startButton = sink.accept(Button.builder(Component.literal("Start"), b -> selectTarget(Target.GRADIENT_START))
                .bounds(x + 160, y + 44, 38, 16).build());
        endButton = sink.accept(Button.builder(Component.literal("End"), b -> selectTarget(Target.GRADIENT_END))
                .bounds(x + 200, y + 44, 38, 16).build());

        sink.accept(Button.builder(Component.translatable("fantasticranks.gui.copy"), b -> copyCode())
                .bounds(x + 160, y + 64, 78, 16).build());

        rgb = sink.accept(new RgbSliderWidget(x, y + 96, 150, 42, this::setActiveColor));

        int ftY = y + 142;
        sink.accept(new GradientToggleWidget(x, ftY, 30, 16, Component.literal("B"),
                style.isBold(), v -> {
            style.setBold(v);
            refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 34, ftY, 30, 16, Component.literal("I"),
                style.isItalic(), v -> {
            style.setItalic(v);
            refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 68, ftY, 30, 16, Component.literal("U"),
                style.isUnderline(), v -> {
            style.setUnderline(v);
            refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 102, ftY, 30, 16, Component.literal("S"),
                style.isStrikethrough(), v -> {
            style.setStrikethrough(v);
            refresh();
        }));

        paletteX = x;
        paletteY = ftY + 22;

        int previewY = paletteY + paletteHeight() + 6;
        preview = sink.accept(new NametagPreviewWidget(x, previewY, 240, 46));

        target = style.isGradient() ? Target.GRADIENT_START : Target.SOLID;
        syncControls(activeColor());
        updateTargetVisibility();
        refresh();
    }

    private int paletteRows() {
        return (palette.size() + SWATCHES_PER_ROW - 1) / SWATCHES_PER_ROW;
    }

    private int paletteHeight() {
        return paletteRows() * SWATCH;
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
        String code = RanksSerializer.toFormatCodeString(style, rankText);
        Minecraft.getInstance().keyboardHandler.setClipboard(code);
    }
}
