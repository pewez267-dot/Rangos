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
 * Flagship player-facing Battle Pass screen, rendered entirely with hand-drawn,
 * fully animated vector-style UI (no baked-text sprites). Dark premium theme
 * inspired by Valorant / Apex Legends battle passes: deep black glass panels,
 * electric cyan + gold accents, animated gradient title, eased progress fill with
 * a moving shimmer and pulsing glow, a horizontally scrollable rail of 100 tier
 * cards with per-state animation (pulse, diagonal sheen sweep, hover lift, claimed
 * check pop, locked desaturation) and a rounded detail panel with animated buttons.
 *
 * Only the background artwork (pass_bg.png) is used as a texture; everything else is
 * drawn procedurally so the GUI matches exactly between design and the live world,
 * and so every element can be animated.
 */
public class PassViewScreen extends Screen {

    // ---- background artwork ----
    private static final ResourceLocation BG =
            new ResourceLocation("fantasticpass", "textures/gui/pass_bg.png");
    private static final int BG_W = 1536;
    private static final int BG_H = 1024;

    // ---- theme palette (0xRRGGBB) ----
    private static final int CYAN = 0x00E5FF;
    private static final int CYAN_DK = 0x0080A0;
    private static final int GOLD = 0xFFD700;
    private static final int GOLD_DK = 0xB8860B;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAB0BC;
    private static final int SILVER = 0xC8CDD6;

    // ---- card geometry ----
    private static final int SLOT = 60;     // horizontal stride between cards
    private static final int CARD_W = 50;   // visible card width
    private static final int CARD_GAP = SLOT - CARD_W;
    private static final int SLOTSZ = 42;   // reward slot square
    private static final int CARD_H = 106;  // number + two stacked slots

    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int minutesPerTier;

    private int selectedTier;

    // rail layout / scroll
    private int railX, railY, railWidth, railTop, railBottom;
    private float scrollX, targetScrollX;
    private int maxScroll;

    // panel layout
    private int pX, pY, pW, pH;
    private int headerBottom;

    // animation state
    private float animFill;
    private long openTime;
    private float hoverLift;      // eased hover lift for the hovered card
    private int hoveredTier = -1;
    private PassMusicInstance music;

    // button hit-boxes
    private int claimX, claimY, claimW, claimH;
    private boolean claimEnabled;
    private int closeX, closeY, closeW, closeH;
    private boolean closeHover, claimHover;

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
        int m = 16;
        this.pX = m;
        this.pY = m;
        this.pW = this.width - 2 * m;
        this.pH = this.height - 2 * m;

        int headerH = 74;
        int detailH = Math.max(92, Math.min(124, pH / 4));
        this.headerBottom = pY + headerH;
        this.railTop = headerBottom + 10;
        this.railBottom = pY + pH - detailH - 10;
        this.railX = pX + 18;
        this.railWidth = pW - 36;
        this.railY = railTop + Math.max(0, (railBottom - railTop - CARD_H) / 2);

        this.maxScroll = Math.max(0, PassDefinition.TIER_COUNT * SLOT - railWidth);
        this.targetScrollX = clampScroll((selectedTier - 1) * SLOT - railWidth / 2f + CARD_W / 2f);
        this.scrollX = this.targetScrollX;
        this.openTime = System.currentTimeMillis();
        this.animFill = 0f;
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

    // ============================================================== render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // eased scroll
        scrollX += (targetScrollX - scrollX) * 0.30f;
        if (Math.abs(targetScrollX - scrollX) < 0.4f) scrollX = targetScrollX;

        float fade = fadeIn();
        long t = System.currentTimeMillis() - openTime;

