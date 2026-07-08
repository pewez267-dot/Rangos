package com.fantasticpass.gui.castle;

import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.castle.CastleScreen;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PassDailyQuestsScreen
extends CastleScreen {
    private static final int NAV_ROW = 4;
    private static final int NAV_PREV = 3;
    private static final int NAV_INFO = 4;
    private static final int NAV_NEXT = 5;
    private static final int PER_PAGE = 5;
    private final PassDefinition pass;
    private final PlayerPassData data;
    private int page = 0;

    public PassDailyQuestsScreen(Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier) {
        super((Component)Component.translatable((String)"fantasticpass.gui.daily_quests"), parent, PassDailyQuestsScreen.castle("battlepass_daily_quest"), 43, 9, 20, 247, 160);
        this.pass = pass;
        this.data = data;
    }

    @Override
    protected void initControls() {
    }

    private int premiumDailyCount() {
        int override = this.pass == null ? 0 : this.pass.getDailyPremiumCount();
        return override > 0 ? override : (Integer)PassConfig.DAILY_PREMIUM_COUNT.get();
    }

    private int totalCount() {
        int free = 0;
        int prem = 0;
        for (Quest q : QuestManager.activeDaily(this.pass, this.data)) {
            if (q.getId().startsWith("dp_")) {
                ++prem;
            } else {
                ++free;
            }
        }
        if (!this.data.isPremium()) {
            prem = this.premiumDailyCount();
        }
        return Math.max(free, prem);
    }

    private int pages() {
        return CastleScreen.pageCount(PER_PAGE, this.totalCount());
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.drawCastleBackground(g);
        if (PassDailyQuestsScreen.isPeek()) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        boolean premium = this.data.isPremium();
        List<Quest> freeQuests = new ArrayList<Quest>();
        List<Quest> premiumQuests = new ArrayList<Quest>();
        for (Quest q2 : QuestManager.activeDaily(this.pass, this.data)) {
            (q2.getId().startsWith("dp_") ? premiumQuests : freeQuests).add(q2);
        }
        if (!premium) {
            premiumQuests = QuestManager.previewPremiumDaily(this.pass, Minecraft.getInstance().player.getUUID(), this.premiumDailyCount());
        }
        int total = Math.max(freeQuests.size(), premiumQuests.size());
        int pages = CastleScreen.pageCount(PER_PAGE, total);
        this.page = Math.max(0, Math.min(pages - 1, this.page));
        int base = CastleScreen.pageBase(this.page, PER_PAGE, total);
        List<Component> tooltip = null;
        for (int i = 0; i < PER_PAGE && base + i < freeQuests.size(); ++i) {
            int col = 2 + i;
            Quest q = freeQuests.get(base + i);
            int progress = this.data.getQuestProgress(q.getId());
            boolean claimed = this.data.isQuestClaimed(q.getId());
            this.drawQuestSlot(g, q, col, 1, progress, claimed);
            if (this.overSlot(mouseX, mouseY, col, 1)) {
                tooltip = this.questTooltip(q, progress, claimed);
            }
        }
        for (int i = 0; i < PER_PAGE && base + i < premiumQuests.size(); ++i) {
            int col = 2 + i;
            Quest q = premiumQuests.get(base + i);
            if (premium) {
                int progress = this.data.getQuestProgress(q.getId());
                boolean claimed = this.data.isQuestClaimed(q.getId());
                this.drawQuestSlot(g, q, col, 2, progress, claimed);
                if (this.overSlot(mouseX, mouseY, col, 2)) {
                    tooltip = this.questTooltip(q, progress, claimed);
                }
            } else {
                this.drawQuestSlotLocked(g, col, 2);
                if (this.overSlot(mouseX, mouseY, col, 2)) {
                    tooltip = this.questLockedTooltip(q);
                }
            }
        }
        boolean multi = pages > 1;
        this.drawIcon(g, PassDailyQuestsScreen.icon(6), 3, 4);
        this.drawIcon(g, PassDailyQuestsScreen.icon(10), 4, 4);
        if (multi) {
            this.drawIcon(g, PassDailyQuestsScreen.icon(7), 5, 4);
        }
        if (this.overSlot(mouseX, mouseY, 3, 4)) {
            this.hover(g, 3);
            tooltip = List.of(Component.translatable((String)"fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
        } else if (this.overSlot(mouseX, mouseY, 4, 4)) {
            this.hover(g, 4);
            tooltip = new ArrayList<Component>(List.of(
                Component.translatable((String)"fantasticpass.gui.daily_quests").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}),
                Component.translatable((String)"fantasticpass.gui.daily_reset").withStyle(ChatFormatting.GRAY),
                Component.translatable((String)"fantasticpass.gui.daily_timer", (Object[])new Object[]{PassDailyQuestsScreen.formatRemaining()}).withStyle(ChatFormatting.AQUA)));
            if (multi) {
                tooltip.add(Component.literal("\u00a77Pagina \u00a7f" + (this.page + 1) + "\u00a77/\u00a7f" + pages));
            }
        } else if (multi && this.overSlot(mouseX, mouseY, 5, 4)) {
            this.hover(g, 5);
            tooltip = List.of(Component.translatable((String)"fantasticpass.gui.next").withStyle(ChatFormatting.YELLOW),
                Component.literal("\u00a77Pagina \u00a7f" + (this.page + 1) + "\u00a77/\u00a7f" + pages));
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (tooltip != null) {
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private static String formatRemaining() {
        long dayMs = 86400000L;
        long rem = dayMs - System.currentTimeMillis() % dayMs;
        long s = rem / 1000L;
        return String.format("%02d:%02d:%02d", s / 3600L, s % 3600L / 60L, s % 60L);
    }

    private void hover(GuiGraphics g, int col) {
        int x = this.slotX(col);
        int y = this.slotY(4);
        g.fill(x, y, x + this.slotPx(), y + this.slotPx(), 0x33FFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (PassDailyQuestsScreen.isPeek()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (this.overSlot(mouseX, mouseY, 3, 4)) {
                this.playClick(0.9f);
                if (this.page == 0) {
                    this.onClose();
                } else {
                    --this.page;
                }
                return true;
            }
            int pages = this.pages();
            if (pages > 1 && this.overSlot(mouseX, mouseY, 5, 4)) {
                this.page = Math.max(0, Math.min(pages - 1, this.page + 1));
                this.playClick(1.1f);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
