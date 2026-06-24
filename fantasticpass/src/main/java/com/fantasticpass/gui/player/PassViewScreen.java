package com.fantasticpass.gui.player;

import com.fantasticpass.client.PassMusicInstance;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;

/**
 * Player-facing Battle Pass screen ({@code /fspass view}).
 *
 * Design deliberately kept simple and flat, modelled on the Jeqo "Battle Pass UI"
 * resource pack: the custom artwork ({@code pass_bg.png}) stays fully visible as the
 * backdrop, and a clean, lightweight overlay sits on top of it. Tier rewards are shown
 * as a horizontally scrollable rail of flat rounded slots (free row + premium row),
 * using the Jeqo slot textures, with simple tier numbers, lock overlays and a thin
 * progress bar. No player-inventory grid, no heavy panels, no flashy animation.
 */
public class PassViewScreen extends Screen {

    private static final ResourceLocation BG =
            new ResourceLocation("fantasticpass", "textures/gui/pass_bg.png");
    private static final int BG_W = 1536, BG_H = 1024;

    private static final ResourceLocation SLOT_UNCLAIMED =
            new ResourceLocation("fantasticpass", "textures/gui/jeqo/slot_unclaimed.png");
    private static final ResourceLocation SLOT_CLAIMED =
            new ResourceLocation("fantasticpass", "textures/gui/jeqo/slot_claimed.png");
    private static final ResourceLocation SLOT_LOCKED =
            new ResourceLocation("fantasticpass", "textures/gui/jeqo/slot_locked.png");
    private static final ResourceLocation SLOT_PREMIUM =
            new ResourceLocation("fantasticpass", "textures/gui/jeqo/slot_premium.png");
    private static final ResourceLocation LOCK =
            new ResourceLocation("fantasticpass", "textures/gui/jeqo/lock.png");

    private static final int CYAN = 0xFF00E5FF;
    private static final int GOLD = 0xFFFFD700;
    private static final int GREY = 0xFFAAB0BC;

    // rail geometry
    private static final int SLOT_SIZE = 40;
    private static final int STRIDE = 52;     // distance between tier columns
    private static final int ROW_GAP = 8;

    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int minutesPerTier;

    private int selectedTier;
    private int railX, railTop, railWidth, freeRowY, premRowY, numberY;
    private float scrollX, targetScrollX;
    private int maxScroll;

    private int claimX, claimY, claimW, claimH;
    private boolean claimEnabled;

    private PassMusicInstance music;

    public PassViewScreen(PassDefinition pass, PlayerPassData data, int minutesPerTier) {
        super(Component.translatable("fantasticpass.gui.view.title"));
        this.pass = pass;
        this.data = data;
        this.minutesPerTier = Math.max(1, minutesPerTier);
        int cur = data.getCurrentTier();
        this.selectedTier = Math.max(1, Math.min(PassDefinition.TIER_COUNT, cur == 0 ? 1 : cur));
    }

    @Override
    protected void init() {
        // vertical layout: header band (top), reward rail (middle), footer (bottom)
        int centerY = this.height / 2;
        this.numberY = centerY - SLOT_SIZE - ROW_GAP / 2 - 12;
        this.freeRowY = numberY + 12;
        this.premRowY = freeRowY + SLOT_SIZE + ROW_GAP;

        this.railX = 30;
        this.railWidth = this.width - 60;
        this.maxScroll = Math.max(0, PassDefinition.TIER_COUNT * STRIDE - railWidth);
        this.railTop = numberY;

        this.targetScrollX = clampScroll((selectedTier - 1) * STRIDE - railWidth / 2f + SLOT_SIZE / 2f);
        this.scrollX = targetScrollX;
        startMusic();
    }

    private void startMusic() {
        if (music == null) {
            music = new PassMusicInstance();
            Minecraft.getInstance().getSoundManager().play(music);
        }
    }

    @Override
    public void removed() {
        if (music != null) {
            Minecraft.getInstance().getSoundManager().stop(music);
            music = null;
        }
    }

    private float clampScroll(float v) {
        return Math.max(0, Math.min(maxScroll, v));
    }

    // ============================================================ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        scrollX += (targetScrollX - scrollX) * 0.35f;
        if (Math.abs(targetScrollX - scrollX) < 0.4f) scrollX = targetScrollX;

