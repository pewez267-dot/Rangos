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
 * <p>Flat, calm layout modelled on the Jeqo "Battle Pass UI" resource pack. The custom
 * artwork ({@code pass_bg.png}) stays fully visible behind a light dim. Rewards are shown
 * as two clearly separated horizontal lanes &mdash; a silver FREE lane on top and a gold
 * PREMIUM lane below &mdash; each with its own labelled strip. Tiers use the flat Jeqo slot
 * textures with the tier number above the column. The progress bar is a single clean track
 * filled with Jeqo's green gradient (no moving gimmicks). Claimable tiers carry a small,
 * static cyan marker; the selected tier is shown with a calm highlighted column rather than
 * a heavy outline. A tiny mute toggle sits in the very top-right corner.</p>
 */
public class PassViewScreen extends Screen {

    private static final ResourceLocation BG =
            new ResourceLocation("fantasticpass", "textures/gui/pass_bg.png");
    private static final int BG_W = 1536, BG_H = 1024;

    private static final String JEQO = "textures/gui/jeqo/";
    private static final ResourceLocation SLOT_UNCLAIMED = jeqo("slot_unclaimed");
    private static final ResourceLocation SLOT_CLAIMED = jeqo("slot_claimed");
    private static final ResourceLocation SLOT_LOCKED = jeqo("slot_locked");
    private static final ResourceLocation SLOT_PREMIUM = jeqo("slot_premium");
    private static final ResourceLocation LOCK = jeqo("lock");
    private static final ResourceLocation XP_FILL = jeqo("xp_fill");

    private static ResourceLocation jeqo(String n) {
        return new ResourceLocation("fantasticpass", JEQO + n + ".png");
    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int CYAN = 0xFF00E5FF;
    private static final int GOLD = 0xFFFFD700;
    private static final int SILVER = 0xFFB9C0CC;
    private static final int MUTED = 0xFF7C828E;
    private static final int XP_PER_MINUTE = 10; // active time -> XP presentation

    // rail geometry
    private static final int SLOT_SIZE = 40;
    private static final int STRIDE = 50;
    private static final int LANE_GAP = 10;
    private static final int LEFT_GUTTER = 60;

    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int minutesPerTier;

    private int selectedTier;
    private int railX, railWidth, railTop, railBottom;
    private int numberY, freeRowY, premRowY;
    private float scrollX, targetScrollX;
    private int maxScroll;

    private int claimX, claimY, claimW, claimH;
    private boolean claimEnabled;
    private int muteX, muteY, muteSize;

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
        int centerY = this.height / 2;
        this.numberY = centerY - SLOT_SIZE - LANE_GAP / 2 - 14;
        this.freeRowY = numberY + 14;
        this.premRowY = freeRowY + SLOT_SIZE + LANE_GAP;
        this.railTop = numberY - 4;
        this.railBottom = premRowY + SLOT_SIZE + 4;

        this.railX = LEFT_GUTTER;
        this.railWidth = this.width - railX - 22;
        this.maxScroll = Math.max(0, PassDefinition.TIER_COUNT * STRIDE - railWidth);

        this.targetScrollX = clampScroll((selectedTier - 1) * STRIDE - railWidth / 2f + SLOT_SIZE / 2f);
        this.scrollX = targetScrollX;

        this.muteSize = 13;
        this.muteX = this.width - muteSize - 8;
        this.muteY = 8;
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
        // smooth, non-gaudy easing for the rail scroll only
        scrollX += (targetScrollX - scrollX) * 0.35f;
        if (Math.abs(targetScrollX - scrollX) < 0.4f) scrollX = targetScrollX;

        drawBackground(g);
        drawHeader(g);
        drawRail(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);
        drawMuteButton(g, mouseX, mouseY);
    }

    private void drawBackground(GuiGraphics g) {
        float scale = Math.max(this.width / (float) BG_W, this.height / (float) BG_H);
        int dw = Math.round(BG_W * scale);
        int dh = Math.round(BG_H * scale);
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        g.blit(BG, dx, dy, dw, dh, 0f, 0f, BG_W, BG_H, BG_W, BG_H);
        // light dim so the artwork stays visible and crisp
        g.fill(0, 0, this.width, this.height, 0x3C000000);
    }

