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

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Player-facing Battle Pass screen ({@code /fspass view}).
 *
 * Simple, flat layout modelled on the Jeqo "Battle Pass UI" resource pack. The custom
 * artwork ({@code pass_bg.png}) stays fully visible behind a very light dim. Tier rewards
 * are shown as a horizontally scrollable rail of flat Jeqo slot textures (free + premium
 * rows) with tier numbers and lock overlays. The progress bar is rendered as a green Jeqo
 * XP bar that fills with accumulated active time (presented as XP, not minutes). Claimable
 * slots gently pulse, hovered slots lift, and a small mute toggle sits in the top-right.
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
    private static final ResourceLocation BP_ICON = jeqo("bp");
    private static final ResourceLocation QUESTBOOK = jeqo("questbook");

    private static ResourceLocation jeqo(String n) {
        return new ResourceLocation("fantasticpass", JEQO + n + ".png");
    }

    private static final int CYAN = 0xFF00E5FF;
    private static final int GOLD = 0xFFFFD700;
    private static final int XP_PER_MINUTE = 10; // active time -> XP presentation

    // rail geometry
    private static final int SLOT_SIZE = 40;
    private static final int STRIDE = 52;
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
        this.numberY = centerY - SLOT_SIZE - ROW_GAP / 2 - 12;
        this.freeRowY = numberY + 12;
        this.premRowY = freeRowY + SLOT_SIZE + ROW_GAP;

        this.railX = 30;
        this.railWidth = this.width - 60;
        this.maxScroll = Math.max(0, PassDefinition.TIER_COUNT * STRIDE - railWidth);
        this.railTop = numberY;

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
        scrollX += (targetScrollX - scrollX) * 0.35f;
        if (Math.abs(targetScrollX - scrollX) < 0.4f) scrollX = targetScrollX;
        long t = System.currentTimeMillis();

        drawBackground(g);
        drawHeader(g, t);
        drawRail(g, mouseX, mouseY, t);
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
        // light dim only, so the (now sharpened) artwork stays visible and crisp
        g.fill(0, 0, this.width, this.height, 0x3C000000);
    }

    private void drawHeader(GuiGraphics g, long t) {
        int tier = data.getCurrentTier();
        int top = 14;

        // questbook icon + title
        String title = (pass.getName() == null || pass.getName().isEmpty())
                ? "BATTLE PASS" : pass.getName().toUpperCase();
        int titleW = (int) (this.font.width(title) * 1.6f);
        int titleX = this.width / 2 - titleW / 2;
        g.blit(QUESTBOOK, titleX - 22, top - 1, 16, 16, 0f, 0f, 16, 16, 16, 16);
        g.pose().pushPose();
        g.pose().translate(titleX, top, 0);
        g.pose().scale(1.6f, 1.6f, 1f);
        g.drawString(this.font, "\u00a7l" + title, 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();

        if (data.isPremium()) {
            String p = "\u2726 PREMIUM";
            int pw = this.font.width(p) + 12;
            int px = this.width - pw - 26;
            roundRect(g, px, top, pw, 16, 0xCC1A1206);
            g.renderOutline(px, top, pw, 16, GOLD);
            g.drawString(this.font, "\u00a7l" + p, px + 6, top + 4, GOLD, true);
        }

        // ---- XP bar (Jeqo green fill), fills with active time presented as XP ----
        int barW = Math.min(420, this.width - 120);
        int barX = (this.width - barW) / 2 + 10;
        int barY = top + 30;
        int barH = 12;

        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        int curXp = minutesInto * XP_PER_MINUTE;
        int tierXp = minutesPerTier * XP_PER_MINUTE;
        float frac = tier >= PassDefinition.TIER_COUNT ? 1f : Math.min(1f, curXp / (float) tierXp);

        // bp coin icon at the left of the bar
        g.blit(BP_ICON, barX - 22, barY - 2, 17, 16, 0f, 0f, 17, 9, 17, 9);

        // track
        roundRect(g, barX, barY, barW, barH, 0xCC04140A);
        g.renderOutline(barX, barY, barW, barH, 0x66FFFFFF);
        // Jeqo green xp fill stretched across the filled portion
        int fillW = Math.round((barW - 2) * frac);
        if (fillW > 0) {
            g.blit(XP_FILL, barX + 1, barY + 1, fillW, barH - 2, 0f, 0f, 2, 9, 2, 9);
            // moving highlight to show it's "charging"
            int sh = barX + 1 + (int) ((t / 14) % Math.max(1, fillW));
            g.fill(sh, barY + 1, Math.min(barX + 1 + fillW, sh + 2), barY + barH - 1, 0x55FFFFFF);
        }

        // labels: LVL + XP, no minutes
        g.drawString(this.font, "\u00a7l\u00a7fLVL " + tier, barX, barY - 11, 0xFFFFFFFF, true);
        String xpText = tier >= PassDefinition.TIER_COUNT ? "MAX" : curXp + " / " + tierXp + " XP";
        g.drawString(this.font, "\u00a7a" + xpText,
                barX + barW - this.font.width(xpText), barY - 11, 0xFF8CFF8C, true);
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY, long t) {
        g.drawString(this.font, "\u00a7fFREE", railX - 26, freeRowY + SLOT_SIZE / 2 - 4, 0xFFFFFFFF, true);
        g.drawString(this.font, "\u00a76PREM", railX - 26, premRowY + SLOT_SIZE / 2 - 4, GOLD, true);

        int hovered = tierAt(mouseX, mouseY);

        g.enableScissor(railX, railTop - 8, railX + railWidth, premRowY + SLOT_SIZE + 8);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int x = railX + (tier - 1) * STRIDE - Math.round(scrollX);
            if (x + SLOT_SIZE < railX || x > railX + railWidth) continue;
            drawTier(g, tier, x, tier == hovered, t);
        }
        g.disableScissor();
    }

    private void drawTier(GuiGraphics g, int tier, int x, boolean hovered, long t) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;
        boolean claimable = unlocked && !claimed;

        // subtle vertical lift: hover bounce + gentle pulse for claimable
        int lift = 0;
        if (hovered) lift += 3;
        if (claimable) lift += (int) Math.round(1.5 * (Math.sin(t / 320.0 + tier) + 1));

        int numCol = unlocked ? (claimed ? 0xFF9097A2 : 0xFFFFFFFF) : 0xFF6A6F79;
        g.drawCenteredString(this.font, String.valueOf(tier), x + SLOT_SIZE / 2, numberY, numCol);

        ItemStack free = (def != null && !def.getFreeRewards().isEmpty())
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        ItemStack prem = (def != null && !def.getPremiumRewards().isEmpty())
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;

        drawSlot(g, x, freeRowY - lift, free, false, claimed, unlocked, false, claimable, t, tier);
        boolean premLocked = !prem.isEmpty() && !data.isPremium();
        drawSlot(g, x, premRowY - lift, prem, true, claimed, unlocked, premLocked, claimable && !premLocked, t, tier);

        if (selected) {
            g.renderOutline(x - 2, freeRowY - lift - 2, SLOT_SIZE + 4,
                    (premRowY) - (freeRowY) + SLOT_SIZE + 4, GOLD);
        } else if (hovered) {
            g.renderOutline(x - 2, freeRowY - lift - 2, SLOT_SIZE + 4,
                    (premRowY) - (freeRowY) + SLOT_SIZE + 4, 0x66FFFFFF);
        }
    }

    private void drawSlot(GuiGraphics g, int x, int y, ItemStack stack, boolean premium,
                          boolean claimed, boolean unlocked, boolean premLocked,
                          boolean pulse, long t, int seed) {
        ResourceLocation tex;
        if (!unlocked) tex = SLOT_LOCKED;
        else if (claimed) tex = SLOT_CLAIMED;
        else if (premium) tex = SLOT_PREMIUM;
        else tex = SLOT_UNCLAIMED;

        // gentle brightness pulse on claimable slots
        if (pulse) {
            float b = 0.85f + 0.15f * (float) ((Math.sin(t / 300.0 + seed) + 1) / 2);
            RenderSystem.setShaderColor(b, b, b, 1f);
        }
        g.blit(tex, x, y, SLOT_SIZE, SLOT_SIZE, 0f, 0f, 16, 16, 16, 16);
        if (pulse) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

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
        g.drawString(this.font, info.toString(), 30, barY + 8, 0xFFFFFFFF, true);

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

    /** Tiny mute / unmute toggle in the very top-right corner. */
    private void drawMuteButton(GuiGraphics g, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, muteX, muteY, muteSize, muteSize);
        roundRect(g, muteX, muteY, muteSize, muteSize, hover ? 0xCC123040 : 0x990A1018);
        g.renderOutline(muteX, muteY, muteSize, muteSize, hover ? CYAN : 0x66FFFFFF);
        int cx = muteX + 3, cy = muteY + muteSize / 2;
        int col = PassMusicInstance.muted ? 0xFF9097A2 : 0xFF00E5FF;
        // speaker body
        g.fill(cx, cy - 1, cx + 2, cy + 1, col);
        g.fill(cx + 2, cy - 2, cx + 4, cy + 2, col);
        // sound waves or mute slash
        if (PassMusicInstance.muted) {
            for (int i = 0; i < 5; i++) g.fill(cx + 5 + i, cy - 2 + i, cx + 6 + i, cy - 1 + i, 0xFFFF5566);
        } else {
            g.fill(cx + 5, cy - 2, cx + 6, cy + 2, col);
            g.fill(cx + 7, cy - 3, cx + 8, cy + 3, col);
        }
    }

    // ============================================================ helpers

    private void roundRect(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x + 1, y, x + w - 1, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private int tierAt(double mx, double my) {
        if (my < railTop - 8 || my > premRowY + SLOT_SIZE + 8) return -1;
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