        drawBackground(g);
        drawHeader(g);
        drawRail(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);
    }

    /** Custom artwork shown full-screen with only a light dim so it stays visible. */
    private void drawBackground(GuiGraphics g) {
        float scale = Math.max(this.width / (float) BG_W, this.height / (float) BG_H);
        int dw = Math.round(BG_W * scale);
        int dh = Math.round(BG_H * scale);
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        g.blit(BG, dx, dy, dw, dh, 0f, 0f, BG_W, BG_H, BG_W, BG_H);
        g.fill(0, 0, this.width, this.height, 0x55000000);
    }

    private void drawHeader(GuiGraphics g) {
        int tier = data.getCurrentTier();
        int top = 14;

        // title
        String title = (pass.getName() == null || pass.getName().isEmpty())
                ? "BATTLE PASS" : pass.getName().toUpperCase();
        g.pose().pushPose();
        g.pose().translate(this.width / 2f, top, 0);
        g.pose().scale(1.6f, 1.6f, 1f);
        g.drawString(this.font, "\u00a7l" + title,
                -this.font.width(title) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();

        if (data.isPremium()) {
            String p = "\u2726 PREMIUM";
            int pw = this.font.width(p) + 12;
            int px = this.width - pw - 16;
            roundRect(g, px, top, pw, 16, 0xCC1A1206);
            g.renderOutline(px, top, pw, 16, GOLD);
            g.drawString(this.font, "\u00a7l" + p, px + 6, top + 4, GOLD, true);
        }

        // simple progress bar
        int barW = Math.min(420, this.width - 80);
        int barX = (this.width - barW) / 2;
        int barY = top + 30;
        int barH = 10;
        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        float frac = tier >= PassDefinition.TIER_COUNT ? 1f
                : Math.min(1f, minutesInto / (float) minutesPerTier);

        roundRect(g, barX, barY, barW, barH, 0xCC05080F);
        g.renderOutline(barX, barY, barW, barH, 0x4DFFFFFF);
        int fillW = Math.round((barW - 2) * frac);
        if (fillW > 0) roundRect(g, barX + 1, barY + 1, fillW, barH - 2, CYAN);

        String left = "\u00a7lTIER " + tier + " \u00a77/ " + PassDefinition.TIER_COUNT;
        g.drawString(this.font, left, barX, barY - 11, 0xFFFFFFFF, true);
        String right = tier >= PassDefinition.TIER_COUNT ? "MAX"
                : minutesInto + " / " + minutesPerTier + " min";
        g.drawString(this.font, "\u00a7b" + right,
                barX + barW - this.font.width(right), barY - 11, CYAN, true);
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY) {
        // row labels
        g.drawString(this.font, "\u00a7fFREE", railX - 26, freeRowY + SLOT_SIZE / 2 - 4, 0xFFFFFFFF, true);
        g.drawString(this.font, "\u00a76PREM", railX - 26, premRowY + SLOT_SIZE / 2 - 4, GOLD, true);

        int hovered = tierAt(mouseX, mouseY);

        g.enableScissor(railX, railTop - 2, railX + railWidth, premRowY + SLOT_SIZE + 2);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int x = railX + (tier - 1) * STRIDE - Math.round(scrollX);
            if (x + SLOT_SIZE < railX || x > railX + railWidth) continue;
            drawTier(g, tier, x, tier == hovered);
        }
        g.disableScissor();
    }

    private void drawTier(GuiGraphics g, int tier, int x, boolean hovered) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;

        // tier number
        int numCol = unlocked ? (claimed ? 0xFF9097A2 : 0xFFFFFFFF) : 0xFF6A6F79;
        g.drawCenteredString(this.font, String.valueOf(tier), x + SLOT_SIZE / 2, numberY, numCol);

        ItemStack free = (def != null && !def.getFreeRewards().isEmpty())
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        ItemStack prem = (def != null && !def.getPremiumRewards().isEmpty())
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;

        drawSlot(g, x, freeRowY, free, false, claimed, unlocked, false);
        boolean premLocked = !prem.isEmpty() && !data.isPremium();
        drawSlot(g, x, premRowY, prem, true, claimed, unlocked, premLocked);

        if (selected) {
            g.renderOutline(x - 2, freeRowY - 2, SLOT_SIZE + 4,
                    (premRowY + SLOT_SIZE) - freeRowY + 4, GOLD);
        } else if (hovered) {
            g.renderOutline(x - 2, freeRowY - 2, SLOT_SIZE + 4,
                    (premRowY + SLOT_SIZE) - freeRowY + 4, 0x66FFFFFF);
        }
    }

    private void drawSlot(GuiGraphics g, int x, int y, ItemStack stack, boolean premium,
                          boolean claimed, boolean unlocked, boolean premLocked) {
        ResourceLocation tex;
        if (!unlocked) tex = SLOT_LOCKED;
        else if (claimed) tex = SLOT_CLAIMED;
        else if (premium) tex = SLOT_PREMIUM;
        else tex = SLOT_UNCLAIMED;
        g.blit(tex, x, y, SLOT_SIZE, SLOT_SIZE, 0f, 0f, 16, 16, 16, 16);

        if (!stack.isEmpty()) {
            int ix = x + (SLOT_SIZE - 16) / 2;
            int iy = y + (SLOT_SIZE - 16) / 2;
            if (!unlocked) {
                // render dimmed behind a veil so locked rewards are only teased
                g.renderItem(stack, ix, iy);
                g.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x99000000);
            } else {
                g.renderItem(stack, ix, iy);
                g.renderItemDecorations(this.font, stack, ix, iy);
            }
        }
        if (premLocked || !unlocked) {
            int lx = x + (SLOT_SIZE - 16) / 2;
            int ly = y + (SLOT_SIZE - 16) / 2;
            g.blit(LOCK, lx, ly, 16, 16, 0f, 0f, 16, 16, 16, 16);
        }
    }

    private void drawFooter(GuiGraphics g, int mouseX, int mouseY) {
        int barY = this.height - 40;
        // thin info line
        TierDefinition def = pass.getTier(selectedTier);
        boolean claimed = data.isTierClaimed(selectedTier);
        boolean unlocked = selectedTier <= data.getCurrentTier();
        claimEnabled = unlocked && !claimed;

        StringBuilder info = new StringBuilder("\u00a7fTier " + selectedTier + "  ");
        if (def != null) {
            int items = def.getFreeRewards().size() + def.getPremiumRewards().size();
            int cmds = def.getFreeCommands().size() + def.getPremiumCommands().size();
            if (items > 0) info.append("\u00a77").append(items).append(" items  ");
            if (cmds > 0) info.append("\u00a7b").append(cmds).append(" cmd  ");
            if (def.hasRankReward()) info.append("\u00a7d\u2756 ").append(def.getRankReward().getRankDisplayText());
        }
        g.drawString(this.font, info.toString(), 30, barY + 8, 0xFFFFFFFF, true);

        // claim button (flat)
        claimW = 110; claimH = 22;
        claimX = this.width - claimW - 30;
        claimY = barY;
        boolean hover = inside(mouseX, mouseY, claimX, claimY, claimW, claimH);
        String label; int bg, edge, txt;
        if (!unlocked) { label = "LOCKED"; bg = 0xCC1C2028; edge = 0xFF3A4150; txt = 0xFF8C92A0; }
        else if (claimed) { label = "CLAIMED"; bg = 0xCC16281A; edge = 0xFF3E7A48; txt = 0xFFB7E6BF; }
        else { label = "CLAIM"; bg = hover ? 0xFF00C8E0 : 0xCC053A48; edge = CYAN; txt = hover ? 0xFF06222B : CYAN; }
        roundRect(g, claimX, claimY, claimW, claimH, bg);
        g.renderOutline(claimX, claimY, claimW, claimH, edge);
        g.drawCenteredString(this.font, "\u00a7l" + label, claimX + claimW / 2, claimY + 7, txt);
    }

    // ============================================================ helpers

    private void roundRect(GuiGraphics g, int x, int y, int w, int h, int argb) {
        // flat fill with 1px clipped corners for a soft edge
        g.fill(x + 1, y, x + w - 1, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private int tierAt(double mx, double my) {
        if (my < railTop - 2 || my > premRowY + SLOT_SIZE + 2) return -1;
        if (mx < railX || mx > railX + railWidth) return -1;
        int rel = (int) (mx - railX + scrollX);
        if (rel < 0) return -1;
        int tier = rel / STRIDE + 1;
        if (rel % STRIDE <= SLOT_SIZE && tier >= 1 && tier <= PassDefinition.TIER_COUNT) return tier;
        return -1;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ============================================================ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (claimEnabled && inside(mouseX, mouseY, claimX, claimY, claimW, claimH)) {
                claimSelected();
                return true;
            }
            int tier = tierAt(mouseX, mouseY);
            if (tier > 0) {
                selectedTier = tier;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void claimSelected() {
        if (selectedTier <= data.getCurrentTier() && !data.isTierClaimed(selectedTier)) {
            PacketHandler.sendToServer(new ClaimTierPacket(selectedTier));
            data.markClaimed(selectedTier);
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f, 0.7f));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        targetScrollX = clampScroll(targetScrollX - (float) delta * STRIDE);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseY >= railTop - 20 && mouseY <= premRowY + SLOT_SIZE + 20) {
            targetScrollX = clampScroll(targetScrollX - (float) dragX);
            scrollX = targetScrollX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