    private void drawHeader(GuiGraphics g) {
        int tier = data.getCurrentTier();
        int top = 16;

        // ---- title (clean, no extra icons) ----
        String title = (pass.getName() == null || pass.getName().isEmpty())
                ? "BATTLE PASS" : pass.getName().toUpperCase();
        int titleW = (int) (this.font.width(title) * 1.5f);
        int titleX = this.width / 2 - titleW / 2;
        g.pose().pushPose();
        g.pose().translate(titleX, top, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawString(this.font, "\u00a7l" + title, 0, 0, WHITE, true);
        g.pose().popPose();

        // ---- premium badge ----
        if (data.isPremium()) {
            String p = "\u2726 PREMIUM";
            int pw = this.font.width(p) + 12;
            int px = this.width - pw - 26;
            roundRect(g, px, top, pw, 16, 0xCC1A1206);
            border(g, px, top, pw, 16, GOLD);
            g.drawString(this.font, "\u00a7l" + p, px + 6, top + 4, GOLD, true);
        }

        // ---- progress bar (single clean track, Jeqo green gradient fill) ----
        int barW = Math.min(440, this.width - 140);
        int barX = (this.width - barW) / 2;
        int barY = top + 34;
        int barH = 14;

        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        int curXp = minutesInto * XP_PER_MINUTE;
        int tierXp = minutesPerTier * XP_PER_MINUTE;
        float frac = tier >= PassDefinition.TIER_COUNT ? 1f : Math.min(1f, curXp / (float) tierXp);

        // dark rounded track + subtle border
        roundRect(g, barX, barY, barW, barH, 0xE6090C12);
        border(g, barX, barY, barW, barH, 0x55FFFFFF);
        // green gradient fill (2x9 jeqo strip stretched -> vertical gradient look)
        int fillW = Math.round((barW - 2) * frac);
        if (fillW > 0) {
            g.blit(XP_FILL, barX + 1, barY + 1, fillW, barH - 2, 0f, 0f, 2, 9, 2, 9);
            // a single thin highlight line along the top of the fill (static, not moving)
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 2, 0x40FFFFFF);
        }

        // labels: LVL on the left, XP on the right
        g.drawString(this.font, "\u00a7lLVL " + tier, barX, barY - 12, WHITE, true);
        String xpText = tier >= PassDefinition.TIER_COUNT ? "MAX" : curXp + " / " + tierXp + " XP";
        g.drawString(this.font, "\u00a7a" + xpText,
                barX + barW - this.font.width(xpText), barY - 12, 0xFF93E6A0, true);
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY) {
        // lane background strips (give FREE / PREMIUM each a clear horizontal track)
        drawLaneStrip(g, freeRowY, SILVER, "FREE");
        drawLaneStrip(g, premRowY, GOLD, "PREM");

        int hovered = tierAt(mouseX, mouseY);

        g.enableScissor(railX - 4, railTop, railX + railWidth + 4, railBottom);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int x = railX + (tier - 1) * STRIDE - Math.round(scrollX);
            if (x + SLOT_SIZE < railX - 4 || x > railX + railWidth + 4) continue;
            drawTier(g, tier, x, tier == hovered);
        }
        g.disableScissor();
    }

    private void drawLaneStrip(GuiGraphics g, int rowY, int accent, String label) {
        int x = railX - 4;
        int w = railWidth + 8;
        int y = rowY - 4;
        int h = SLOT_SIZE + 8;
        g.fill(x, y, x + w, y + h, 0x33000000);          // faint dark track
        g.fill(x, y, x + 2, y + h, accent & 0xCCFFFFFF);  // colored left edge
        // label sits in the left gutter, vertically centred on the lane
        int ly = rowY + (SLOT_SIZE - this.font.lineHeight) / 2;
        g.drawString(this.font, "\u00a7l" + label, 10, ly, accent, true);
    }

    private void drawTier(GuiGraphics g, int tier, int x, boolean hovered) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;
        boolean claimable = unlocked && !claimed;

        // calm selection / hover column highlight (no big outline box, no motion)
        if (selected) {
            g.fill(x - 3, railTop, x + SLOT_SIZE + 3, railBottom, 0x2600E5FF);
            g.fill(x - 3, railTop, x + SLOT_SIZE + 3, railTop + 1, CYAN);
            g.fill(x - 3, railBottom - 1, x + SLOT_SIZE + 3, railBottom, CYAN);
        } else if (hovered) {
            g.fill(x - 3, railTop, x + SLOT_SIZE + 3, railBottom, 0x14FFFFFF);
        }

        // tier number above the column
        int numCol = selected ? GOLD : (unlocked ? (claimed ? MUTED : WHITE) : 0xFF656A74);
        g.drawCenteredString(this.font, (selected ? "\u00a7l" : "") + tier,
                x + SLOT_SIZE / 2, numberY, numCol);

        ItemStack free = (def != null && !def.getFreeRewards().isEmpty())
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        ItemStack prem = (def != null && !def.getPremiumRewards().isEmpty())
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;

        drawSlot(g, x, freeRowY, free, false, claimed, unlocked, false, claimable);
        boolean premLocked = !prem.isEmpty() && !data.isPremium();
        drawSlot(g, x, premRowY, prem, true, claimed, unlocked, premLocked, claimable && !premLocked);
    }

    private void drawSlot(GuiGraphics g, int x, int y, ItemStack stack, boolean premium,
                          boolean claimed, boolean unlocked, boolean premLocked, boolean claimable) {
        ResourceLocation tex;
        if (!unlocked) tex = SLOT_LOCKED;
        else if (claimed) tex = SLOT_CLAIMED;
        else if (premium) tex = SLOT_PREMIUM;
        else tex = SLOT_UNCLAIMED;

        g.blit(tex, x, y, SLOT_SIZE, SLOT_SIZE, 0f, 0f, 16, 16, 16, 16);

        if (!stack.isEmpty()) {
            int ix = x + (SLOT_SIZE - 16) / 2;
            int iy = y + (SLOT_SIZE - 16) / 2;
            g.renderItem(stack, ix, iy);
            if (unlocked) g.renderItemDecorations(this.font, stack, ix, iy);
            if (!unlocked) g.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x99000000);
        }
        if (premLocked || !unlocked) {
            int lx = x + (SLOT_SIZE - 16) / 2;
            int ly = y + (SLOT_SIZE - 16) / 2;
            g.blit(LOCK, lx, ly, 16, 16, 0f, 0f, 16, 16, 16, 16);
        }
        // small, static "ready to claim" marker (top-right corner dot)
        if (claimable) {
            int dx = x + SLOT_SIZE - 8, dy = y + 4;
            g.fill(dx - 1, dy - 1, dx + 5, dy + 5, 0xFF06222B);
            g.fill(dx, dy, dx + 4, dy + 4, CYAN);
        }
    }

    private void drawFooter(GuiGraphics g, int mouseX, int mouseY) {
        int barY = this.height - 40;
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
        g.drawString(this.font, info.toString(), railX - 4, barY + 8, WHITE, true);

        claimW = 110; claimH = 22;
        claimX = this.width - claimW - 22;
        claimY = barY;
        boolean hover = inside(mouseX, mouseY, claimX, claimY, claimW, claimH);
        String label; int bg, edge, txt;
        if (!unlocked) { label = "LOCKED"; bg = 0xCC1C2028; edge = 0xFF3A4150; txt = MUTED; }
        else if (claimed) { label = "CLAIMED"; bg = 0xCC16281A; edge = 0xFF3E7A48; txt = 0xFFB7E6BF; }
        else { label = "CLAIM"; bg = hover ? 0xFF00C8E0 : 0xCC053A48; edge = CYAN; txt = hover ? 0xFF06222B : CYAN; }
        roundRect(g, claimX, claimY, claimW, claimH, bg);
        border(g, claimX, claimY, claimW, claimH, edge);
        g.drawCenteredString(this.font, "\u00a7l" + label, claimX + claimW / 2, claimY + 7, txt);
    }

    /** Tiny mute / unmute toggle in the very top-right corner. */
    private void drawMuteButton(GuiGraphics g, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, muteX, muteY, muteSize, muteSize);
        roundRect(g, muteX, muteY, muteSize, muteSize, hover ? 0xCC123040 : 0x990A1018);
        border(g, muteX, muteY, muteSize, muteSize, hover ? CYAN : 0x66FFFFFF);
        int cx = muteX + 3, cy = muteY + muteSize / 2;
        int col = PassMusicInstance.muted ? MUTED : CYAN;
        g.fill(cx, cy - 1, cx + 2, cy + 1, col);
        g.fill(cx + 2, cy - 2, cx + 4, cy + 2, col);
        if (PassMusicInstance.muted) {
            for (int i = 0; i < 5; i++) g.fill(cx + 5 + i, cy - 2 + i, cx + 6 + i, cy - 1 + i, 0xFFFF5566);
        } else {
            g.fill(cx + 5, cy - 2, cx + 6, cy + 2, col);
            g.fill(cx + 7, cy - 3, cx + 8, cy + 3, col);
        }
    }

    // ============================================================ helpers

    /** Filled rounded-ish rectangle (1px clipped corners). */
    private void roundRect(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x + 1, y, x + w - 1, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    /** Thin 1px border with clipped corners, matching {@link #roundRect}. */
    private void border(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x + 1, y, x + w - 1, y + 1, argb);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private int tierAt(double mx, double my) {
        if (my < railTop || my > railBottom) return -1;
        if (mx < railX - 4 || mx > railX + railWidth + 4) return -1;
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
            if (inside(mouseX, mouseY, muteX, muteY, muteSize, muteSize)) {
                PassMusicInstance.muted = !PassMusicInstance.muted;
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
                return true;
            }
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
        if (button == 0 && mouseY >= railTop - 16 && mouseY <= railBottom + 16) {
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
