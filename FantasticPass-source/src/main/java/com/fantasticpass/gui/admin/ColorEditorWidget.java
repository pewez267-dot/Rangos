package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassSerializer;
import com.fantasticpass.gui.widgets.ColorWheelWidget;
import com.fantasticpass.gui.widgets.GradientToggleWidget;
import com.fantasticpass.gui.widgets.HexInputWidget;
import com.fantasticpass.gui.widgets.NametagPreviewWidget;
import com.fantasticpass.gui.widgets.RgbSliderWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Editor de estilo completo (color solido, gradiente, arcoiris animado, negrita/cursiva/subrayado/tachado)
 * con paleta compacta. Portado del editor de FantasticRanks (layout probado sin solapes).
 */
public final class ColorEditorWidget {
    // Paleta curada y compacta (24 colores, 8 por fila = 3 filas).
    private static final int[] PALETTE = new int[]{
        0xFF5555, 0xFF0000, 0xFF8800, 0xFFAA00, 0xFFFF55, 0xFFFF00, 0x55FF55, 0x00FF00,
        0x00AA00, 0x00FFAA, 0x55FFFF, 0x00FFFF, 0x00AAFF, 0x5555FF, 0x0000FF, 0xAA00FF,
        0xFF55FF, 0xFF00FF, 0xFF0088, 0xFF88CC, 0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000
    };
    private static final int SWATCH = 12;
    private static final int SWATCHES_PER_ROW = 8;

    private final NametagStyle style;
    private String rankText;
    private int previewLevel = 1;
    private String previewName = "Jugador";
    private Target target = Target.SOLID;
    private Screen parentScreen;
    private ColorWheelWidget wheel;
    private RgbSliderWidget rgb;
    private HexInputWidget hex;
    private NametagPreviewWidget preview;
    private GradientToggleWidget gradientToggle;
    private GradientToggleWidget rainbowToggle;
    private Button startButton;
    private Button endButton;
    private Button rainbowPickButton;
    private int paletteX;
    private int paletteY;

    public ColorEditorWidget(NametagStyle style, String rankText) {
        // Se usa el objeto directamente (sin copiar) para que las ediciones persistan
        // cuando la pantalla se re-inicializa al volver del selector de arcoiris.
        this.style = style == null ? new NametagStyle() : style;
        this.rankText = rankText == null ? "" : rankText;
    }

    public NametagStyle getStyle() {
        return this.style;
    }

