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
import net.minecraft.world.item.ItemStack;

/**
 * The flagship player-facing Battle Pass screen. Renders the custom hand-drawn sprite set
 * (banner, ornate tier frames, progress bar, premium badge, themed buttons and detail panel)
 * over a cover-scaled background, with looping music, a smooth horizontally-scrollable rail
 * of 100 tier cards (mouse wheel + drag), pulsing/shimmering animations and a fade-in.
 * Fully self-contained; no external resource pack required.
 */
public class PassViewScreen extends Screen {

    // ---- background ----
    private static final ResourceLocation BG =
            new ResourceLocation("fantasticpass", "textures/gui/pass_bg.png");
    private static final int BG_W = 1536;
    private static final int BG_H = 1024;

    // ---- custom sprite set (textures/gui/sprites/) ----
    private static ResourceLocation sprite(String name) {
        return new ResourceLocation("fantasticpass", "textures/gui/sprites/" + name + ".png");
    }

    private static final ResourceLocation S_BANNER = sprite("banner");
    private static final int BANNER_W = 500;
    private static final int BANNER_H = 241;

    private static final ResourceLocation S_FRAME_FREE = sprite("frame_free");
    private static final int FRAME_FREE_W = 88;
    private static final int FRAME_FREE_H = 91;

    private static final ResourceLocation S_FRAME_PREM = sprite("frame_prem");
    private static final int FRAME_PREM_W = 90;
    private static final int FRAME_PREM_H = 92;

    private static final ResourceLocation S_PROGRESS = sprite("progress");
    private static final int PROGRESS_W = 234;
    private static final int PROGRESS_H = 71;

    private static final ResourceLocation S_BTN_CLAIM = sprite("btn_claim");
    private static final int BTN_CLAIM_W = 121;
    private static final int BTN_CLAIM_H = 50;

    private static final ResourceLocation S_BTN_LOCKED = sprite("btn_locked");
    private static final int BTN_LOCKED_W = 118;
    private static final int BTN_LOCKED_H = 44;

    private static final ResourceLocation S_BTN_GRAY = sprite("btn_gray");
    private static final int BTN_GRAY_W = 124;
    private static final int BTN_GRAY_H = 49;

    private static final ResourceLocation S_PREMIUM = sprite("premium");
    private static final int PREMIUM_W = 129;
    private static final int PREMIUM_H = 169;

    private static final ResourceLocation S_PANEL = sprite("panel");
    private static final int PANEL_W = 261;
    private static final int PANEL_H = 152;

    // ---- layout ----
    private static final int SLOT = 58;       // horizontal distance between tier cards
    private static final int CARD_W = 48;     // visible card width
    private static final int FRAME_SZ = 44;   // square frame size inside a card
    private static final int CARD_H = 104;    // number + two stacked frames

    // ---- theme colors (0xRRGGBB) ----
    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFD700;
    private static final int SILVER = 0xC0C0C8;

    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int minutesPerTier;

    private int selectedTier;
    private int railX;
    private int railY;
    private int railWidth;
    private float scrollX;
    private float targetScrollX;
    private int maxScroll;

    private int pX;
    private int pY;
    private int pW;
    private int pH;
    private int railTop;
    private int railBottom;

    private float animProgress;
    private long openTime;
    private PassMusicInstance music;

    // claim/close button hit-boxes (computed in renderDetail)
    private int claimX;
    private int claimY;
    private int claimW;
    private int claimH;
    private boolean claimEnabled;
    private int doneX;
    private int doneY;
    private int doneW;
    private int doneH;

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
        int m = 14;
        this.pX = m;
        this.pY = m;
        this.pW = this.width - 2 * m;
        this.pH = this.height - 2 * m;

        int headerH = 70;
        int detailH = Math.max(86, Math.min(120, pH / 4));
        this.railTop = pY + 8 + headerH + 8;
        this.railBottom = pY + pH - 10 - detailH - 8;
        this.railX = pX + 16;
        this.railWidth = pW - 32;
        this.railY = railTop + Math.max(0, (railBottom - railTop - CARD_H) / 2);

        this.maxScroll = Math.max(0, PassDefinition.TIER_COUNT * SLOT - railWidth);
        this.targetScrollX = clampScroll((selectedTier - 1) * SLOT - railWidth / 2 + CARD_W / 2);
        this.scrollX = this.targetScrollX;
        this.openTime = System.currentTimeMillis();
        this.animProgress = 0f;
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

