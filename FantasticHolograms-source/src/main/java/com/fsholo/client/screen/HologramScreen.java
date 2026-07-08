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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

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
    private int previewTop;
    private int previewH;
    private int previewScroll = 0;
    private int previewStep = 12;
    private int previewMaxScroll = 0;

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
        int margin = 6;
        int gap = 4;
        this.panelWidth = Math.min(this.width - 20, 480);
        int avail = this.height - margin * 2;
        // El editor necesita ~222px para no solaparse con el footer; el resto es la franja de preview.
        // Damos prioridad al editor y capamos su alto para no dejar hueco enorme en pantallas grandes.
        // El editor necesita ~222px; se lo damos y CAPAMOS su alto para que la franja de preview reciba
        // la mayor parte del espacio libre (asi se ven muchas mas lineas a la vez, no solo 3-4).
        this.panelHeight = Math.max(222, Math.min(236, avail - gap - 90));
        if (this.panelHeight > avail - gap - 24) {
            this.panelHeight = Math.max(180, avail - gap - 24);
        }
        this.previewH = Math.max(40, Math.min(320, avail - this.panelHeight - gap));
        int blockH = this.panelHeight + gap + this.previewH;
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = Math.max(margin, (this.height - blockH) / 2);
        this.previewTop = this.topPos + this.panelHeight + gap;
        this.footerSep = this.topPos + this.panelHeight - 46;
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
        int listTop = this.topPos + 32;
        int opsY = this.footerSep - 20;
        int addY = this.footerSep - 40;
        int listBottom = addY - 4;
        this.maxVisible = Math.max(1, (listBottom - listTop) / 18);
        this.labels.add(new Label("\u00a7b\u00a7lL\u00edneas \u00a77(" + this.holo.lines.size() + ")", lx, this.topPos + 23, 9099519));
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
        int y = this.topPos + 22;
        this.labels.add(new Label("\u00a7fTexto \u00a77(admite &c\u00f3digos)", rx, y, 0xE0E0E0));
        EditBox text = new EditBox(this.font, rx, y + 11, rw, 16, (Component)Component.empty());
        text.setMaxLength(256);
        text.setValue(line.text);
        text.setResponder(s -> {
            line.text = s;
        });
        this.addRenderableWidget(text);
        y += 31;
        this.swatches.add(new int[]{rx, y, 16, HoloColors.parse(line.color, 0xFFFFFF)});
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7bElegir color \u00bb"), b -> this.minecraft.setScreen((Screen)new HoloColorPickerScreen("Color del texto", HoloColors.parse(line.color, 0xFFFFFF), c -> {
            line.color = HoloColors.toHex(c);
        }, this::reopen))).bounds(rx + 20, y, rw - 20, 16).build());
        this.addToggle(rx, y += 19, colW, "Negrita", line.bold, () -> {
            line.bold = !line.bold;
        });
        this.addToggle(rx + colW + 4, y, colW, "Cursiva", line.italic, () -> {
            line.italic = !line.italic;
        });
        this.addToggle(rx, y += 17, colW, "Subrayado", line.underline, () -> {
            line.underline = !line.underline;
        });
        this.addToggle(rx + colW + 4, y, colW, "Tachado", line.strikethrough, () -> {
            line.strikethrough = !line.strikethrough;
        });
        this.addToggle(rx, y += 17, colW, "Ofuscado", line.obfuscated, () -> {
            line.obfuscated = !line.obfuscated;
        });
        this.addToggle(rx + colW + 4, y, colW, "Sombra", line.shadow, () -> {
            line.shadow = !line.shadow;
        });
        this.addToggle(rx, y += 17, colW, "Degradado", line.gradient, () -> {
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
        y += 17;
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
        y += 17;
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
        int sy = this.topPos + this.panelHeight - 44;
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
        if (mouseY >= (double)this.previewTop) {
            // Desplaza el preview de a una linea por muesca y lo limita al maximo real (ultima linea),
            // asi se pueden ver TODAS las lineas por muchas que sean (3 o 100).
            int amount = Math.max(10, this.previewStep);
            this.previewScroll = Math.max(0, Math.min(this.previewMaxScroll, this.previewScroll - (int)Math.signum(delta) * amount));
            return true;
        }
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
        this.renderPreview(g);
    }

    /**
     * Vista previa EN VIVO del holograma completo, en una franja PROPIA debajo del panel (reservada en
     * init, nunca invade el editor ni el footer). Escala el contenido para que TODAS las lineas quepan
     * dentro de la franja (con recorte de seguridad), centradas, con color/estilo/degradado/arcoiris/
     * sombra/fondo reales. Resalta la linea seleccionada. Se actualiza solo.
     */
    private void renderPreview(GuiGraphics g) {
        int x0 = this.leftPos;
        int x1 = this.leftPos + this.panelWidth;
        int y0 = this.previewTop;
        int y1 = this.previewTop + this.previewH;
        g.fill(x0, y0, x1, y1, 0xE0101014);
        g.renderOutline(x0, y0, this.panelWidth, this.previewH, 0x50FFFFFF);
        g.drawString(this.font, "\u00a7d\u2726 \u00a77Vista previa", x0 + 8, y0 + 3, 0xAAAAAA, false);
        List<HoloLine> lines = this.holo.lines;
        if (lines.isEmpty()) {
            return;
        }
        float time = (float)(System.currentTimeMillis() % 3000L) / 3000.0f;
        int n = lines.size();
        int lh = this.font.lineHeight;
        List<Component> comps = new ArrayList<Component>();
        int maxWun = 8;
        for (HoloLine ln : lines) {
            Component c = this.styledComponent(ln, time);
            comps.add(c);
            int w = this.font.width(c);
            if (w > maxWun) {
                maxWun = w;
            }
        }
        int areaTop = y0 + 13;
        int areaBot = y1 - 3;
        int areaH = Math.max(8, areaBot - areaTop);
        int areaW = this.panelWidth - 16;
        float unitH = lh + 2;
        // Escala FIJA (tamano inicial): solo se limita por el ancho del holo y su escala real, NUNCA por la
        // cantidad de lineas. Asi las lineas mantienen su tamano y, si no caben todas, el preview se hace
        // scrolleable (rueda del raton) en vez de encogerse.
        float scaleW = (float)areaW / (float)maxWun;
        float scale = Math.min(scaleW, Math.max(0.5f, Math.min(2.0f, this.holo.scale)));
        scale = Math.max(0.35f, scale);
        int step = Math.max(1, Math.round(unitH * scale));
        int contentH = (n - 1) * step + Math.round((float)lh * scale);
        int totalH = n * step;
        int maxScroll = Math.max(0, totalH - areaH);
        this.previewStep = step;
        this.previewMaxScroll = maxScroll;
        this.previewScroll = Math.max(0, Math.min(this.previewScroll, maxScroll));
        int cx = this.leftPos + this.panelWidth / 2;
        // Si todo cabe, centrado; si no, se ancla arriba y se desplaza con el scroll.
        int startY = maxScroll <= 0 ? areaTop + Math.max(0, (areaH - totalH) / 2) : areaTop - this.previewScroll;
        int bgAlpha = (int)(Math.max(0.0f, Math.min(1.0f, this.holo.background)) * 255.0f);
        int halfBg = Math.min(areaW / 2, Math.round((float)maxWun * scale) / 2 + 3);
        g.enableScissor(x0 + 1, areaTop, x1 - 1, areaBot);
        if (bgAlpha > 0) {
            g.fill(cx - halfBg, startY - 1, cx + halfBg, startY + contentH + 1, bgAlpha << 24);
        }
        for (int i = 0; i < n; ++i) {
            int lineY = startY + i * step;
            if (lineY + step < areaTop || lineY > areaBot) {
                continue;
            }
            Component c = comps.get(i);
            int wUn = this.font.width(c);
            if (i == this.selected) {
                int hlW = Math.round((float)wUn * scale);
                g.fill(cx - hlW / 2 - 2, lineY - 1, cx + hlW / 2 + 2, lineY + Math.round((float)lh * scale) + 1, 0x33FFEE55);
            }
            g.pose().pushPose();
            g.pose().translate((float)cx, (float)lineY, 0.0f);
            g.pose().scale(scale, scale, 1.0f);
            g.drawString(this.font, c, -wUn / 2, 0, 0xFFFFFF, lines.get(i).shadow);
            g.pose().popPose();
        }
        g.disableScissor();
        // Barra de scroll a la derecha cuando hay mas lineas de las que caben en la franja.
        if (maxScroll > 0) {
            int barX = x1 - 4;
            int barH = Math.max(12, areaH * areaH / totalH);
            int barY = areaTop + (areaH - barH) * this.previewScroll / maxScroll;
            g.fill(barX, areaTop, barX + 2, areaBot, 0x40FFFFFF);
            g.fill(barX, barY, barX + 2, barY + barH, 0xC0FFEE55);
        }
    }

    /** Construye el Component estilizado de una linea (color solido o degradado/arcoiris por letra). */
    private Component styledComponent(HoloLine ln, float time) {
        Style base = Style.EMPTY.withBold(Boolean.valueOf(ln.bold)).withItalic(Boolean.valueOf(ln.italic)).withUnderlined(Boolean.valueOf(ln.underline)).withStrikethrough(Boolean.valueOf(ln.strikethrough)).withObfuscated(Boolean.valueOf(ln.obfuscated));
        if (ln.gradient || ln.rainbow) {
            String txt = HoloColors.strip(ln.text);
            if (txt.isEmpty()) {
                txt = " ";
            }
            MutableComponent out = Component.empty();
            int len = txt.length();
            int from = HoloColors.parse(ln.gradFrom, 0xFF5555);
            int to = HoloColors.parse(ln.gradTo, 0x55AAFF);
            for (int i = 0; i < len; ++i) {
                int color = ln.rainbow
                        ? HoloColors.rainbowColor(ln.rainbowStyle, (float)i / (float)Math.max(1, len), time)
                        : HoloColors.lerp(from, to, len <= 1 ? 0.0f : (float)i / (float)(len - 1));
                out.append((Component)Component.literal((String)String.valueOf(txt.charAt(i))).withStyle(base.withColor(TextColor.fromRgb((int)color))));
            }
            return out;
        }
        int color = HoloColors.parse(ln.color, 0xFFFFFF);
        return Component.literal((String)HoloColors.amp(ln.text)).withStyle(base.withColor(TextColor.fromRgb((int)color)));
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