    public void setParentScreen(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public void setRankText(String rankText) {
        this.rankText = rankText == null ? "" : rankText;
        this.refresh();
    }

    public void setPreviewContext(String name, int level) {
        this.previewName = name;
        this.previewLevel = level;
        this.refresh();
    }

    /**
     * Layout compacto. Columna izquierda: rueda + RGB + estilos. Columna derecha: hex, toggles,
     * gradiente/arcoiris, copiar y paleta. El preview se coloca aparte (attachPreview).
     */
    public void build(WidgetSink sink, Font font, int x, int y) {
        int rx = x + 158;
        // Columna izquierda
        this.wheel = sink.accept(new ColorWheelWidget(x, y, 150, 70, this::setActiveColor));
        this.rgb = sink.accept(new RgbSliderWidget(x, y + 74, 150, 34, this::setActiveColor));
        int ftY = y + 112;
        sink.accept(new GradientToggleWidget(x, ftY, 30, 16, Component.literal("B"), this.style.isBold(), v -> {
            this.style.setBold(v);
            this.refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 34, ftY, 30, 16, Component.literal("I"), this.style.isItalic(), v -> {
            this.style.setItalic(v);
            this.refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 68, ftY, 30, 16, Component.literal("U"), this.style.isUnderline(), v -> {
            this.style.setUnderline(v);
            this.refresh();
        }));
        sink.accept(new GradientToggleWidget(x + 102, ftY, 30, 16, Component.literal("S"), this.style.isStrikethrough(), v -> {
            this.style.setStrikethrough(v);
            this.refresh();
        }));
        // Columna derecha
        this.hex = sink.accept(new HexInputWidget(font, rx, y, 100, 16, this::setActiveColor));
        this.gradientToggle = sink.accept(new GradientToggleWidget(rx, y + 20, 48, 16, Component.literal("Gradiente"), this.style.isGradient(), this::onGradientToggle));
        this.rainbowToggle = sink.accept(new GradientToggleWidget(rx + 52, y + 20, 48, 16, Component.literal("Arcoiris"), this.style.isRainbow(), this::onRainbowToggle));
        this.startButton = sink.accept(Button.builder(Component.literal("Inicio"), b -> this.selectTarget(Target.GRADIENT_START)).bounds(rx, y + 40, 48, 16).build());
        this.endButton = sink.accept(Button.builder(Component.literal("Fin"), b -> this.selectTarget(Target.GRADIENT_END)).bounds(rx + 52, y + 40, 48, 16).build());
        this.rainbowPickButton = sink.accept(Button.builder(Component.literal("Elegir estilo"), b -> this.openRainbowPicker()).bounds(rx, y + 40, 100, 16).build());
        sink.accept(Button.builder(Component.literal("Copiar codigo"), b -> this.copyCode()).bounds(rx, y + 60, 100, 16).build());
        this.paletteX = rx;
        this.paletteY = y + 82;
        this.target = this.style.isGradient() ? Target.GRADIENT_START : Target.SOLID;
        this.syncControls(this.activeColor());
        this.updateControlState();
        this.refresh();
    }

    public void attachPreview(WidgetSink sink, int x, int y, int width, int height) {
        this.preview = sink.accept(new NametagPreviewWidget(x, y, width, height));
        this.refresh();
    }

    public void renderPalette(GuiGraphics graphics) {
        for (int i = 0; i < PALETTE.length; ++i) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx = this.paletteX + col * SWATCH;
            int sy = this.paletteY + row * SWATCH;
            graphics.fill(sx, sy, sx + SWATCH - 1, sy + SWATCH - 1, 0xFF000000 | PALETTE[i]);
            graphics.renderOutline(sx, sy, SWATCH - 1, SWATCH - 1, -16777216);
        }
    }

    public boolean handlePaletteClick(double mouseX, double mouseY) {
        for (int i = 0; i < PALETTE.length; ++i) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx = this.paletteX + col * SWATCH;
            int sy = this.paletteY + row * SWATCH;
            if (mouseX < sx || mouseX >= sx + SWATCH - 1 || mouseY < sy || mouseY >= sy + SWATCH - 1) {
                continue;
            }
            this.setActiveColor(PALETTE[i]);
            return true;
        }
        return false;
    }

    private int activeColor() {
        switch (this.target) {
            case GRADIENT_START:
                return this.style.getGradientStart();
            case GRADIENT_END:
                return this.style.getGradientEnd();
            case SOLID:
            default:
                return this.style.getColor();
        }
    }

    private void setActiveColor(int color) {
        switch (this.target) {
            case SOLID: {
                this.style.setColor(color);
                break;
            }
            case GRADIENT_START: {
                this.style.setGradientStart(color);
                break;
            }
            case GRADIENT_END: {
                this.style.setGradientEnd(color);
            }
        }
        this.syncControls(color);
        this.refresh();
    }

    private void selectTarget(Target newTarget) {
        this.target = newTarget;
        this.syncControls(this.activeColor());
    }

    private void onGradientToggle(boolean on) {
        this.style.setGradient(on);
        if (on) {
            this.style.setRainbow(false);
            if (this.rainbowToggle != null) {
                this.rainbowToggle.setStateSilently(false);
            }
        }
        this.target = on ? Target.GRADIENT_START : Target.SOLID;
        this.syncControls(this.activeColor());
        this.updateControlState();
        this.refresh();
    }

    private void onRainbowToggle(boolean on) {
        this.style.setRainbow(on);
        if (on) {
            this.style.setGradient(false);
            if (this.gradientToggle != null) {
                this.gradientToggle.setStateSilently(false);
            }
            this.target = Target.SOLID;
        }
        this.updateControlState();
        this.refresh();
    }

    private void openRainbowPicker() {
        if (!this.style.isRainbow()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen back = this.parentScreen;
        mc.setScreen(new RainbowPickerScreen(this.style.getRainbowStyle(), s -> {
            this.style.setRainbowStyle(s);
            this.refresh();
        }, () -> mc.setScreen(back)));
    }

    private void updateControlState() {
        boolean rainbow = this.style.isRainbow();
        boolean gradient = this.style.isGradient();
        if (this.wheel != null) {
            this.wheel.active = !rainbow;
        }
        if (this.rgb != null) {
            this.rgb.active = !rainbow;
        }
        if (this.hex != null) {
            this.hex.setEditable(!rainbow);
        }
        if (this.startButton != null) {
            this.startButton.visible = gradient;
            this.startButton.active = gradient;
        }
        if (this.endButton != null) {
            this.endButton.visible = gradient;
            this.endButton.active = gradient;
        }
        if (this.rainbowPickButton != null) {
            this.rainbowPickButton.visible = rainbow;
            this.rainbowPickButton.active = rainbow;
        }
    }

    private void syncControls(int color) {
        if (this.wheel != null) {
            this.wheel.setColor(color);
        }
        if (this.rgb != null) {
            this.rgb.setColor(color);
        }
        if (this.hex != null) {
            this.hex.setColorSilently(color);
        }
    }

    private void refresh() {
        if (this.preview != null) {
            this.preview.setStyle(this.style);
            this.preview.setRankText(this.rankText);
            this.preview.setLevel(this.previewLevel);
            this.preview.setPlayerName(this.previewName);
        }
    }

    private void copyCode() {
        String code = PassSerializer.toFormatCodeString(this.style, this.rankText);
        Minecraft.getInstance().keyboardHandler.setClipboard(code);
    }

    public static enum Target {
        SOLID,
        GRADIENT_START,
        GRADIENT_END;
    }

    public static interface WidgetSink {
        public <T extends GuiEventListener & Renderable & NarratableEntry> T accept(T var1);
    }
}
