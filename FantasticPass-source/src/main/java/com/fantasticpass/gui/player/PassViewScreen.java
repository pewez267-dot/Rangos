/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.player;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.castle.CastleScreen;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.progression.RewardDispatcher;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class PassViewScreen
extends CastleScreen {
    private static final int TIERS_PER_PAGE = 9;
    private static final int ROW_FREE = 0;
    private static final int ROW_TRACK = 1;
    private static final int ROW_PREM = 2;
    private static final int ROW_NAV = 4;
    private static final int NAV_PREV = 3;
    private static final int NAV_INFO = 4;
    private static final int NAV_NEXT = 5;
    private static final int BAR_X0 = 49;
    private static final int BAR_X1 = 207;
    private static final int BAR_Y = 40;
    private static final int BAR_H = 4;
    private final PassDefinition pass;
    private PlayerPassData data;
    private final int pointsPerTier;
    private final boolean premiumView;
    private final int tierCount;
    private int page;
    private float pulse;
    private long flashUntil;
    private int flashTier;
    private boolean flashSuccess;
    private boolean flashPremium;

    public PassViewScreen(@Nullable Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier, boolean premiumView) {
        super((Component)Component.translatable((String)"fantasticpass.gui.view.title"), parent, PassViewScreen.castle("battlepass_reward"), 43, 9, 20, 247, 160);
        this.pass = pass;
        this.data = data;
        this.pointsPerTier = Math.max(1, pointsPerTier);
        this.premiumView = premiumView;
        this.tierCount = pass.getTierCount();
        int cur = Math.max(1, data.getCurrentTier());
        this.page = Mth.clamp((int)((cur - 1) / 9), (int)0, (int)(PassViewScreen.pageCount(9, this.tierCount) - 1));
    }

    private int pages() {
        return PassViewScreen.pageCount(9, this.tierCount);
    }

    @Override
    protected void initControls() {
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.pulse += partialTick * 0.12f;
        this.drawCastleBackground(g);
        if (PassViewScreen.isPeek()) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        this.drawProgressBar(g, 49, 207, 40, 4, this.tierFraction());
        int base = PassViewScreen.pageBase(this.page, 9, this.tierCount);
        List<Component> tooltip = null;
        for (int c = 0; c < 9; ++c) {
            int tier = base + c + 1;
            if (tier > this.tierCount) continue;
            this.drawTierColumn(g, c, tier);
            if (this.overSlot(mouseX, mouseY, c, 0)) {
                tooltip = this.rewardTooltip(tier, false);
                continue;
            }
            if (this.overSlot(mouseX, mouseY, c, 2)) {
                tooltip = this.premiumView ? this.rewardTooltip(tier, true) : this.premiumLockedTooltip(tier);
                continue;
            }
            if (!this.overSlot(mouseX, mouseY, c, 1)) continue;
            tooltip = this.trackTooltip(tier);
        }
        this.drawNav(g, mouseX, mouseY);
        if (this.overSlot(mouseX, mouseY, 3, 4)) {
            tooltip = List.<Component>of(Component.translatable((String)"fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
        } else if (this.overSlot(mouseX, mouseY, 5, 4)) {
            tooltip = List.<Component>of(Component.translatable((String)"fantasticpass.gui.next").withStyle(ChatFormatting.YELLOW));
        } else if (this.overSlot(mouseX, mouseY, 4, 4)) {
            tooltip = this.infoTooltip();
        } else if (this.overProgressBar(mouseX, mouseY)) {
            tooltip = this.progressTooltip();
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (tooltip != null) {
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private float tierFraction() {
        int cur = this.data.getCurrentTier();
        if (cur >= this.tierCount) {
            return 1.0f;
        }
        int into = this.data.getPoints() - cur * this.pointsPerTier;
        return Mth.clamp((float)((float)into / (float)this.pointsPerTier), (float)0.0f, (float)1.0f);
    }

    private boolean overProgressBar(double mx, double my) {
        return mx >= (double)this.sx(49) && mx < (double)this.sx(207) && my >= (double)(this.sy(40) - 2) && my < (double)(this.sy(44) + 2);
    }

    private List<Component> progressTooltip() {
        ArrayList<Component> l = new ArrayList<Component>();
        int cur = this.data.getCurrentTier();
        l.add((Component)Component.translatable((String)"fantasticpass.gui.progress").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.level", (Object[])new Object[]{cur}).withStyle(ChatFormatting.YELLOW));
        if (cur >= this.tierCount) {
            l.add((Component)Component.translatable((String)"fantasticpass.gui.maxed").withStyle(ChatFormatting.GREEN));
        } else {
            int into = Math.max(0, this.data.getPoints() - cur * this.pointsPerTier);
            l.add((Component)Component.translatable((String)"fantasticpass.gui.xp", (Object[])new Object[]{Math.min(into, this.pointsPerTier), this.pointsPerTier}).withStyle(ChatFormatting.AQUA));
            l.add((Component)Component.translatable((String)"fantasticpass.gui.total_points", (Object[])new Object[]{this.data.getPoints()}).withStyle(ChatFormatting.GRAY));
        }
        return l;
    }

    private void drawTierColumn(GuiGraphics g, int col, int tier) {
        boolean hasPrem;
        TierDefinition def = this.pass.getTier(tier);
        boolean unlocked = tier <= this.data.getCurrentTier();
        boolean premium = this.data.isPremium();
        boolean freeClaimed = this.data.isFreeClaimed(tier);
        boolean premClaimed = this.data.isPremiumClaimed(tier);
        boolean hasFree = def != null && (!def.getFreeRewards().isEmpty() || def.hasRankReward());
        this.drawRewardSlot(g, col, 0, hasFree, unlocked, freeClaimed, unlocked && !freeClaimed, false);
        boolean bl = hasPrem = def != null && !def.getPremiumRewards().isEmpty();
        if (this.premiumView) {
            this.drawRewardSlot(g, col, 2, hasPrem, unlocked, premClaimed, unlocked && premium && !premClaimed, !premium && hasPrem);
        } else {
            this.drawRewardSlot(g, col, 2, hasPrem, false, false, false, hasPrem);
        }
        this.drawIcon(g, PassViewScreen.icon(unlocked ? 4 : 5), col, 1);
        int s = this.slotPx();
        int cx = this.slotX(col) + s / 2;
        int cy = this.slotY(1) + s / 2 - 4;
        int numColor = tier == this.data.getCurrentTier() ? -7605 : (unlocked ? -1 : -4609652);
        g.drawCenteredString(this.font, String.valueOf(tier), cx, cy, numColor);
    }

    private void drawRewardSlot(GuiGraphics g, int col, int row, boolean hasReward, boolean unlocked, boolean claimed, boolean claimable, boolean notEligible) {
        boolean flashRow;
        int x = this.slotX(col);
        int y = this.slotY(row);
        int s = this.slotPx();
        if (notEligible) {
            this.drawIconAt(g, PassViewScreen.icon(9), x, y);
            return;
        }
        if (!hasReward) {
            return;
        }
        if (claimed) {
            this.drawIconAt(g, PassViewScreen.icon(3), x, y);
        } else {
            this.drawIconAt(g, PassViewScreen.icon(2), x, y);
            if (claimable) {
                float a = 0.45f + 0.45f * (float)Math.abs(Math.sin(this.pulse));
                g.renderOutline(x - 1, y - 1, s + 2, s + 2, (int)(a * 220.0f) << 24 | 0xFFE24B);
            }
        }
        boolean bl = flashRow = row == 0 && !this.flashPremium || row == 2 && this.flashPremium;
        if (flashRow && System.currentTimeMillis() < this.flashUntil && this.flashTier == this.tierOfColumn(col)) {
            float ft = Mth.clamp((float)((float)(this.flashUntil - System.currentTimeMillis()) / 600.0f), (float)0.0f, (float)1.0f);
            g.fill(x, y, x + s, y + s, (int)(ft * 170.0f) << 24 | (this.flashSuccess ? 0x6FFF6F : 0xFF6F6F));
        }
    }

    private void drawNav(GuiGraphics g, int mouseX, int mouseY) {
        this.drawIcon(g, PassViewScreen.icon(6), 3, 4);
        this.drawIcon(g, PassViewScreen.icon(8), 4, 4);
        this.drawIcon(g, PassViewScreen.icon(7), 5, 4);
        this.hoverSlot(g, 3, 4, mouseX, mouseY);
        this.hoverSlot(g, 4, 4, mouseX, mouseY);
        this.hoverSlot(g, 5, 4, mouseX, mouseY);
    }

    private void hoverSlot(GuiGraphics g, int col, int row, int mouseX, int mouseY) {
        if (this.overSlot(mouseX, mouseY, col, row)) {
            int x = this.slotX(col);
            int y = this.slotY(row);
            int s = this.slotPx();
            g.fill(x, y, x + s, y + s, 0x33FFFFFF);
        }
    }

    private int tierOfColumn(int col) {
        return PassViewScreen.pageBase(this.page, 9, this.tierCount) + col + 1;
    }

    private List<Component> rewardTooltip(int tier, boolean premium) {
        TierDefinition def = this.pass.getTier(tier);
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)Component.translatable((String)"fantasticpass.gui.tier_info", (Object[])new Object[]{tier}).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}).append((Component)Component.literal((String)"  ")).append((Component)Component.translatable((String)(premium ? "fantasticpass.gui.premium" : "fantasticpass.gui.free")).withStyle(premium ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)));
        List<ItemStack> rewards = def == null ? List.<ItemStack>of() : (premium ? def.getPremiumRewards() : def.getFreeRewards());
        if (rewards.isEmpty() && (def == null || !def.hasRankReward() || premium)) {
            l.add((Component)Component.literal((String)"\u2014").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (ItemStack st : rewards) {
                l.add((Component)Component.literal((String)" \u2022 ").withStyle(ChatFormatting.GRAY).append(st.getCount() + "x ").append(st.getHoverName()));
            }
        }
        if (def != null && def.hasRankReward() && !premium) {
            l.add((Component)Component.literal((String)" \u2756 ").withStyle(ChatFormatting.LIGHT_PURPLE).append((Component)Component.literal((String)def.getRankReward().getRankDisplayText())));
        }
        l.add((Component)Component.empty());
        l.add(this.statusLine(tier, premium));
        return l;
    }

    private List<Component> premiumLockedTooltip(int tier) {
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)Component.translatable((String)"fantasticpass.gui.premium_rewards").withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD}));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.premium_locked").withStyle(ChatFormatting.RED));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.premium_hint").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
        return l;
    }

    private Component statusLine(int tier, boolean premium) {
        boolean unlocked = tier <= this.data.getCurrentTier();
        boolean claimed = this.data.isClaimed(tier, premium);
        if (premium && !this.data.isPremium()) {
            return Component.translatable((String)"fantasticpass.gui.not_eligible").withStyle(ChatFormatting.RED);
        }
        if (claimed) {
            return Component.translatable((String)"fantasticpass.gui.claimed").withStyle(ChatFormatting.GREEN);
        }
        if (unlocked) {
            return Component.translatable((String)"fantasticpass.gui.click_to_claim").withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable((String)"fantasticpass.gui.locked").withStyle(ChatFormatting.RED);
    }

    private List<Component> trackTooltip(int tier) {
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)Component.translatable((String)"fantasticpass.gui.tier_info", (Object[])new Object[]{tier}).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
        if (tier == this.data.getCurrentTier()) {
            int into = Math.max(0, this.data.getPoints() - tier * this.pointsPerTier);
            l.add((Component)Component.translatable((String)"fantasticpass.gui.xp", (Object[])new Object[]{Math.min(into, this.pointsPerTier), this.pointsPerTier}).withStyle(ChatFormatting.AQUA));
        }
        l.add(this.statusLine(tier, this.premiumView));
        return l;
    }

    private List<Component> infoTooltip() {
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)Component.translatable((String)(this.premiumView ? "fantasticpass.gui.premium" : "fantasticpass.gui.rewards")).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.page", (Object[])new Object[]{this.page + 1, this.pages()}).withStyle(ChatFormatting.AQUA));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.level", (Object[])new Object[]{this.data.getCurrentTier()}).withStyle(ChatFormatting.YELLOW));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.premium").append(": ").append((Component)(this.data.isPremium() ? Component.literal((String)"\u2714").withStyle(ChatFormatting.GREEN) : Component.literal((String)"\u2715").withStyle(ChatFormatting.RED))));
        return l;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (PassViewScreen.isPeek()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (this.overSlot(mouseX, mouseY, 3, 4)) {
                this.changePage(-1);
                return true;
            }
            if (this.overSlot(mouseX, mouseY, 5, 4)) {
                this.changePage(1);
                return true;
            }
            int base = PassViewScreen.pageBase(this.page, 9, this.tierCount);
            for (int c = 0; c < 9; ++c) {
                int tier = base + c + 1;
                if (tier > this.tierCount) continue;
                if (this.overSlot(mouseX, mouseY, c, 0)) {
                    this.tryClaim(tier, false);
                    return true;
                }
                if (this.overSlot(mouseX, mouseY, c, 1)) {
                    this.tryClaim(tier, this.premiumView);
                    return true;
                }
                if (!this.overSlot(mouseX, mouseY, c, 2)) continue;
                if (this.premiumView) {
                    this.tryClaim(tier, true);
                } else {
                    this.playDenied();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void changePage(int delta) {
        this.changePage(delta, true);
    }

    private void changePage(int delta, boolean allowExit) {
        if (delta < 0 && this.page == 0) {
            if (allowExit) {
                this.playClick(0.9f);
                Minecraft.getInstance().setScreen(this.parent);
            }
            return;
        }
        int np = Mth.clamp((int)(this.page + delta), (int)0, (int)(this.pages() - 1));
        if (np != this.page) {
            this.page = np;
            // Solo suena al usar los botones (allowExit=true); el scroll cambia de pagina en silencio.
            if (allowExit) {
                this.playClick(1.0f + 0.1f * (float)delta);
            }
        }
    }

    private void tryClaim(int tier, boolean premium) {
        boolean unlocked = tier <= this.data.getCurrentTier();
        boolean alreadyClaimed = !this.data.isTestMode() && this.data.isClaimed(tier, premium);
        boolean premiumEligible = !premium || this.data.isPremium();
        TierDefinition def = this.pass.getTier(tier);
        boolean hasReward = def != null && (premium ? !def.getPremiumRewards().isEmpty() : (!def.getFreeRewards().isEmpty() || def.hasRankReward()));
        if (unlocked && premiumEligible && hasReward && !alreadyClaimed) {
            PacketHandler.sendToServer(new ClaimTierPacket(tier, premium));
            this.playClick(0.8f);
        } else {
            this.playDenied();
        }
    }

    public void applyServerData(PlayerPassData serverData, RewardDispatcher.ClaimResult result, int tier, boolean premium) {
        this.data.copyFrom(serverData);
        this.flashTier = tier;
        this.flashPremium = premium;
        this.flashUntil = System.currentTimeMillis() + 600L;
        boolean bl = this.flashSuccess = result == RewardDispatcher.ClaimResult.SUCCESS;
        if (this.flashSuccess) {
            this.playClaimFx(premium);
        } else {
            this.playDenied();
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.changePage(delta > 0.0 ? -1 : 1, false);
        return true;
    }
}