    @Override
    public void onClose() {
        super.onClose();
    }

    private float clampScroll(float v) {
        return Math.max(0, Math.min(maxScroll, v));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        scrollX += (targetScrollX - scrollX) * 0.35f;
        if (Math.abs(targetScrollX - scrollX) < 0.5f) {
            scrollX = targetScrollX;
        }

        float fade = fadeIn();
        drawCoverBackground(g);
        // Cohesion darken so the sprites pop, plus top/bottom vignette framing.
        g.fill(0, 0, this.width, this.height, ((int) (0x40 * fade)) << 24);
        g.fillGradient(0, 0, this.width, 60, ((int) (0xB0 * fade)) << 24, 0);
        g.fillGradient(0, this.height - 60, this.width, this.height, 0, ((int) (0xB0 * fade)) << 24);

        renderHeader(g, fade);
        renderRail(g, mouseX, mouseY);
        renderDetail(g, mouseX, mouseY);
    }

    private float fadeIn() {
        long e = System.currentTimeMillis() - openTime;
        return Math.max(0f, Math.min(1f, e / 320f));
    }

    private void drawCoverBackground(GuiGraphics g) {
        float scale = Math.max(this.width / (float) BG_W, this.height / (float) BG_H);
        int dw = Math.round(BG_W * scale);
        int dh = Math.round(BG_H * scale);
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        g.blit(BG, dx, dy, dw, dh, 0f, 0f, BG_W, BG_H, BG_W, BG_H);
    }

    // ---------------------------------------------------------------- header

    private void renderHeader(GuiGraphics g, float fade) {
        int top = pY + 4;

        // Hand-drawn BATTLE PASS banner, centered.
        int bannerH = 58;
        int bannerW = Math.round(bannerH * (BANNER_W / (float) BANNER_H));
        int maxW = Math.min(pW - 40, 300);
        if (bannerW > maxW) {
            bannerW = maxW;
            bannerH = Math.round(bannerW * (BANNER_H / (float) BANNER_W));
        }
        int bannerX = pX + (pW - bannerW) / 2;
        g.blit(S_BANNER, bannerX, top, bannerW, bannerH, 0f, 0f, BANNER_W, BANNER_H, BANNER_W, BANNER_H);

        // Pass name under the banner.
        String name = pass.getName() == null ? "" : pass.getName();
        if (!name.isEmpty()) {
            int nw = this.font.width(name);
            g.drawString(this.font, "\u00a77" + name, pX + (pW - nw) / 2, top + bannerH - 2, 0xFFB6C2CC, true);
        }

        int tier = data.getCurrentTier();

        // PREMIUM badge sprite (top-right) if the player owns premium.
        if (data.isPremium()) {
            int pbH = 56;
            int pbW = Math.round(pbH * (PREMIUM_W / (float) PREMIUM_H));
            int pbX = pX + pW - pbW - 4;
            int pbY = top - 2;
            g.blit(S_PREMIUM, pbX, pbY, pbW, pbH, 0f, 0f, PREMIUM_W, PREMIUM_H, PREMIUM_W, PREMIUM_H);
        }

        // Progress bar: ornate sprite frame + functional inner fill.
        int barW = Math.min(pW - 48, 300);
        int barH = Math.round(barW * (PROGRESS_H / (float) PROGRESS_W));
        if (barH > 30) {
            barH = 30;
            barW = Math.round(barH * (PROGRESS_W / (float) PROGRESS_H));
        }
        int barX = pX + (pW - barW) / 2;
        int barY = top + bannerH + 8;

        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        float target = tier >= PassDefinition.TIER_COUNT ? 1f
                : Math.min(1f, minutesInto / (float) minutesPerTier);
        animProgress += (target - animProgress) * 0.08f;

        // inner functional fill region (inset inside the ornate frame)
        int innerPadX = Math.round(barW * 0.10f);
        int innerPadY = Math.round(barH * 0.30f);
        int fx0 = barX + innerPadX;
        int fy0 = barY + innerPadY;
        int fx1 = barX + barW - innerPadX;
        int fy1 = barY + barH - innerPadY;
        g.fill(fx0, fy0, fx1, fy1, 0xFF0A0E16);
        int fillW = Math.round((fx1 - fx0) * animProgress);
        if (fillW > 0) {
            g.fillGradient(fx0, fy0, fx0 + fillW, fy1, 0xFF00E5FF, 0xFF0066AA);
            // moving shimmer on the fill
            int range = Math.max(1, fillW);
            int sh = fx0 + (int) ((System.currentTimeMillis() / 12) % range);
            g.fill(sh, fy0, Math.min(fx0 + fillW, sh + 2), fy1, 0x66FFFFFF);
        }
        // ornate frame on top of the fill
        g.blit(S_PROGRESS, barX, barY, barW, barH, 0f, 0f, PROGRESS_W, PROGRESS_H, PROGRESS_W, PROGRESS_H);

        // tier label + minutes
        String tierText = "\u00a7lTIER \u00a7e" + tier + "\u00a78/100";
        int tw = this.font.width(tierText);
        g.drawString(this.font, tierText, barX + (barW - tw) / 2, barY - 11, 0xFFFFFFFF, true);
        String pt = tier >= PassDefinition.TIER_COUNT ? "MAX" : minutesInto + " / " + minutesPerTier + " min";
        int ptw = this.font.width(pt);
        g.drawString(this.font, pt, barX + (barW - ptw) / 2, barY + barH / 2 - 4, 0xFFFFFFFF, true);
    }

