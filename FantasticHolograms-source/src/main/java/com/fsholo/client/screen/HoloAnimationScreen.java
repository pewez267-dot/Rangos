package com.fsholo.client.screen;

import com.fsholo.data.Hologram;
import com.fsholo.util.HoloAnimations;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Editor de animacion del texto del holograma: lista de animaciones a la izquierda,
 * velocidad e intensidad a la derecha. La animacion aplica a todo el holograma.
 */
public class HoloAnimationScreen extends Screen {
    private final Hologram holo;
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
    private int rightX;
    private int rightW;

    public HoloAnimationScreen(Hologram holo, Runnable onBack) {
        super((Component) Component.literal("Animaci\u00f3n del Holograma"));
        this.holo = holo;
        this.onBack = onBack;
    }

    private int visibleRows() {
        return Math.max(1, this.listH / this.rowH);
    }

    private int maxScroll() {
        return Math.max(0, HoloAnimations.count() - this.visibleRows());
    }

    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 420);
        this.panelHeight = Math.min(this.height - 20, 260);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        int b = this.topPos + this.panelHeight;
        this.listX = this.leftPos + 8;
        this.listY = this.topPos + 28;
        this.listW = 150;
        this.listH = this.panelHeight - 28 - 12;
        this.rightX = this.listX + this.listW + 10;
        this.rightW = this.leftPos + this.panelWidth - 8 - this.rightX;
        this.listScroll = Math.max(0, Math.min(this.listScroll, this.maxScroll()));
        int y = this.topPos + 56;
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bVelocidad: \u00a7f" + HoloAnimations.speedName(this.holo.animSpeed))), a -> {
            this.holo.animSpeed = (this.holo.animSpeed + 1) % HoloAnimations.speedCount();
            this.rebuildWidgets();
        }).bounds(this.rightX, y, this.rightW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal((String) ("\u00a7bIntensidad: \u00a7f" + HoloAnimations.intensityName(this.holo.animIntensity))), a -> {
            this.holo.animIntensity = (this.holo.animIntensity + 1) % HoloAnimations.intensityCount();
            this.rebuildWidgets();
        }).bounds(this.rightX, y + 22, this.rightW, 18).build());
        this.addRenderableWidget(Button.builder((Component) Component.literal("\u00a7aListo"), a -> this.onClose()).bounds(this.rightX, b - 26, this.rightW, 18).build());
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
            if (idx >= 0 && idx < HoloAnimations.count()) {
                this.holo.animation = idx;
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
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fAnimaci\u00f3n del Texto", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        g.fill(this.listX, this.listY, this.listX + this.listW, this.listY + this.listH, -1072689128);
        int rows = this.visibleRows();
        for (int i = 0; i < rows; ++i) {
            int idx = this.listScroll + i;
            if (idx >= HoloAnimations.count()) {
                break;
            }
            int ry = this.listY + i * this.rowH;
            boolean sel = idx == this.holo.animation;
            boolean hov = mx >= (double) this.listX && mx < (double) (this.listX + this.listW - 5) && my >= (double) ry && my < (double) (ry + this.rowH);
            if (sel) {
                g.fill(this.listX, ry, this.listX + this.listW - 5, ry + this.rowH, -13800225);
            } else if (hov) {
                g.fill(this.listX, ry, this.listX + this.listW - 5, ry + this.rowH, 0x40FFFFFF);
            }
            String nm = (sel ? "\u00a7a\u25b6 \u00a7f" : "\u00a77") + HoloAnimations.name(idx);
            g.drawString(this.font, nm, this.listX + 4, ry + (this.rowH - 8) / 2, 0xE0E0E0, false);
        }
        if (this.maxScroll() > 0) {
            int sbx = this.listX + this.listW - 4;
            g.fill(sbx, this.listY, sbx + 3, this.listY + this.listH, 0x60000000);
            int thumbH = Math.max(10, this.listH * this.visibleRows() / Math.max(1, HoloAnimations.count()));
            int thumbY = this.listY + (this.listH - thumbH) * this.listScroll / Math.max(1, this.maxScroll());
            g.fill(sbx, thumbY, sbx + 3, thumbY + thumbH, -8355680);
        }
        g.drawString(this.font, "\u00a77Animaci\u00f3n: \u00a7f" + HoloAnimations.name(this.holo.animation), this.rightX, this.topPos + 30, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a78Aplica a todo el holo", this.rightX, this.topPos + 42, 0xFFFFFF, false);
        super.render(g, mx, my, pt);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
