package com.fsholo.client.screen;

import com.fsholo.data.HoloLine;
import com.fsholo.util.HoloParticles;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Editor completo de particulas estilo FSCrates: lista de TIPOS a la izquierda,
 * configuracion a la derecha (movimiento, posicion, offsets numericos alto/lado,
 * densidad, velocidad, tamano, dispersion).
 */
public class HoloParticlePickerScreen extends Screen {
    private final HoloLine line;
    private final Runnable onBack;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private final int rowH = 16;
    private int listScroll;
    private int editY;
    private int rightX;
    private int rightW;

    public HoloParticlePickerScreen(HoloLine line, Runnable onBack) {
        super((Component) Component.literal("Editor de Part\u00edculas"));
        this.line = line;
        this.onBack = onBack;
    }

    private int visibleRows() {
        return Math.max(1, this.listH / this.rowH);
    }

    private int maxScroll() {
        return Math.max(0, HoloParticles.count() - this.visibleRows());
    }

    private static String fmt(float v) {
        if (v == (float) ((int) v)) {
            return Integer.toString((int) v);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", Float.valueOf(v));
    }

    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 440);
        this.panelHeight = Math.min(this.height - 20, 300);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        int b = this.topPos + this.panelHeight;
        this.listX = this.leftPos + 8;
        this.listY = this.topPos + 28;
        this.listW = 150;
        this.listH = this.panelHeight - 28 - 30;
        this.rightX = this.listX + this.listW + 10;
        this.rightW = this.leftPos + this.panelWidth - 8 - this.rightX;
        this.listScroll = Math.max(0, Math.min(this.listScroll, this.maxScroll()));