    // ---------------------------------------------------------------- rail

    private void renderRail(GuiGraphics g, int mouseX, int mouseY) {
        int stripY = railTop - 4;
        int stripH = railBottom - railTop + 8;
        // translucent backdrop strip with cyan accents
        g.fill(railX - 10, stripY, railX + railWidth + 10, stripY + stripH, 0xB0070A12);
        g.fill(railX - 10, stripY, railX + railWidth + 10, stripY + 2, 0x6600E5FF);
        g.fill(railX - 10, stripY + stripH - 2, railX + railWidth + 10, stripY + stripH, 0x33FFD700);

        g.enableScissor(railX - 8, stripY + 2, railX + railWidth + 8, stripY + stripH - 2);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int cx = railX + (tier - 1) * SLOT - Math.round(scrollX);
            if (cx + CARD_W < railX - 8 || cx > railX + railWidth + 8) {
                continue;
            }
            boolean hovered = mouseX >= cx && mouseX < cx + CARD_W
                    && mouseY >= railY && mouseY < railY + CARD_H;
            drawCard(g, tier, cx, railY, hovered);
        }
        g.disableScissor();
    }

    private void drawCard(GuiGraphics g, int tier, int x, int y, boolean hovered) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;

        // tier number plate
        int numCol = claimed ? 0xFF9AA0AC : unlocked ? 0xFFFFFFFF : 0xFF6A6A74;
        g.drawCenteredString(this.font, String.valueOf(tier), x + CARD_W / 2, y, numCol);

        int frameX = x + (CARD_W - FRAME_SZ) / 2;
        int freeY = y + 12;
        int premY = freeY + FRAME_SZ + 2;

        // ---- free reward frame ----
        applyStateTint(g, claimed, unlocked, false);
        g.blit(S_FRAME_FREE, frameX, freeY, FRAME_SZ, FRAME_SZ, 0f, 0f, FRAME_FREE_W, FRAME_FREE_H, FRAME_FREE_W, FRAME_FREE_H);
        g.setColor(1f, 1f, 1f, 1f);
        ItemStack free = def != null && !def.getFreeRewards().isEmpty()
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        drawCenteredItem(g, free, frameX + FRAME_SZ / 2, freeY + FRAME_SZ / 2);

        // ---- premium reward frame ----
        applyStateTint(g, claimed, unlocked, true);
        g.blit(S_FRAME_PREM, frameX, premY, FRAME_SZ, FRAME_SZ, 0f, 0f, FRAME_PREM_W, FRAME_PREM_H, FRAME_PREM_W, FRAME_PREM_H);
        g.setColor(1f, 1f, 1f, 1f);
        ItemStack prem = def != null && !def.getPremiumRewards().isEmpty()
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;
        drawCenteredItem(g, prem, frameX + FRAME_SZ / 2, premY + FRAME_SZ / 2);
        if (!prem.isEmpty() && !data.isPremium()) {
            // premium lock veil
            g.fill(frameX + 4, premY + 4, frameX + FRAME_SZ - 4, premY + FRAME_SZ - 4, 0x99000000);
            g.drawCenteredString(this.font, "\u26BF", frameX + FRAME_SZ / 2, premY + FRAME_SZ / 2 - 4, 0xFF000000 | GOLD);
        }

        // ---- overlays ----
        if (unlocked && !claimed) {
            int pc = pulse(0x004A55, CYAN);
            g.renderOutline(frameX - 2, freeY - 2, FRAME_SZ + 4, premY + FRAME_SZ - freeY + 4, 0xFF000000 | pc);
        }
        if (claimed) {
            g.drawString(this.font, "\u2714", frameX + FRAME_SZ - 8, freeY - 1, 0xFF55FF55, true);
        }
        if (selected) {
            g.renderOutline(frameX - 3, freeY - 3, FRAME_SZ + 6, premY + FRAME_SZ - freeY + 6, 0xFF000000 | GOLD);
        }
        if (hovered) {
            g.fill(frameX - 2, freeY - 2, frameX + FRAME_SZ + 2, premY + FRAME_SZ + 2, 0x2600E5FF);
        }
    }

    /** Multiplicative tint to convey tier state on a sprite. */
    private void applyStateTint(GuiGraphics g, boolean claimed, boolean unlocked, boolean premiumFrame) {
        if (!unlocked) {
            g.setColor(0.42f, 0.42f, 0.48f, 1f);        // desaturated/dark = locked
        } else if (claimed) {
            g.setColor(0.72f, 0.74f, 0.78f, 1f);        // dimmed = already claimed
        } else if (premiumFrame) {
            g.setColor(1f, 0.92f, 0.55f, 1f);           // gold glow = premium available
        } else {
            g.setColor(0.8f, 0.96f, 1f, 1f);            // cyan tint = free available
        }
    }

    private void drawCenteredItem(GuiGraphics g, ItemStack stack, int cx, int cy) {
        if (stack.isEmpty()) {
            return;
        }
        int ix = cx - 8;
        int iy = cy - 8;
        g.renderItem(stack, ix, iy);
        g.renderItemDecorations(this.font, stack, ix, iy);
    }

    // ---------------------------------------------------------------- detail

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        int x = pX + 6;
        int y = railBottom + 8;
        int w = pW - 12;
        int h = pY + pH - 10 - y;
        if (h < 40) {
            return;
        }

        // hand-drawn panel background, stretched to the detail area
        g.blit(S_PANEL, x, y, w, h, 0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        int pad = 14;
        TierDefinition def = pass.getTier(selectedTier);
        g.drawString(this.font, Component.translatable("fantasticpass.gui.tier", selectedTier),
                x + pad, y + pad, 0xFF000000 | CYAN, true);

        g.drawString(this.font, "\u00a7fFree", x + pad, y + pad + 16, 0xFFFFFFFF, true);
        drawRewardIcons(g, def, x + pad + 46, y + pad + 13, false);
        g.drawString(this.font, "\u00a76Premium", x + pad, y + pad + 38, 0xFF000000 | GOLD, true);
        drawRewardIcons(g, def, x + pad + 46, y + pad + 35, true);

        // ---- Claim button (sprite-based) ----
        boolean claimed = selectedTier <= data.getCurrentTier() && data.isTierClaimed(selectedTier);
        boolean unlocked = selectedTier <= data.getCurrentTier();
        claimEnabled = unlocked && !claimed;

        claimW = 120;
        claimH = 30;
        claimX = x + w - claimW - pad;
        claimY = y + h - claimH - pad;
        boolean hov = inside(mouseX, mouseY, claimX, claimY, claimW, claimH);

        ResourceLocation btn;
        int btnTexW;
        int btnTexH;
        String label;
        int labelCol;
        if (!unlocked) {
            btn = S_BTN_LOCKED;
            btnTexW = BTN_LOCKED_W;
            btnTexH = BTN_LOCKED_H;
            label = "\ud83d\udd12 Locked";
            labelCol = 0xFFE8C0C0;
        } else if (claimed) {
            btn = S_BTN_GRAY;
            btnTexW = BTN_GRAY_W;
            btnTexH = BTN_GRAY_H;
            label = "\u2714 Claimed";
            labelCol = 0xFFCFE8D2;
        } else {
            btn = S_BTN_CLAIM;
            btnTexW = BTN_CLAIM_W;
            btnTexH = BTN_CLAIM_H;
            label = "Claim";
            labelCol = 0xFFFFFFFF;
        }
        if (hov && claimEnabled) {
            g.setColor(1.15f, 1.15f, 1.15f, 1f);
        }
        g.blit(btn, claimX, claimY, claimW, claimH, 0f, 0f, btnTexW, btnTexH, btnTexW, btnTexH);
        g.setColor(1f, 1f, 1f, 1f);
        int lw = this.font.width(label);
        g.drawString(this.font, label, claimX + (claimW - lw) / 2, claimY + (claimH - 8) / 2, labelCol, true);

        // ---- Close button (sprite-based) ----
        doneW = 90;
        doneH = 26;
        doneX = x + pad;
        doneY = y + h - doneH - pad;
        boolean dh = inside(mouseX, mouseY, doneX, doneY, doneW, doneH);
        if (dh) {
            g.setColor(1.15f, 1.15f, 1.15f, 1f);
        }
        g.blit(S_BTN_GRAY, doneX, doneY, doneW, doneH, 0f, 0f, BTN_GRAY_W, BTN_GRAY_H, BTN_GRAY_W, BTN_GRAY_H);
        g.setColor(1f, 1f, 1f, 1f);
        int cw = this.font.width("Close");
        g.drawString(this.font, "Close", doneX + (doneW - cw) / 2, doneY + (doneH - 8) / 2, 0xFFFFFFFF, true);
    }

    private void drawRewardIcons(GuiGraphics g, TierDefinition def, int x, int y, boolean premium) {
        if (def == null) {
            return;
        }
        java.util.List<ItemStack> items = premium ? def.getPremiumRewards() : def.getFreeRewards();
        java.util.List<String> cmds = premium ? def.getPremiumCommands() : def.getFreeCommands();
        int cx = x;
        for (ItemStack s : items) {
            if (s.isEmpty()) {
                continue;
            }
            g.renderItem(s, cx, y);
            g.renderItemDecorations(this.font, s, cx, y);
            cx += 20;
        }
        if (!cmds.isEmpty()) {
            g.drawString(this.font, "\u00a7b+" + cmds.size() + " cmd", cx + 2, y + 4, 0xFF66DDFF, true);
        }
        if (def.hasRankReward() && !premium) {
            g.drawString(this.font, "\u00a7d\u2756 " + def.getRankReward().getRankDisplayText(),
                    cx + 2, y + 4, 0xFFDD88FF, true);
        }
    }

    // ---------------------------------------------------------------- helpers

    private int pulse(int a, int b) {
        float t = (float) ((Math.sin(System.currentTimeMillis() / 450.0) + 1) / 2);
        return lerpColor(a, b, t);
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int gg = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (r << 16) | (gg << 8) | bl;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, doneX, doneY, doneW, doneH)) {
                onClose();
                return true;
            }
            if (claimEnabled && inside(mouseX, mouseY, claimX, claimY, claimW, claimH)) {
                claimSelected();
                return true;
            }
            if (mouseX >= railX && mouseX <= railX + railWidth
                    && mouseY >= railY && mouseY <= railY + CARD_H) {
                int rel = (int) (mouseX - railX + scrollX);
                int tier = rel / SLOT + 1;
                int withinCard = rel % SLOT;
                if (withinCard <= CARD_W && tier >= 1 && tier <= PassDefinition.TIER_COUNT) {
                    selectedTier = tier;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void claimSelected() {
        if (selectedTier <= data.getCurrentTier() && !data.isTierClaimed(selectedTier)) {
            PacketHandler.sendToServer(new ClaimTierPacket(selectedTier));
            data.markClaimed(selectedTier);
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.2f, 0.6f));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= railTop - 6 && mouseY <= railBottom + 6) {
            targetScrollX = clampScroll(targetScrollX - (float) delta * SLOT);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseY >= railTop - 6 && mouseY <= railBottom + 6) {
            targetScrollX = clampScroll(targetScrollX - (float) dragX);
            scrollX = targetScrollX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
