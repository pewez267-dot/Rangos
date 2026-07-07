/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.castle.CastleScreen;
import com.fantasticpass.quest.Quest;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class PassWeekScreen
extends CastleScreen {
    private static final int NAV_ROW = 4;
    private static final int NAV_PREV = 3;
    private static final int NAV_INFO = 4;
    private static final int NAV_NEXT = 5;
    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int week;
    private int page;

    public PassWeekScreen(Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier, int week) {
        super((Component)Component.translatable((String)"fantasticpass.gui.week", (Object[])new Object[]{week}), parent, PassWeekScreen.castle("battlepass_quest_overview"), 43, 9, 20, 247, 160);
        this.pass = pass;
        this.data = data;
        this.week = week;
    }

    @Override
    protected void initControls() {
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean claimed;
        int progress;
        Quest q;
        int col;
        int i;
        this.drawCastleBackground(g);
        if (PassWeekScreen.isPeek()) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        List<Quest> freeQuests = this.pass.weekFreeQuests(this.week);
        List<Quest> premiumQuests = this.pass.weekPremiumQuests(this.week);
        boolean premium = this.data.isPremium();
        int pages = this.pageCount();
        if (this.page >= pages) {
            this.page = pages - 1;
        }
        int base = this.page * 5;
        List<Component> tooltip = null;
        for (i = 0; i < 5 && base + i < freeQuests.size(); ++i) {
            col = 2 + i;
            q = freeQuests.get(base + i);
            progress = this.data.getQuestProgress(q.getId());
            claimed = this.data.isQuestClaimed(q.getId());
            this.drawQuestSlot(g, q, col, 1, progress, claimed);
            if (!this.overSlot(mouseX, mouseY, col, 1)) continue;
            tooltip = this.questTooltip(q, progress, claimed);
        }
        for (i = 0; i < 5 && base + i < premiumQuests.size(); ++i) {
            col = 2 + i;
            q = premiumQuests.get(base + i);
            if (premium) {
                progress = this.data.getQuestProgress(q.getId());
                claimed = this.data.isQuestClaimed(q.getId());
                this.drawQuestSlot(g, q, col, 2, progress, claimed);
                if (!this.overSlot(mouseX, mouseY, col, 2)) continue;
                tooltip = this.questTooltip(q, progress, claimed);
                continue;
            }
            this.drawQuestSlotLocked(g, col, 2);
            if (!this.overSlot(mouseX, mouseY, col, 2)) continue;
            tooltip = this.questLockedTooltip(q);
        }
        boolean multi = pages > 1;
        this.drawIcon(g, PassWeekScreen.icon(6), 3, 4);
        this.drawIcon(g, PassWeekScreen.icon(1), 4, 4);
        if (multi) {
            this.drawIcon(g, PassWeekScreen.icon(7), 5, 4);
        }
        if (this.overSlot(mouseX, mouseY, 3, 4)) {
            this.hover(g, 3);
            tooltip = List.of(Component.translatable((String)"fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
        } else if (this.overSlot(mouseX, mouseY, 4, 4)) {
            this.hover(g, 4);
            tooltip = List.of(Component.translatable((String)"fantasticpass.gui.week", (Object[])new Object[]{this.week}).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), Component.translatable((String)"fantasticpass.hub.quests_desc").withStyle(ChatFormatting.GRAY));
        } else if (multi && this.overSlot(mouseX, mouseY, 5, 4)) {
            this.hover(g, 5);
            tooltip = List.of(Component.translatable((String)"fantasticpass.gui.next").withStyle(ChatFormatting.YELLOW));
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (tooltip != null) {
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void hover(GuiGraphics g, int col) {
        int x = this.slotX(col);
        int y = this.slotY(4);
        g.fill(x, y, x + this.slotPx(), y + this.slotPx(), 0x33FFFFFF);
    }

    private int pageCount() {
        int total = Math.max(this.pass.weekFreeQuests(this.week).size(), this.pass.weekPremiumQuests(this.week).size());
        return CastleScreen.pageCount(5, total);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (PassWeekScreen.isPeek()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (this.overSlot(mouseX, mouseY, 3, 4)) {
                this.playClick(0.9f);
                if (this.page <= 0) {
                    this.onClose();
                } else {
                    --this.page;
                }
                return true;
            }
            if (this.pageCount() > 1 && this.overSlot(mouseX, mouseY, 5, 4)) {
                this.page = Mth.clamp((int)(this.page + 1), (int)0, (int)(this.pageCount() - 1));
                this.playClick(1.1f);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