        drawBackground(g, fade);
        renderHeader(g, t, fade);
        renderRail(g, mouseX, mouseY, t);
        renderDetail(g, mouseX, mouseY, t);
    }

    private float fadeIn() {
        long e = System.currentTimeMillis() - openTime;
        return Math.max(0f, Math.min(1f, e / 280f));
    }

    private void drawBackground(GuiGraphics g, float fade) {
        // cover-scaled artwork
        float scale = Math.max(this.width / (float) BG_W, this.height / (float) BG_H);
        int dw = Math.round(BG_W * scale);
        int dh = Math.round(BG_H * scale);
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        g.blit(BG, dx, dy, dw, dh, 0f, 0f, BG_W, BG_H, BG_W, BG_H);

        // heavy darken so the bright UI pops; slightly stronger as fade completes
        int a = (int) (0x9E * fade);
        g.fill(0, 0, this.width, this.height, a << 24);
        // top + bottom cinematic vignette bands
        g.fillGradient(0, 0, this.width, 70, ((int) (0xCC * fade)) << 24, 0);
        g.fillGradient(0, this.height - 70, this.width, this.height, 0, ((int) (0xCC * fade)) << 24);
    }

    // ============================================================== header

    private void renderHeader(GuiGraphics g, long t, float fade) {
        int x = pX;
        int y = pY;
        int w = pW;

        // header glass strip
        glassPanel(g, x, y, w, 56, 6);
        // animated top accent line (cyan -> gold sweep)
        accentLine(g, x + 8, y + 3, w - 16, t);

        // animated gradient title
        String title = "FANTASTIC PASS";
        int titleW = this.font.width(title) * 2; // drawn at 2x scale
        int tx = x + (w - titleW) / 2;
        int ty = y + 12;
        drawGradientTitle(g, title, tx, ty, t);

        // season / pass name subtitle, left aligned under title
        String name = pass.getName() == null || pass.getName().isEmpty()
                ? "Battle Pass" : pass.getName();
        g.drawString(this.font, "\u00a7l" + name.toUpperCase(), x + 12, y + 40, 0xFF000000 | CYAN, true);

        // premium badge, top-right
        if (data.isPremium()) {
            drawPremiumBadge(g, x + w - 92, y + 8, t);
        }

        // ---- progress bar ----
        int tier = data.getCurrentTier();
        int barH = 14;
        int barX = x + 12;
        int barY = y + 56 + 8;
        int barW = w - 24;

        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        float target = tier >= PassDefinition.TIER_COUNT ? 1f
                : Math.min(1f, minutesInto / (float) minutesPerTier);
        animFill += (target - animFill) * 0.10f;

        drawProgressBar(g, barX, barY, barW, barH, animFill, t);

        // labels
        String tierText = "TIER " + tier + " \u00a78/ 100";
        g.drawString(this.font, "\u00a7l\u00a7f" + tierText, barX + 2, barY - 11, 0xFFFFFFFF, true);
        String pt = tier >= PassDefinition.TIER_COUNT ? "MAX"
                : minutesInto + " / " + minutesPerTier + " min";
        int ptw = this.font.width(pt);
        g.drawString(this.font, "\u00a7b" + pt, barX + barW - ptw - 2, barY - 11, 0xFF000000 | CYAN, true);
    }

    /** Title drawn char-by-char with a moving cyan->gold gradient and soft glow, at 2x scale. */
    private void drawGradientTitle(GuiGraphics g, String text, int x, int y, long t) {
        float phase = (t % 2600L) / 2600f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2f, 2f, 1f);
        int cx = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float f = (float) ((Math.sin((i / (float) text.length() + phase) * Math.PI * 2) + 1) / 2);
            int col = 0xFF000000 | lerpColor(CYAN, GOLD, f);
            g.drawString(this.font, ch, cx, 0, col, true);
            cx += this.font.width(ch);
        }
        g.pose().popPose();
    }

    private void accentLine(GuiGraphics g, int x, int y, int w, long t) {
        g.fill(x, y, x + w, y + 1, 0x33FFFFFF);
        // moving bright segment
        int seg = Math.max(40, w / 5);
        int range = w + seg;
        int pos = (int) ((t / 6) % range) - seg;
        int s = Math.max(x, x + pos);
        int e = Math.min(x + w, x + pos + seg);
        if (e > s) {
            g.fillGradient(s, y, (s + e) / 2, y + 1, 0x0000E5FF, 0xCC00E5FF);
            g.fillGradient((s + e) / 2, y, e, y + 1, 0xCCFFD700, 0x00FFD700);
        }
    }

    private void drawPremiumBadge(GuiGraphics g, int x, int y, long t) {
        int w = 80, h = 20;
        // pulsing gold glow
        float pulse = (float) ((Math.sin(t / 320.0) + 1) / 2);
        softGlow(g, x, y, w, h, 5, lerpAlpha(0x30, 0x70, pulse), GOLD);
        roundRectGrad(g, x, y, w, h, 5, 0xFF6A4E00, 0xFF3A2C00);
        roundRing(g, x, y, w, h, 5, 1, 0xFF000000 | GOLD);
        String s = "\u2726 PREMIUM";
        int sw = this.font.width(s);
        g.drawString(this.font, "\u00a7l" + s, x + (w - sw) / 2, y + (h - 8) / 2, 0xFF000000 | GOLD, true);
    }

    private void drawProgressBar(GuiGraphics g, int x, int y, int w, int h, float frac, long t) {
        frac = Math.max(0f, Math.min(1f, frac));
        // track
        roundRect(g, x, y, w, h, h / 2, 0xCC05080F);
        roundRing(g, x, y, w, h, h / 2, 1, 0x55FFFFFF);
        int fillW = Math.round((w - 2) * frac);
        if (fillW > h) {
            int fx = x + 1;
            int fy = y + 1;
            int fh = h - 2;
            // glow under the fill
            softGlow(g, fx, fy, fillW, fh, 4, 0x55, CYAN);
            roundRectGradH(g, fx, fy, fillW, fh, fh / 2, 0xFF00E5FF, 0xFF0066AA);
            // top sheen
            roundRect(g, fx + 2, fy + 1, fillW - 4, Math.max(1, fh / 3), fh / 4, 0x44FFFFFF);
            // moving shimmer
            int range = Math.max(1, fillW);
            int sh = fx + (int) ((t / 9) % range);
            g.fill(sh, fy, Math.min(fx + fillW, sh + 2), fy + fh, 0x88FFFFFF);
        }
        // segment ticks every 10%
        for (int i = 1; i < 10; i++) {
            int tx = x + (w * i) / 10;
            g.fill(tx, y + 3, tx + 1, y + h - 3, 0x22FFFFFF);
        }
        // percentage centered
        String pct = Math.round(frac * 100) + "%";
        int pw = this.font.width(pct);
        g.drawString(this.font, "\u00a7l" + pct, x + (w - pw) / 2, y + (h - 8) / 2, 0xFFFFFFFF, true);
    }

    // ============================================================== rail

    private void renderRail(GuiGraphics g, int mouseX, int mouseY, long t) {
        int stripY = railTop;
        int stripH = railBottom - railTop;
        glassPanel(g, railX - 12, stripY, railWidth + 24, stripH, 6);
        // thin accent edges
        g.fill(railX - 12, stripY, railX + railWidth + 12, stripY + 1, 0x4400E5FF);
        g.fill(railX - 12, stripY + stripH - 1, railX + railWidth + 12, stripY + stripH, 0x33FFD700);

        // determine hovered tier
        hoveredTier = -1;
        if (mouseY >= railY - 4 && mouseY < railY + CARD_H + 4) {
            int rel = (int) (mouseX - railX + scrollX);
            if (rel >= 0) {
                int tier = rel / SLOT + 1;
                if (rel % SLOT <= CARD_W && tier >= 1 && tier <= PassDefinition.TIER_COUNT) {
                    hoveredTier = tier;
                }
            }
        }
        float targetLift = hoveredTier > 0 ? 1f : 0f;
        hoverLift += (targetLift - hoverLift) * 0.25f;

        g.enableScissor(railX - 10, stripY + 1, railX + railWidth + 10, stripY + stripH - 1);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int cx = railX + (tier - 1) * SLOT - Math.round(scrollX);
            if (cx + CARD_W < railX - 10 || cx > railX + railWidth + 10) continue;
            drawCard(g, tier, cx, railY, t);
        }
        g.disableScissor();

        // edge fades to hint scrollability
        g.fillGradient(railX - 12, stripY + 1, railX + 8, stripY + stripH - 1, 0xCC0A0A0F, 0);
        g.fillGradient(railX + railWidth - 8, stripY + 1, railX + railWidth + 12, stripY + stripH - 1, 0, 0xCC0A0A0F);
    }

    private void drawCard(GuiGraphics g, int tier, int x, int y, long t) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;
        boolean hovered = tier == hoveredTier;

        int lift = hovered ? Math.round(4 * hoverLift) : 0;
        int cy = y - lift;

        int slotX = x + (CARD_W - SLOTSZ) / 2;
        int numY = cy;
        int freeY = cy + 12;
        int premY = freeY + SLOTSZ + 4;

        // tier number
        int numCol = claimed ? 0xFF8A909C : unlocked ? 0xFFFFFFFF : 0xFF5E626C;
        g.drawCenteredString(this.font, String.valueOf(tier), x + CARD_W / 2, numY, numCol);

        // ---- free slot ----
        drawRewardSlot(g, slotX, freeY, def != null && !def.getFreeRewards().isEmpty()
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY, false, claimed, unlocked, false, t);
        // ---- premium slot ----
        ItemStack prem = def != null && !def.getPremiumRewards().isEmpty()
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;
        boolean locked = !prem.isEmpty() && !data.isPremium();
        drawRewardSlot(g, slotX, premY, prem, true, claimed, unlocked, locked, t);

        // ---- card-wide state overlays ----
        int bx = slotX - 3, by = freeY - 3, bw = SLOTSZ + 6, bh = (premY + SLOTSZ) - freeY + 6;

        if (unlocked && !claimed) {
            // pulsing cyan claimable glow + diagonal sheen sweep
            float pulse = (float) ((Math.sin(t / 360.0 + tier) + 1) / 2);
            softGlow(g, bx, by, bw, bh, 4, lerpAlpha(0x18, 0x55, pulse), CYAN);
            roundRing(g, bx, by, bw, bh, 5, 1, 0xFF000000 | lerpColor(CYAN_DK, CYAN, pulse));
            sheenSweep(g, bx, by, bw, bh, t, tier);
        }
        if (selected) {
            float pulse = (float) ((Math.sin(t / 300.0) + 1) / 2);
            softGlow(g, bx - 1, by - 1, bw + 2, bh + 2, 3, lerpAlpha(0x28, 0x66, pulse), GOLD);
            roundRing(g, bx - 1, by - 1, bw + 2, bh + 2, 6, 2, 0xFF000000 | GOLD);
        }
        if (hovered && !selected) {
            roundRing(g, bx, by, bw, bh, 5, 1, 0x88FFFFFF);
        }
        if (claimed) {
            drawCheck(g, slotX + SLOTSZ - 9, freeY - 4, t);
        }
    }

    private void drawRewardSlot(GuiGraphics g, int x, int y, ItemStack stack, boolean premium,
                                boolean claimed, boolean unlocked, boolean locked, long t) {
        int base, edge;
        if (!unlocked) {
            base = 0xE61C2028; edge = 0xFF2A2F3A;            // locked: dark slate
        } else if (claimed) {
            base = 0xE6242A34; edge = 0xFF3A414E;            // claimed: dim
        } else if (premium) {
            base = 0xF0322300; edge = 0xFF000000 | GOLD;     // premium ready: gold
        } else {
            base = 0xF0042530; edge = 0xFF000000 | CYAN;     // free ready: cyan
        }
        roundRectGrad(g, x, y, SLOTSZ, SLOTSZ, 4, base, darken(base, 0.6f));
        roundRing(g, x, y, SLOTSZ, SLOTSZ, 4, 1, edge);
        // inner bevel highlight
        g.fill(x + 3, y + 3, x + SLOTSZ - 3, y + 4, 0x18FFFFFF);

        if (!stack.isEmpty()) {
            int ix = x + (SLOTSZ - 16) / 2;
            int iy = y + (SLOTSZ - 16) / 2;
            g.renderItem(stack, ix, iy);
            g.renderItemDecorations(this.font, stack, ix, iy);
        }
        if (locked) {
            g.fill(x + 1, y + 1, x + SLOTSZ - 1, y + SLOTSZ - 1, 0xAA0A0A0F);
            // small padlock glyph (gold)
            g.drawCenteredString(this.font, "\u26BF", x + SLOTSZ / 2, y + SLOTSZ / 2 - 4, 0xFF000000 | GOLD);
        }
    }

    /** A diagonal bright band that travels across a claimable card. */
    private void sheenSweep(GuiGraphics g, int x, int y, int w, int h, long t, int seed) {
        g.enableScissor(x, y, x + w, y + h);
        int period = 2600;
        int p = (int) ((t + seed * 220L) % period);
        float f = p / (float) period;
        int sweepX = x - h + Math.round(f * (w + h * 2));
        for (int row = 0; row < h; row++) {
            int sx = sweepX + row; // diagonal
            g.fill(sx, y + row, sx + 6, y + row + 1, 0x22FFFFFF);
            g.fill(sx + 6, y + row, sx + 9, y + row + 1, 0x11FFFFFF);
        }
        g.disableScissor();
    }

    private void drawCheck(GuiGraphics g, int x, int y, long t) {
        // small green badge with check
        softGlow(g, x, y, 10, 10, 2, 0x44, 0x55FF66);
        roundRect(g, x, y, 11, 11, 3, 0xFF1E7A2E);
        roundRing(g, x, y, 11, 11, 3, 1, 0xFF55FF66);
        g.drawString(this.font, "\u2714", x + 2, y + 2, 0xFFEFFFEF, false);
    }

    // ============================================================== detail

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY, long t) {
        int x = pX;
        int y = railBottom + 10;
        int w = pW;
        int h = pY + pH - y;
        if (h < 44) return;

        glassPanel(g, x, y, w, h, 7);
        accentLine(g, x + 8, y + 3, w - 16, t + 600);

        int pad = 14;
        TierDefinition def = pass.getTier(selectedTier);

        // title
        g.drawString(this.font, "\u00a7l\u00a7fTIER " + selectedTier,
                x + pad, y + pad, 0xFFFFFFFF, true);
        // rank reward chip
        if (def != null && def.hasRankReward()) {
            String rank = "\u2756 " + def.getRankReward().getRankDisplayText();
            int rw = this.font.width(rank) + 10;
            roundRect(g, x + pad + 64, y + pad - 3, rw, 13, 4, 0xAA2A0E33);
            roundRing(g, x + pad + 64, y + pad - 3, rw, 13, 4, 1, 0xFFDD88FF);
            g.drawString(this.font, "\u00a7d" + rank, x + pad + 69, y + pad, 0xFFDD88FF, true);
        }

        // free / premium reward rows
        g.drawString(this.font, "\u00a7fFREE", x + pad, y + pad + 18, 0xFFFFFFFF, true);
        drawRewardRow(g, def, x + pad + 52, y + pad + 14, false);
        g.drawString(this.font, "\u00a76PREMIUM", x + pad, y + pad + 40, 0xFF000000 | GOLD, true);
        drawRewardRow(g, def, x + pad + 52, y + pad + 36, true);

        // ---- buttons ----
        boolean claimed = selectedTier <= data.getCurrentTier() && data.isTierClaimed(selectedTier);
        boolean unlocked = selectedTier <= data.getCurrentTier();
        claimEnabled = unlocked && !claimed;

        claimW = 116; claimH = 26;
        claimX = x + w - claimW - pad;
        claimY = y + h - claimH - pad;
        claimHover = inside(mouseX, mouseY, claimX, claimY, claimW, claimH);

        String claimLabel;
        int c1, c2, cEdge, cText;
        if (!unlocked) {
            claimLabel = "\u26BF BLOQUEADO";
            c1 = 0xFF2A2F3A; c2 = 0xFF1A1E26; cEdge = 0xFF3C4250; cText = 0xFF8C92A0;
        } else if (claimed) {
            claimLabel = "\u2714 RECLAMADO";
            c1 = 0xFF24402A; c2 = 0xFF16281A; cEdge = 0xFF3E7A48; cText = 0xFFB7E6BF;
        } else {
            float pulse = (float) ((Math.sin(t / 280.0) + 1) / 2);
            if (claimHover) { c1 = 0xFF00F0FF; c2 = 0xFF0088CC; }
            else { c1 = lerpColor(0xFF00C8E0, 0xFF00E5FF, pulse) | 0xFF000000; c2 = 0xFF0077B0; }
            cEdge = 0xFF000000 | CYAN; cText = 0xFF06222B;
            claimLabel = "RECLAMAR";
            softGlow(g, claimX, claimY, claimW, claimH, 5, lerpAlpha(0x30, 0x70, pulse), CYAN);
        }
        drawButton(g, claimX, claimY, claimW, claimH, claimLabel, c1, c2, cEdge, cText, claimHover && claimEnabled);

        closeW = 90; closeH = 24;
        closeX = x + pad;
        closeY = y + h - closeH - pad;
        closeHover = inside(mouseX, mouseY, closeX, closeY, closeW, closeH);
        int e1 = closeHover ? 0xFF3A2030 : 0xFF2A1822;
        drawButton(g, closeX, closeY, closeW, closeH, "CERRAR",
                e1, 0xFF1A0E14, 0xFFCC4466, 0xFFF0C0CC, closeHover);
    }

    private void drawRewardRow(GuiGraphics g, TierDefinition def, int x, int y, boolean premium) {
        if (def == null) return;
        java.util.List<ItemStack> items = premium ? def.getPremiumRewards() : def.getFreeRewards();
        java.util.List<String> cmds = premium ? def.getPremiumCommands() : def.getFreeCommands();
        int cx = x;
        for (ItemStack s : items) {
            if (s.isEmpty()) continue;
            // mini slot behind each item
            roundRect(g, cx - 2, y - 2, 20, 20, 3, premium ? 0xAA3A2A00 : 0xAA052830);
            roundRing(g, cx - 2, y - 2, 20, 20, 3, 1, premium ? (0xFF000000 | GOLD_DK) : (0xFF000000 | CYAN_DK));
            g.renderItem(s, cx, y);
            g.renderItemDecorations(this.font, s, cx, y);
            cx += 22;
        }
        if (!cmds.isEmpty()) {
            String c = "\u00a7b+" + cmds.size() + " cmd";
            g.drawString(this.font, c, cx + 2, y + 5, 0xFF66DDFF, true);
            cx += this.font.width(c) + 6;
        }
        if (items.stream().allMatch(ItemStack::isEmpty) && cmds.isEmpty()) {
            g.drawString(this.font, "\u00a78\u2014", x, y + 5, 0xFF6A6A74, false);
        }
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label,
                            int top, int bot, int edge, int textCol, boolean hover) {
        if (hover) { top = brighten(top, 0.12f); bot = brighten(bot, 0.12f); }
        roundRectGrad(g, x, y, w, h, 5, top, bot);
        roundRing(g, x, y, w, h, 5, 1, edge);
        g.fill(x + 4, y + 2, x + w - 4, y + 3, 0x22FFFFFF); // top sheen
        int lw = this.font.width(label);
        g.drawString(this.font, "\u00a7l" + label, x + (w - lw) / 2, y + (h - 8) / 2, textCol, false);
    }

    // ============================================================== draw helpers

    private void glassPanel(GuiGraphics g, int x, int y, int w, int h, int r) {
        roundRectGrad(g, x, y, w, h, r, 0xE6121826, 0xF00A0E16);
        roundRing(g, x, y, w, h, r, 1, 0x3300E5FF);
        roundRing(g, x, y, w, h, r, 1, 0x11FFFFFF);
    }

    private static int cornerInset(int r, int row, int h) {
        if (row < r) {
            int dy = r - 1 - row;
            return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
        } else if (row >= h - r) {
            int dy = row - (h - r);
            return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
        }
        return 0;
    }

    private void roundRect(GuiGraphics g, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = cornerInset(r, row, h);
            g.fill(x + in, y + row, x + w - in, y + row + 1, argb);
        }
    }

    /** Vertical gradient rounded rect. */
    private void roundRectGrad(GuiGraphics g, int x, int y, int w, int h, int r, int topArgb, int botArgb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = cornerInset(r, row, h);
            float f = h <= 1 ? 0 : row / (float) (h - 1);
            int c = lerpArgb(topArgb, botArgb, f);
            g.fill(x + in, y + row, x + w - in, y + row + 1, c);
        }
    }

    /** Horizontal gradient rounded rect. */
    private void roundRectGradH(GuiGraphics g, int x, int y, int w, int h, int r, int leftArgb, int rightArgb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = cornerInset(r, row, h);
            g.fillGradient(x + in, y + row, x + w - in, y + row + 1, leftArgb, rightArgb);
        }
    }

    private void roundRing(GuiGraphics g, int x, int y, int w, int h, int r, int th, int argb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = cornerInset(r, row, h);
            if (row < th || row >= h - th) {
                g.fill(x + in, y + row, x + w - in, y + row + 1, argb);
            } else {
                g.fill(x + in, y + row, x + in + th, y + row + 1, argb);
                g.fill(x + w - in - th, y + row, x + w - in, y + row + 1, argb);
            }
        }
    }

    /** Soft glow: expanding translucent rounded rects of a colour around the element. */
    private void softGlow(GuiGraphics g, int x, int y, int w, int h, int layers, int maxAlpha, int rgb) {
        for (int i = layers; i >= 1; i--) {
            int a = (maxAlpha * (layers - i + 1)) / (layers * 2);
            int argb = (a << 24) | (rgb & 0xFFFFFF);
            int e = i * 2;
            roundRect(g, x - e, y - e, w + 2 * e, h + 2 * e, 5 + e, argb);
        }
    }

    // ============================================================== colour math

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8)
                | Math.round(ab + (bb - ab) * t);
    }

    private static int lerpArgb(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * t) << 24)
                | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8)
                | Math.round(ab + (bb - ab) * t);
    }

    private static int lerpAlpha(int a0, int a1, float t) {
        return Math.round(a0 + (a1 - a0) * t);
    }

    private static int darken(int argb, float f) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int gg = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static int brighten(int argb, float f) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * (1 + f)));
        int gg = Math.min(255, (int) (((argb >> 8) & 0xFF) * (1 + f)));
        int b = Math.min(255, (int) ((argb & 0xFF) * (1 + f)));
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    // ============================================================== input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, closeX, closeY, closeW, closeH)) {
                onClose();
                return true;
            }
            if (claimEnabled && inside(mouseX, mouseY, claimX, claimY, claimW, claimH)) {
                claimSelected();
                return true;
            }
            if (mouseX >= railX && mouseX <= railX + railWidth
                    && mouseY >= railY - 4 && mouseY <= railY + CARD_H + 4) {
                int rel = (int) (mouseX - railX + scrollX);
                if (rel >= 0) {
                    int tier = rel / SLOT + 1;
                    if (rel % SLOT <= CARD_W && tier >= 1 && tier <= PassDefinition.TIER_COUNT) {
                        selectedTier = tier;
                        return true;
                    }
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
