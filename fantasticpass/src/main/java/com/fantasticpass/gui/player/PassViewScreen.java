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
 * The flagship player-facing Battle Pass screen: a custom full-screen background, animated
 * dark scrim, an animated progress header, a smooth horizontally-scrollable rail of 100
 * tier cards with per-state visuals and pulses, a detail panel with a themed Claim button,
 * and looping background music. Fully self-contained; no external resource pack required.
 */
public class PassViewScreen extends Screen {

    private static final ResourceLocation BG =
            new ResourceLocation("fantasticpass", "textures/gui/pass_bg.png");
    private static final int BG_W = 1536;
    private static final int BG_H = 1024;

    private static final int SLOT = 54;
    private static final int CARD_W = 46;
    private static final int CARD_H = 76;

    // Theme colors (0xRRGGBB).
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

        int headerH = 56;
        int detailH = Math.max(54, Math.min(78, pH / 4));
        this.railTop = pY + 12 + headerH + 8;
        this.railBottom = pY + pH - 12 - detailH - 8;
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
        // Light cohesion darken so the art stays vibrant, plus edge vignette framing.
        g.fill(0, 0, this.width, this.height, ((int) (0x26 * fade)) << 24);
        g.fillGradient(0, 0, this.width, 48, ((int) (0x99 * fade)) << 24, 0);
        g.fillGradient(0, this.height - 48, this.width, this.height, 0, ((int) (0x99 * fade)) << 24);

