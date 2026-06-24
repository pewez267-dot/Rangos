package com.fantasticranks.gui.widgets;

import com.fantasticranks.data.NametagStyle;
import com.fantasticranks.gui.GuiTheme;
import com.fantasticranks.nametag.NametagBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Live preview of the resulting nametag: the player's name in white with the styled rank
 * line beneath it, exactly as it will render in-world. Re-reads its style/text every frame
 * so it updates on every keystroke and color change.
 */
public class NametagPreviewWidget extends AbstractWidget {

    private NametagStyle style = new NametagStyle();
    private String rankText = "";
    private int level = 1;
    private String playerName = "Player";

    public NametagPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Preview"));
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
        this.playerName = (playerName == null || playerName.isEmpty()) ? "Player" : playerName;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawAccentPanel(graphics, getX(), getY(), this.width, this.height, GuiTheme.ACCENT_CYAN);
        graphics.fill(getX() + 4, getY() + 4, getX() + this.width - 4, getY() + this.height - 4, 0xC0101018);

        Minecraft mc = Minecraft.getInstance();
        int centerX = getX() + this.width / 2;

        int nameWidth = mc.font.width(playerName);
        graphics.drawString(mc.font, playerName, centerX - nameWidth / 2, getY() + this.height / 2 - 12,
                0xFFFFFFFF, false);

        MutableComponent line = Component.literal("Lvl " + level + " ")
                .append(NametagBuilder.buildStyledText(rankText, style));
        int lineWidth = mc.font.width(line);
        graphics.drawString(mc.font, line, centerX - lineWidth / 2, getY() + this.height / 2 + 2,
                0xFFAAAAAA, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