        int hx = this.rightX;
        int half = (this.rightW - 4) / 2;
        int rx2 = hx + half + 4;
        int y = this.topPos + 44;
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bMovimiento: \u00a7f" + HoloParticles.movementName(this.line.particleMovement))), a -> {
            this.line.particleMovement = (this.line.particleMovement + 1) % HoloParticles.movementCount();
            this.rebuildWidgets();
        }).bounds(hx, y, this.rightW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bPosici\u00f3n: \u00a7f" + HoloParticles.anchorName(this.line.particleAnchor))), a -> {
            this.line.particleAnchor = (this.line.particleAnchor + 1) % HoloParticles.anchorCount();
            this.rebuildWidgets();
        }).bounds(hx, y + 20, this.rightW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bDensidad: \u00a7f" + Math.max(1, Math.min(4, this.line.particleDensity)))), a -> {
            this.line.particleDensity = this.line.particleDensity >= 4 ? 1 : this.line.particleDensity + 1;
            this.rebuildWidgets();
        }).bounds(hx, y + 40, half, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bVelocidad: \u00a7f" + HoloParticles.speedName(this.line.particleSpeed))), a -> {
            this.line.particleSpeed = (this.line.particleSpeed + 1) % HoloParticles.speedCount();
            this.rebuildWidgets();
        }).bounds(rx2, y + 40, half, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bTama\u00f1o: \u00a7f" + HoloParticles.sizeName(this.line.particleSize))), a -> {
            this.line.particleSize = (this.line.particleSize + 1) % HoloParticles.sizeCount();
            this.rebuildWidgets();
        }).bounds(hx, y + 60, half, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bDispersi\u00f3n: \u00a7f" + HoloParticles.spreadName(this.line.particleSpread))), a -> {
            this.line.particleSpread = (this.line.particleSpread + 1) % HoloParticles.spreadCount();
            this.rebuildWidgets();
        }).bounds(rx2, y + 60, half, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bRitmo: \u00a7f" + HoloParticles.rateName(this.line.particleRate))), a -> {
            this.line.particleRate = (this.line.particleRate + 1) % HoloParticles.rateCount();
            this.rebuildWidgets();
        }).bounds(hx, y + 80, this.rightW, 18).build());
        this.editY = y + 104;
        EditBox alto = new EditBox(this.font, hx + 30, this.editY, half - 34, 16, (Component) Component.literal("Alto"));
        alto.setMaxLength(8);
        alto.setValue(fmt(this.line.particleOffY));
        alto.setResponder(v -> {
            try {
                this.line.particleOffY = Float.parseFloat(v.trim());
            } catch (Exception e) {
                // valor incompleto
            }
        });
        this.addRenderableWidget(alto);
        EditBox lado = new EditBox(this.font, rx2 + 30, this.editY, half - 34, 16, (Component) Component.literal("Lado"));
        lado.setMaxLength(8);
        lado.setValue(fmt(this.line.particleOffX));
        lado.setResponder(v -> {
            try {
                this.line.particleOffX = Float.parseFloat(v.trim());
            } catch (Exception e) {
                // valor incompleto
            }
        });
        this.addRenderableWidget(lado);
        this.addRenderableWidget(Button.builder((Component) Component.literal("\u00a7aListo"), a -> this.onClose()).bounds(hx, b - 24, this.rightW, 18).build());
    }

    public void onClose() {
        if (this.onBack != null) {
            this.onBack.run();
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button == 0 && mx >= (double) this.listX && mx < (double) (this.listX + this.listW) && my >= (double) this.listY && my < (double) (this.listY + this.listH)) {
            int row = (int) ((my - (double) this.listY) / (double) this.rowH);
            int idx = this.listScroll + row;
            if (idx >= 0 && idx < HoloParticles.count()) {
                this.line.particleStyle = idx;
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= (double) this.listX && mx < (double) (this.listX + this.listW) && my >= (double) this.listY && my < (double) (this.listY + this.listH)) {
            this.listScroll = Math.max(0, Math.min(this.maxScroll(), this.listScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int r = this.leftPos + this.panelWidth;
        int b = this.topPos + this.panelHeight;
        g.fill(this.leftPos, this.topPos, r, b, -300410848);
        g.fill(this.leftPos, this.topPos, r, this.topPos + 20, -233959916);
        g.fill(this.leftPos, b - 1, r, b, -12947803);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fEditor de Part\u00edculas \u00a77(" + HoloParticles.count() + " tipos)", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        // Lista izquierda
        g.fill(this.listX, this.listY, this.listX + this.listW, this.listY + this.listH, -1072689128);
        int rows = this.visibleRows();
        for (int i = 0; i < rows; ++i) {
            int idx = this.listScroll + i;
            if (idx >= HoloParticles.count()) {
                break;
            }
            int ry = this.listY + i * this.rowH;
            boolean sel = idx == this.line.particleStyle;
            boolean hov = mx >= (double) this.listX && mx < (double) (this.listX + this.listW - 5) && my >= (double) ry && my < (double) (ry + this.rowH);
            if (sel) {
                g.fill(this.listX, ry, this.listX + this.listW - 5, ry + this.rowH, -13800225);
            } else if (hov) {
                g.fill(this.listX, ry, this.listX + this.listW - 5, ry + this.rowH, 0x40FFFFFF);
            }
            String nm = (sel ? "\u00a7a\u25b6 \u00a7f" : "\u00a77") + HoloParticles.name(idx);
            String trimmed = this.font.plainSubstrByWidth(nm, this.listW - 12);
            g.drawString(this.font, trimmed, this.listX + 4, ry + (this.rowH - 8) / 2, 0xE0E0E0, false);
        }
        if (this.maxScroll() > 0) {
            int sbx = this.listX + this.listW - 4;
            g.fill(sbx, this.listY, sbx + 3, this.listY + this.listH, 0x60000000);
            int thumbH = Math.max(10, this.listH * this.visibleRows() / Math.max(1, HoloParticles.count()));
            int thumbY = this.listY + (this.listH - thumbH) * this.listScroll / Math.max(1, this.maxScroll());
            g.fill(sbx, thumbY, sbx + 3, thumbY + thumbH, -8355680);
        }
        // Cabecera panel derecho
        g.drawString(this.font, "\u00a77Tipo: \u00a7f" + HoloParticles.name(this.line.particleStyle), this.rightX, this.topPos + 30, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a77Alto", this.rightX, this.editY + 4, 0xFFFFFF, false);
        int half = (this.rightW - 4) / 2;
        g.drawString(this.font, "\u00a77Lado", this.rightX + half + 4, this.editY + 4, 0xFFFFFF, false);
        super.render(g, mx, my, pt);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
