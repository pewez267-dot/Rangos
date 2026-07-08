/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.widgets;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.nametag.NametagBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

public class NametagPreviewWidget
extends AbstractWidget {
    private NametagStyle style = new NametagStyle();
    private String rankText = "";
    private int level = 1;
    private String playerName = "Player";
    // Si es true (modo rango), la 2a linea muestra "Lvl N " + texto estilizado.
    // Si es false (modo editor de nivel), muestra SOLO el texto estilizado (evita "Lvl 100 Nivel 100").
    private boolean showLevelPrefix = true;

    public void setShowLevelPrefix(boolean show) {
        this.showLevelPrefix = show;
    }

    public NametagPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, (Component)Component.literal((String)"Preview"));
    }

    public void setStyle(NametagStyle style) {
        this.style = style == null ? new NametagStyle() : style;
    }

    public void setRankText(String rankText) {
        this.rankText = rankText == null ? "" : rankText;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName != null && !playerName.isEmpty() ? playerName : "Player";
    }

    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawAccentPanel(graphics, this.getX(), this.getY(), this.width, this.height, 58879);
        graphics.fill(this.getX() + 4, this.getY() + 4, this.getX() + this.width - 4, this.getY() + this.height - 4, -1072689128);
        Minecraft mc = Minecraft.getInstance();
        int centerX = this.getX() + this.width / 2;
        int nameWidth = mc.font.width(this.playerName);
        graphics.drawString(mc.font, this.playerName, centerX - nameWidth / 2, this.getY() + this.height / 2 - 12, -1, false);
        MutableComponent line = this.showLevelPrefix
            ? Component.literal((String)("Lvl " + this.level + " ")).append((Component)NametagBuilder.buildStyledText(this.rankText, this.style))
            : NametagBuilder.buildStyledText(this.rankText, this.style);
        int lineWidth = mc.font.width((FormattedText)line);
        graphics.drawString(mc.font, (Component)line, centerX - lineWidth / 2, this.getY() + this.height / 2 + 2, -5592406, false);
    }

    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}

