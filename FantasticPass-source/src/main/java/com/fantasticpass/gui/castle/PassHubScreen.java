/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.castle.CastleScreen;
import com.fantasticpass.gui.castle.PassQuestOverviewScreen;
import com.fantasticpass.gui.player.PassViewScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PassHubScreen
extends CastleScreen {
    private static final int[] REWARDS = new int[]{0, 0, 2, 1};
    private static final int[] PASS = new int[]{3, 0, 5, 1};
    private static final int[] QUESTS = new int[]{6, 0, 8, 1};
    private static final int[] INFO = new int[]{3, 2, 5, 2};
    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int pointsPerTier;
    private float pulse;

    public PassHubScreen(PassDefinition pass, PlayerPassData data, int pointsPerTier) {
        super((Component)Component.translatable((String)"fantasticpass.gui.view.title"), null, PassHubScreen.castle("battlepass_main"), 22, 9, 0, 247, 103);
        this.pass = pass;
        this.data = data;
        this.pointsPerTier = pointsPerTier;
    }

    @Override
    protected void initControls() {
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.pulse += partialTick * 0.1f;
        this.drawCastleBackground(g);
        if (PassHubScreen.isPeek()) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        List<Component> tip = null;
        tip = this.hoverBlock(g, REWARDS, mouseX, mouseY) ? this.tip("fantasticpass.gui.rewards", "fantasticpass.hub.rewards_desc") : tip;
        tip = this.hoverBlock(g, PASS, mouseX, mouseY) ? this.passTip() : tip;
        tip = this.hoverBlock(g, QUESTS, mouseX, mouseY) ? this.tip("fantasticpass.gui.quests", "fantasticpass.hub.quests_desc") : tip;
        tip = this.hoverBlock(g, INFO, mouseX, mouseY) ? this.tip("fantasticpass.gui.info", "fantasticpass.hub.info_desc") : tip;
        super.render(g, mouseX, mouseY, partialTick);
        if (tip != null) {
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    private List<Component> tip(String titleKey, String descKey) {
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)Component.translatable((String)titleKey).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
        l.add((Component)Component.translatable((String)descKey).withStyle(ChatFormatting.GRAY));
        return l;
    }

    private List<Component> passTip() {
        ArrayList<Component> l = new ArrayList<Component>();
        String name = this.pass.getName() != null && !this.pass.getName().isEmpty() ? this.pass.getName() : "Fantastic Pass";
        l.add((Component)Component.literal((String)name).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.level", (Object[])new Object[]{this.data.getCurrentTier()}).withStyle(ChatFormatting.YELLOW));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.premium").append(": ").append((Component)(this.data.isPremium() ? Component.literal((String)"\u2714").withStyle(ChatFormatting.GREEN) : Component.literal((String)"\u2715").withStyle(ChatFormatting.RED))));
        if (!this.data.isPremium()) {
            l.add((Component)Component.translatable((String)"fantasticpass.gui.premium_hint").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
        }
        return l;
    }

    private boolean hoverBlock(GuiGraphics g, int[] b, int mouseX, int mouseY) {
        boolean hover;
        int x0 = this.slotX(b[0]);
        int y0 = this.slotY(b[1]);
        int x1 = this.slotX(b[2]) + this.slotPx();
        int y1 = this.slotY(b[3]) + this.slotPx();
        boolean bl = hover = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
        if (hover) {
            float p = 0.12f + 0.06f * (float)Math.abs(Math.sin(this.pulse));
            g.fill(x0, y0, x1, y1, (int)(p * 255.0f) << 24 | 0xFFFFFF);
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, -8054);
        }
        return hover;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (PassHubScreen.isPeek()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (this.in(REWARDS, mouseX, mouseY)) {
                this.open(new PassViewScreen(this, this.pass, this.data, this.pointsPerTier, false));
                return true;
            }
            if (this.in(PASS, mouseX, mouseY)) {
                if (this.data.isPremium()) {
                    this.open(new PassViewScreen(this, this.pass, this.data, this.pointsPerTier, true));
                } else {
                    this.playDenied();
                }
                return true;
            }
            if (this.in(QUESTS, mouseX, mouseY)) {
                this.open(new PassQuestOverviewScreen(this, this.pass, this.data, this.pointsPerTier));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean in(int[] b, double mx, double my) {
        return mx >= (double)this.slotX(b[0]) && mx < (double)(this.slotX(b[2]) + this.slotPx()) && my >= (double)this.slotY(b[1]) && my < (double)(this.slotY(b[3]) + this.slotPx());
    }

    private void open(CastleScreen screen) {
        this.playClick(1.0f);
        Minecraft.getInstance().setScreen((Screen)screen);
    }
}