        renderHeader(g, fade);
        renderRail(g, mouseX, mouseY);
        renderDetail(g, mouseX, mouseY);
    }

    private void floatPanel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, 0xB0060A12);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0x2600E5FF, 0x00000000);
        g.renderOutline(x, y, w, h, 0xFF000000 | accent);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0x5500E5FF);
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

    private void renderHeader(GuiGraphics g, float fade) {
        int x = pX + 14;
        int y = pY + 10;
        int w = pW - 28;
        floatPanel(g, pX, pY, pW, 52, GOLD);
        // Title with a soft cyan/white shimmer.
        float t = (float) ((Math.sin(System.currentTimeMillis() / 600.0) + 1) / 2);
        int titleCol = lerpColor(0xFFFFFF, CYAN, t);
        g.drawString(this.font, Component.literal("\u00a7lBATTLE PASS"), x, y, 0xFF000000 | titleCol, true);
        g.drawString(this.font, "\u00a77" + pass.getName(), x, y + 12, 0xFFB0B0C0, false);

        int tier = data.getCurrentTier();
        Component tierText = Component.literal("\u00a7lTIER \u00a7e" + tier + "\u00a78/100");
        int tw = this.font.width(tierText);
        g.drawString(this.font, tierText, x + w / 2 - tw / 2, y + 2, 0xFFFFFFFF, true);

        if (data.isPremium()) {
            Component badge = Component.literal("\u2605 PREMIUM");
            int bw = this.font.width(badge) + 12;
            int bx = x + w - bw;
            g.fill(bx, y, bx + bw, y + 14, 0xCC4A3D00);
            g.renderOutline(bx, y, bw, 14, 0xFF000000 | GOLD);
            g.drawString(this.font, badge, bx + 6, y + 3, 0xFF000000 | GOLD, false);
        }

        // Progress bar with animated fill.
        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        float target = tier >= PassDefinition.TIER_COUNT ? 1f : Math.min(1f, minutesInto / (float) minutesPerTier);
        animProgress += (target - animProgress) * 0.08f;
        int barX = x;
        int barY = y + 30;
        int barW = w;
        g.fill(barX, barY, barX + barW, barY + 9, 0xFF0B0F18);
        int fillW = (int) (barW * animProgress);
        g.fillGradient(barX, barY, barX + fillW, barY + 9, 0xFF00E5FF, 0xFF0066AA);
        // moving shimmer on the fill
        if (fillW > 6) {
            int sh = barX + (int) ((System.currentTimeMillis() / 12 % Math.max(1, fillW)));
            g.fill(sh, barY, Math.min(barX + fillW, sh + 2), barY + 9, 0x66FFFFFF);
        }
        g.renderOutline(barX, barY, barW, 9, 0xFF000000 | CYAN);
        String pt = tier >= PassDefinition.TIER_COUNT ? "MAX" : minutesInto + " / " + minutesPerTier + " min";
        int ptw = this.font.width(pt);
        g.drawString(this.font, pt, barX + barW / 2 - ptw / 2, barY + 1, 0xFFFFFFFF, true);
    }

    private void renderRail(GuiGraphics g, int mouseX, int mouseY) {
        int stripY = railTop - 2;
        int stripH = railBottom - railTop + 4;
        floatPanel(g, railX - 8, stripY, railWidth + 16, stripH, CYAN);

        g.enableScissor(railX - 6, stripY + 1, railX + railWidth + 6, stripY + stripH - 1);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int cx = railX + (tier - 1) * SLOT - Math.round(scrollX);
            if (cx + CARD_W < railX - 6 || cx > railX + railWidth + 6) {
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

        int border;
        if (claimed) {
            border = SILVER;
        } else if (unlocked) {
            border = pulse(0x0A3A44, CYAN);
        } else {
            border = 0x3A3A42;
        }
        int bg = claimed ? 0xE0151520 : (unlocked ? 0xE0101822 : 0xC00C0C12);

        g.fill(x, y, x + CARD_W, y + CARD_H, bg);
        if (selected) {
            g.renderOutline(x - 2, y - 2, CARD_W + 4, CARD_H + 4, 0xFF000000 | GOLD);
        }
        if (hovered) {
            g.fill(x, y, x + CARD_W, y + CARD_H, 0x2600E5FF);
        }
        g.renderOutline(x, y, CARD_W, CARD_H, 0xFF000000 | border);
        g.renderOutline(x + 1, y + 1, CARD_W - 2, CARD_H - 2, 0x40000000 | (border & 0xFFFFFF));

        g.drawCenteredString(this.font, String.valueOf(tier), x + CARD_W / 2, y + 3, 0xFFFFFFFF);
        drawCardIcons(g, def, x, y, claimed);
    }

    private void drawCardIcons(GuiGraphics g, TierDefinition def, int x, int y, boolean claimed) {
        int iconX = x + CARD_W / 2 - 8;

        ItemStack free = def != null && !def.getFreeRewards().isEmpty()
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        g.renderItem(free, iconX, y + 16);
        g.renderItemDecorations(this.font, free, iconX, y + 16);

        ItemStack prem = def != null && !def.getPremiumRewards().isEmpty()
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;
        g.renderItem(prem, iconX, y + 40);
        g.renderItemDecorations(this.font, prem, iconX, y + 40);
        if (!prem.isEmpty() && !data.isPremium()) {
            g.fill(iconX, y + 40, iconX + 16, y + 56, 0x99000000);
            g.drawString(this.font, "\u26BF", iconX + 4, y + 44, 0xFF000000 | GOLD, false);
        }

        // Small "free"/"premium" lane ticks.
        g.fill(x + 3, y + 14, x + CARD_W - 3, y + 15, 0x40C0C0C8);
        g.fill(x + 3, y + 38, x + CARD_W - 3, y + 39, 0x40FFD700);

        if (claimed) {
            g.drawString(this.font, "\u2714", x + CARD_W - 9, y + 3, 0xFF55FF55, false);
        }
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        int x = pX + 14;
        int y = railBottom + 8;
        int w = pW - 28;
        int h = pY + pH - 12 - y;
        if (h < 36) {
            return;
        }
        floatPanel(g, x, y, w, h, GOLD);

        TierDefinition def = pass.getTier(selectedTier);
        g.drawString(this.font, Component.translatable("fantasticpass.gui.tier", selectedTier),
                x + 10, y + 8, 0xFF000000 | CYAN, true);

        g.drawString(this.font, "\u00a7fFree", x + 10, y + 24, 0xFFFFFFFF, false);
        int fx = drawRewardIcons(g, def == null ? null : def, x + 50, y + 21, false);
        g.drawString(this.font, "\u00a76Premium", x + 10, y + 44, 0xFF000000 | GOLD, false);
        drawRewardIcons(g, def == null ? null : def, x + 50, y + 41, true);

        // Themed claim button (right side).
        boolean claimed = selectedTier <= data.getCurrentTier() && data.isTierClaimed(selectedTier);
        boolean unlocked = selectedTier <= data.getCurrentTier();
        claimEnabled = unlocked && !claimed;
        claimW = 110;
        claimH = 22;
        claimX = x + w - claimW - 10;
        claimY = y + h - claimH - 8;
        boolean hov = mouseX >= claimX && mouseX < claimX + claimW && mouseY >= claimY && mouseY < claimY + claimH;
        int cbg = !unlocked ? 0xFF202028 : claimed ? 0xFF14301A : (hov ? 0xFF00E5FF : 0xFF0A3A44);
        int cborder = claimed ? 0xFF55FF55 : claimEnabled ? GOLD : 0xFF555560;
        g.fill(claimX, claimY, claimX + claimW, claimY + claimH, cbg);
        g.renderOutline(claimX, claimY, claimW, claimH, 0xFF000000 | cborder);
        String label = claimed ? "\u2714 Claimed" : unlocked ? "Claim Rewards" : "\ud83d\udd12 Locked";
        int lw = this.font.width(label);
        g.drawString(this.font, label, claimX + (claimW - lw) / 2, claimY + 7,
                claimEnabled ? 0xFFFFFFFF : 0xFFAAAAAA, claimEnabled);

        // Done button.
        doneW = 70;
        doneH = 18;
        doneX = x + 10;
        doneY = y + h - doneH - 8;
        boolean dh = mouseX >= doneX && mouseX < doneX + doneW && mouseY >= doneY && mouseY < doneY + doneH;
        g.fill(doneX, doneY, doneX + doneW, doneY + doneH, dh ? 0xFF333344 : 0xFF1A1A24);
        g.renderOutline(doneX, doneY, doneW, doneH, 0xFF000000 | CYAN);
        g.drawCenteredString(this.font, "Close", doneX + doneW / 2, doneY + 5, 0xFFFFFFFF);
    }

    private int drawRewardIcons(GuiGraphics g, TierDefinition def, int x, int y, boolean premium) {
        if (def == null) {
            return x;
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
            g.drawString(this.font, "\u00a7b+" + cmds.size() + " cmd", cx + 2, y + 4, 0xFF66DDFF, false);
        }
        if (def.hasRankReward() && !premium) {
            g.drawString(this.font, "\u00a7d\u2756 " + def.getRankReward().getRankDisplayText(),
                    x, y + 18, 0xFFDD88FF, false);
        }
        return cx;
    }

    // ---- helpers ----

    private void panel(GuiGraphics g, int x, int y, int w, int h, int accent, int fillArgb) {
        g.fill(x, y, x + w, y + h, fillArgb);
        g.renderOutline(x, y, w, h, 0xFF000000 | accent);
        g.fill(x, y, x + w, y + 1, 0x60000000 | (accent & 0xFFFFFF));
    }

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
        if (mouseY >= railTop - 4 && mouseY <= railBottom + 4) {
            targetScrollX = clampScroll(targetScrollX - (float) delta * SLOT);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseY >= railTop - 4 && mouseY <= railBottom + 4) {
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
