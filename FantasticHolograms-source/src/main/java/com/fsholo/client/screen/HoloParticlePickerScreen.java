package com.fsholo.client.screen;

import com.fsholo.data.HoloLine;
import com.fsholo.util.HoloParticles;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Apartado dedicado para las particulas de una linea: elegir estilo (cuadricula scrolleable)
 * y editar TODAS sus propiedades: posicion (ancla), offsets numericos alto/lado, densidad,
 * velocidad, tamano y dispersion.
 */
public class HoloParticlePickerScreen extends Screen {
    private static final int COLS = 2;
    private final HoloLine line;
    private final Runnable onBack;
    private int scroll;
    private int rowsVisible;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int gridTop;
    private int cellW;
    private int cellH;
    private int fy4;

    public HoloParticlePickerScreen(HoloLine line, Runnable onBack) {
        super((Component) Component.literal("Particulas"));
        this.line = line;
        this.onBack = onBack;
    }

    private int totalRows() {
        return (HoloParticles.count() + COLS - 1) / COLS;
    }

    private static String fmt(float v) {
        if (v == (float) ((int) v)) {
            return Integer.toString((int) v);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", Float.valueOf(v));
    }

    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 360);
        this.panelHeight = Math.min(this.height - 20, 300);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.gridTop = this.topPos + 28;
        int b = this.topPos + this.panelHeight;
        int gridBottom = b - 122;
        this.cellW = (this.panelWidth - 24) / COLS;
        this.cellH = 20;
        this.rowsVisible = Math.max(1, (gridBottom - this.gridTop) / this.cellH);
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.totalRows() - this.rowsVisible)));
        int count = HoloParticles.count();
        for (int row = 0; row < this.rowsVisible; ++row) {
            for (int c = 0; c < COLS; ++c) {
                int idx = (this.scroll + row) * COLS + c;
                if (idx >= count) {
                    continue;
                }
                int bx = this.leftPos + 12 + c * this.cellW;
                int by = this.gridTop + row * this.cellH;
                boolean sel = idx == this.line.particleStyle;
                String label = (sel ? "\u00a7a\u25b6 \u00a7f" : "\u00a77") + HoloParticles.name(idx);
                int fidx = idx;
                this.addRenderableWidget(Button.builder((Component) Component.literal(label), b2 -> {
                    this.line.particleStyle = fidx;
                    this.rebuildWidgets();
                }).bounds(bx, by, this.cellW - 8, 18).build());
            }
        }
        int fullW = this.panelWidth - 24;
        int halfW = (fullW - 4) / 2;
        int lx = this.leftPos + 12;
        int rxh = lx + halfW + 4;
        int fy1 = b - 110;
        int fy2 = b - 88;
        int fy3 = b - 66;
        this.fy4 = b - 44;
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bPosici\u00f3n: \u00a7f" + HoloParticles.anchorName(this.line.particleAnchor))), b2 -> {
            this.line.particleAnchor = (this.line.particleAnchor + 1) % HoloParticles.anchorCount();
            this.rebuildWidgets();
        }).bounds(lx, fy1, fullW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bDensidad: \u00a7f" + Math.max(1, Math.min(4, this.line.particleDensity)))), b2 -> {
            this.line.particleDensity = this.line.particleDensity >= 4 ? 1 : this.line.particleDensity + 1;
            this.rebuildWidgets();
        }).bounds(lx, fy2, halfW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bVelocidad: \u00a7f" + HoloParticles.speedName(this.line.particleSpeed))), b2 -> {
            this.line.particleSpeed = (this.line.particleSpeed + 1) % HoloParticles.speedCount();
            this.rebuildWidgets();
        }).bounds(rxh, fy2, halfW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bTama\u00f1o: \u00a7f" + HoloParticles.sizeName(this.line.particleSize))), b2 -> {
            this.line.particleSize = (this.line.particleSize + 1) % HoloParticles.sizeCount();
            this.rebuildWidgets();
        }).bounds(lx, fy3, halfW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bDispersi\u00f3n: \u00a7f" + HoloParticles.spreadName(this.line.particleSpread))), b2 -> {
            this.line.particleSpread = (this.line.particleSpread + 1) % HoloParticles.spreadCount();
            this.rebuildWidgets();
        }).bounds(rxh, fy3, halfW, 18).build());
        // EditBoxes numericos: Alto (Y) y Lado (X)
        EditBox alto = new EditBox(this.font, lx + 34, this.fy4, halfW - 38, 16, (Component) Component.literal("Alto"));
        alto.setMaxLength(8);
        alto.setValue(fmt(this.line.particleOffY));
        alto.setResponder(v -> {
            try {
                this.line.particleOffY = Float.parseFloat(v.trim());
            } catch (Exception e) {
                // valor incompleto, se ignora
            }
        });
        this.addRenderableWidget(alto);
        EditBox lado = new EditBox(this.font, rxh + 34, this.fy4, halfW - 38, 16, (Component) Component.literal("Lado"));
        lado.setMaxLength(8);
        lado.setValue(fmt(this.line.particleOffX));
        lado.setResponder(v -> {
            try {
                this.line.particleOffX = Float.parseFloat(v.trim());
            } catch (Exception e) {
                // valor incompleto, se ignora
            }
        });
        this.addRenderableWidget(lado);
        this.addRenderableWidget(Button.builder((Component) Component.literal("\u00a7aListo"), b2 -> this.onClose()).bounds(this.leftPos + this.panelWidth / 2 - 45, b - 22, 90, 18).build());
    }

    public void onClose() {
        if (this.onBack != null) {
            this.onBack.run();
        }
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, this.totalRows() - this.rowsVisible);
        int prev = this.scroll;
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(delta)));
        if (prev != this.scroll) {
            this.rebuildWidgets();
        }
        return true;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int r = this.leftPos + this.panelWidth;
        int b = this.topPos + this.panelHeight;
        g.fill(this.leftPos, this.topPos, r, b, -300410848);
        g.fill(this.leftPos, this.topPos, r, this.topPos + 20, -233959916);
        g.fill(this.leftPos, b - 118, r, b - 117, -12947803);
        g.fill(this.leftPos, b - 1, r, b, -12947803);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fParticulas \u00a77(" + HoloParticles.count() + " estilos)", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a78Propiedades \u00a77(Alto/Lado en bloques)", this.leftPos + 12, b - 115, 0xFFFFFF, false);
        int halfW = (this.panelWidth - 24 - 4) / 2;
        int lx = this.leftPos + 12;
        int rxh = lx + halfW + 4;
        g.drawString(this.font, "\u00a77Alto", lx, this.fy4 + 4, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a77Lado", rxh, this.fy4 + 4, 0xFFFFFF, false);
        super.render(g, mx, my, pt);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
