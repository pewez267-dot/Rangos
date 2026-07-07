/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 */
package com.fsholo.client.screen;

import com.fsholo.client.screen.HoloColorPickerScreen;
import com.fsholo.data.HoloLine;
import com.fsholo.data.Hologram;
import com.fsholo.net.FSHoloNetwork;
import com.fsholo.net.SaveHoloPacket;
import com.fsholo.util.HoloColors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HologramScreen
extends Screen {
    private final Hologram holo;
    private int selected = 0;
    private int listOffset = 0;
    private final List<Label> labels = new ArrayList<Label>();
    private final List<int[]> swatches = new ArrayList<int[]>();
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int maxVisible = 6;
    private int footerSep;

    public HologramScreen(Hologram holo) {
        super((Component)Component.literal((String)"Fantastic Holograms"));
        this.holo = holo;
        if (this.holo.lines.isEmpty()) {
            this.holo.lines.add(new HoloLine("Texto"));
        }
    }

    public HologramScreen(Hologram holo, int selected) {
        this(holo);
        this.selected = selected;
    }

    private void reopen() {
        this.minecraft.setScreen((Screen)new HologramScreen(this.holo, this.selected));
    }

    private HoloLine sel() {
        this.selected = Math.max(0, Math.min(this.selected, this.holo.lines.size() - 1));
        return this.holo.lines.get(this.selected);
    }

    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 480);
        this.panelHeight = Math.min(this.height - 20, 300);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.footerSep = this.topPos + this.panelHeight - 52;
        this.labels.clear();
        this.swatches.clear();
        this.buildLineList();
        this.buildEditor();
        this.buildFooter();
    }

    private void buildLineList() {
        int i;
        int lx = this.leftPos + 8;
        int lw = 132;
        int listTop = this.topPos + 34;
        int opsY = this.footerSep - 22;
        int addY = this.footerSep - 42;
        int listBottom = addY - 4;
        this.maxVisible = Math.max(1, (listBottom - listTop) / 18);
        this.labels.add(new Label("\u00a7b\u00a7lL\u00edneas \u00a77(" + this.holo.lines.size() + ")", lx, this.topPos + 24, 9099519));
        int count = this.holo.lines.size();
        this.listOffset = Math.max(0, Math.min(this.listOffset, Math.max(0, count - this.maxVisible)));
        for (int row = 0; row < this.maxVisible && (i = this.listOffset + row) < count; ++row) {
            boolean isSel = i == this.selected;
            String preview = this.font.plainSubstrByWidth(HoloColors.strip(this.holo.lines.get((int)i).text), lw - 22);
            if (preview.isEmpty()) {
                preview = "(vac\u00edo)";
            }
            int index = i;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)((isSel ? "\u00a7e\u25b6 " : "\u00a7f") + (i + 1) + ". \u00a77" + preview)), b -> {
                this.selected = index;
                this.rebuildWidgets();
            }).bounds(lx, listTop + row * 18, lw, 16).build());
        }
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7a+ A\u00f1adir l\u00ednea"), b -> {
            this.holo.lines.add(new HoloLine("Nueva l\u00ednea"));
            this.selected = this.holo.lines.size() - 1;
            this.listOffset = Integer.MAX_VALUE;
            this.rebuildWidgets();
        }).bounds(lx, addY, lw, 16).build());
        int ow = (lw - 8) / 3;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7bSubir"), b -> this.moveLine(-1)).bounds(lx, opsY, ow, 16).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7bBajar"), b -> this.moveLine(1)).bounds(lx + ow + 4, opsY, ow, 16).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7cBorrar"), b -> {
            if (this.holo.lines.size() > 1) {
                this.holo.lines.remove(this.selected);
                this.selected = Math.max(0, this.selected - 1);
            }
            this.rebuildWidgets();
        }).bounds(lx + 2 * (ow + 4), opsY, ow, 16).build());
    }

    private void buildEditor() {
        HoloLine line = this.sel();
        int rx = this.leftPos + 156;
        int rw = this.panelWidth - 156 - 8;
        int colW = (rw - 4) / 2;
        int y = this.topPos + 24;
        this.labels.add(new Label("\u00a7fTexto \u00a77(admite &c\u00f3digos)", rx, y, 0xE0E0E0));
        EditBox text = new EditBox(this.font, rx, y + 11, rw, 16, (Component)Component.empty());
        text.setMaxLength(256);
        text.setValue(line.text);
        text.setResponder(s -> {
            line.text = s;
        });
        this.addRenderableWidget(text);
        this.labels.add(new Label("\u00a7fColor", rx, y += 32, 0xE0E0E0));
        this.swatches.add(new int[]{rx, y + 11, 16, HoloColors.parse(line.color, 0xFFFFFF)});
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7bElegir color \u00bb"), b -> this.minecraft.setScreen((Screen)new HoloColorPickerScreen("Color del texto", HoloColors.parse(line.color, 0xFFFFFF), c -> {
            line.color = HoloColors.toHex(c);
        }, this::reopen))).bounds(rx + 20, y + 11, rw - 20, 16).build());
        this.addToggle(rx, y += 32, colW, "Negrita", line.bold, () -> {
            line.bold = !line.bold;
        });
        this.addToggle(rx + colW + 4, y, colW, "Cursiva", line.italic, () -> {
            line.italic = !line.italic;
        });
        this.addToggle(rx, y += 18, colW, "Subrayado", line.underline, () -> {
            line.underline = !line.underline;
        });
        this.addToggle(rx + colW + 4, y, colW, "Tachado", line.strikethrough, () -> {
            line.strikethrough = !line.strikethrough;
        });
        this.addToggle(rx, y += 18, colW, "Ofuscado", line.obfuscated, () -> {
            line.obfuscated = !line.obfuscated;
        });
        this.addToggle(rx + colW + 4, y, colW, "Sombra", line.shadow, () -> {
            line.shadow = !line.shadow;
        });
        this.addToggle(rx, y += 20, colW, "Degradado", line.gradient, () -> {
            boolean bl = line.gradient = !line.gradient;
            if (line.gradient) {
                line.rainbow = false;
            }
        });
        this.addToggle(rx + colW + 4, y, colW, "Arco\u00edris", line.rainbow, () -> {
            boolean bl = line.rainbow = !line.rainbow;
            if (line.rainbow) {
                line.gradient = false;
            }
        });
        y += 18;
        if (line.rainbow) {
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)("\u00a7dArco\u00edris: \u00a7f" + HoloColors.rainbowStyleName(line.rainbowStyle) + " \u00bb")), b -> {
                this.minecraft.setScreen((Screen)new HoloRainbowPickerScreen(line.rainbowStyle, s -> {
                    line.rainbowStyle = s;
                }, this::reopen));
            }).bounds(rx, y, rw, 16).build());
        } else {
            this.swatches.add(new int[]{rx, y, 16, HoloColors.parse(line.gradFrom, 0xFF5555)});
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a77Inicio \u00bb"), b -> this.minecraft.setScreen((Screen)new HoloColorPickerScreen("Degradado: inicio", HoloColors.parse(line.gradFrom, 0xFF5555), c -> {
                line.gradFrom = HoloColors.toHex(c);
                line.gradient = true;
            }, this::reopen))).bounds(rx + 20, y, colW - 20, 16).build());
            this.swatches.add(new int[]{rx + colW + 4, y, 16, HoloColors.parse(line.gradTo, 0x55AAFF)});
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a77Final \u00bb"), b -> this.minecraft.setScreen((Screen)new HoloColorPickerScreen("Degradado: final", HoloColors.parse(line.gradTo, 0x55AAFF), c -> {
                line.gradTo = HoloColors.toHex(c);
                line.gradient = true;
            }, this::reopen))).bounds(rx + colW + 4 + 20, y, colW - 20, 16).build());
        }
        y += 20;
        this.addToggle(rx, y, colW, "Part\u00edculas", line.particles, () -> {
            line.particles = !line.particles;
        });
        if (line.particles) {
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)("\u00a7d\u2726 \u00a7f" + com.fsholo.util.HoloParticles.name(line.particleStyle) + " \u00bb")), b -> {
                this.minecraft.setScreen((Screen)new HoloParticlePickerScreen(line, this::reopen));
            }).bounds(rx + colW + 4, y, colW, 16).build());
        }
    }

    private void buildFooter() {
        int sy = this.topPos + this.panelHeight - 46;
        int x = this.leftPos + 8;
        this.labels.add(new Label("\u00a7fAltura", x + 2, sy + 4, 0xE0E0E0));
        this.addField(x + 40, sy, 34, HologramScreen.fmt(this.holo.yOffset), s -> {
            this.holo.yOffset = HologramScreen.parse(s, this.holo.yOffset);
        });
        this.labels.add(new Label("\u00a7fEspaciado", x + 92, sy + 4, 0xE0E0E0));
        this.addField(x + 150, sy, 34, HologramScreen.fmt(this.holo.lineSpacing), s -> {
            this.holo.lineSpacing = Math.max(0.05, HologramScreen.parse(s, this.holo.lineSpacing));
        });
        this.labels.add(new Label("\u00a7fEscala", x + 196, sy + 4, 0xE0E0E0));
        this.addField(x + 238, sy, 34, HologramScreen.fmt(this.holo.scale), s -> {
            this.holo.scale = (float)Math.max(0.1, Math.min(8.0, HologramScreen.parse(s, this.holo.scale)));
        });
        this.labels.add(new Label("\u00a7fFondo", x + 286, sy + 4, 0xE0E0E0));
        this.addField(x + 326, sy, 34, String.valueOf(Math.round(this.holo.background * 100.0f)), s -> {
            double p = HologramScreen.parse(s, (double)this.holo.background * 100.0);
            this.holo.background = (float)Math.max(0.0, Math.min(1.0, p / 100.0));
        });
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Cerrar"), b -> this.onClose()).bounds(this.leftPos + 8, this.topPos + this.panelHeight - 22, 90, 18).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)("\u00a7d\u2726 Animaci\u00f3n: \u00a7f" + com.fsholo.util.HoloAnimations.name(this.holo.animation))), b -> {
            this.minecraft.setScreen((Screen)new HoloAnimationScreen(this.holo, this::reopen));
        }).bounds(this.leftPos + 104, this.topPos + this.panelHeight - 22, this.panelWidth - 238, 18).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7a\u00a7lGuardar"), b -> {
            FSHoloNetwork.sendToServer(new SaveHoloPacket(this.holo));
            this.onClose();
        }).bounds(this.leftPos + this.panelWidth - 130, this.topPos + this.panelHeight - 22, 122, 18).build());
    }

    private void moveLine(int dir) {
        int ni = this.selected + dir;
        if (ni >= 0 && ni < this.holo.lines.size()) {
            HoloLine l = this.holo.lines.remove(this.selected);
            this.holo.lines.add(ni, l);
            this.selected = ni;
            this.rebuildWidgets();
        }
    }

    private void addToggle(int x, int y, int w, String text, boolean state, Runnable onToggle) {
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)((state ? "\u00a7a\u2714 \u00a7f" : "\u00a78\u2718 \u00a77") + text)), b -> {
            onToggle.run();
            this.rebuildWidgets();
        }).bounds(x, y, w, 16).build());
    }

    private void addField(int x, int y, int w, String value, Consumer<String> setter) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(8);
        box.setValue(value);
        box.setResponder(s -> {
            if (!s.trim().isEmpty()) {
                setter.accept(s.trim());
            }
        });
        this.addRenderableWidget(box);
    }

    private static double parse(String s, double def) {
        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long)v) : String.format(Locale.US, "%.2f", v);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < (double)(this.leftPos + 150)) {
            int max = Math.max(0, this.holo.lines.size() - this.maxVisible);
            this.listOffset = Math.max(0, Math.min(max, this.listOffset - (int)Math.signum(delta)));
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int r = this.leftPos + this.panelWidth;
        int b = this.topPos + this.panelHeight;
        g.fill(this.leftPos, this.topPos, r, b, -300410848);
        g.fill(this.leftPos, this.topPos, r, this.topPos + 20, -233959916);
        g.fill(this.leftPos, b - 1, r, b, -12947803);
        g.fill(this.leftPos + 148, this.topPos + 22, this.leftPos + 149, this.footerSep, 0x35FFFFFF);
        g.fill(this.leftPos + 6, this.footerSep, r - 6, this.footerSep + 1, 0x35FFFFFF);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Holograms \u00a7d\u2726  \u00a78" + this.holo.id, this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        super.render(g, mouseX, mouseY, partialTick);
        for (int[] s : this.swatches) {
            g.fill(s[0], s[1], s[0] + s[2], s[1] + s[2], 0xFF000000 | s[3]);
            g.renderOutline(s[0], s[1], s[2], s[2], -16777216);
        }
        for (Label l : this.labels) {
            g.drawString(this.font, l.text, l.x, l.y, l.color, false);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static final class Label {
        final String text;
        final int x;
        final int y;
        final int color;

        Label(String text, int x, int y, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
}

