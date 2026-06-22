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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinator for the complete nametag color editor. It owns the {@link NametagStyle}
 * being edited and wires together the HSB wheel, RGB sliders, hex input, format toggles,
 * the gradient switch with start/end targets, the predefined palette, and the live
 * preview, keeping every control bidirectionally synced.
 *
 * <p>The owning {@link ColorEditorScreen} registers the produced widgets and forwards
 * palette clicks; this class holds no rendering of its own beyond the palette helper.
 */
public final class ColorEditorWidget {

    /** Which color slot the wheel/sliders/hex are currently editing. */
    public enum Target {
        SOLID,
        GRADIENT_START,
        GRADIENT_END
    }

    /** Mirror of {@code Screen#addRenderableWidget}. */
    public interface WidgetSink {
        <T extends GuiEventListener & Renderable & NarratableEntry> T accept(T widget);
    }

    private NametagStyle style;
    private String rankText;
    private int previewLevel = 100;
    private String previewName = "Player";

    private Target target = Target.SOLID;

    private ColorWheelWidget wheel;
    private RgbSliderWidget rgb;
    private HexInputWidget hex;
    private NametagPreviewWidget preview;
    private GradientToggleWidget gradientToggle;
    private Button startButton;
    private Button endButton;

    // Palette layout (rendered + clicked by the screen).
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

    /**
     * Creates and registers every control. Layout flows downward from (x, y).
     *
     * @return the total height consumed, so the screen can place the palette/preview.
     */
    public void build(WidgetSink sink, Font font, int x, int y) {
        // HSB wheel.
        wheel = sink.accept(new ColorWheelWidget(x, y, 150, 90, this::setActiveColor));

        // Hex input to the right of the wheel.
        hex = sink.accept(new HexInputWidget(font, x + 160, y, 78, 18, this::setActiveColor));

        // Gradient toggle + start/end target buttons under the hex field.
        gradientToggle = sink.accept(new GradientToggleWidget(x + 160, y + 24, 78, 16,
                Component.translatable("fantasticpass.gui.gradient"), style.isGradient(), this::onGradientToggle));

        startButton = sink.accept(Button.builder(Component.literal("Start"), b -> selectTarget(Target.GRADIENT_START))
                .bounds(x + 160, y + 44, 38, 16).build());
        endButton = sink.accept(Button.builder(Component.literal("End"), b -> selectTarget(Target.GRADIENT_END))
                .bounds(x + 200, y + 44, 38, 16).build());

        // Copy code button.
        sink.accept(Button.builder(Component.translatable("fantasticpass.gui.copy"), b -> copyCode())
                .bounds(x + 160, y + 64, 78, 16).build());

        // RGB sliders below the wheel.
        rgb = sink.accept(new RgbSliderWidget(x, y + 96, 150, 42, this::setActiveColor));

        // Format toggles row.
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

        // Palette block.
        paletteX = x;
        paletteY = ftY + 22;

        // Preview at the bottom.
        int previewY = paletteY + paletteHeight() + 6;
        preview = sink.accept(new NametagPreviewWidget(x, previewY, 240, 46));

        // Initialize the active color + control sync + button visibility.
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

    /** Draws the predefined Minecraft color palette; called from the screen's render. */
    public void renderPalette(net.minecraft.client.gui.GuiGraphics graphics) {
        for (int i = 0; i < palette.size(); i++) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx = paletteX + col * SWATCH;
            int sy = paletteY + row * SWATCH;
            graphics.fill(sx, sy, sx + SWATCH - 1, sy + SWATCH - 1, 0xFF000000 | palette.get(i));
            graphics.renderOutline(sx, sy, SWATCH - 1, SWATCH - 1, 0xFF000000);
        }
    }

    /** Handles a click in the palette region; returns true if a swatch was selected. */
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
        String code = PassSerializer.toFormatCodeString(style, rankText);
        Minecraft.getInstance().keyboardHandler.setClipboard(code);
    }
}
