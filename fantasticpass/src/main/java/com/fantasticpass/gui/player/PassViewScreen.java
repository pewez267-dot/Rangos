package com.fantasticpass.gui.player;

import com.fantasticpass.client.PassMusicInstance;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;

/**
 * Player Battle Pass screen ({@code /fspass view}).
 *
 * <p>Reuses the mod's shared {@link GuiTheme} (Valorant/Apex cyan-gold palette) and native
 * Minecraft {@link Button} widgets &mdash; the same components the admin screens use &mdash;
 * so the player view matches the rest of the mod instead of hand-drawn controls. The custom
 * {@code pass_bg.png} artwork stays visible behind a light dim. Rewards are two clearly
 * separated lanes (silver FREE on top, gold PREMIUM below) using the flat Jeqo slot
 * textures. The progress bar sits BELOW the reward lanes, styled with the theme accent.</p>
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

    private static ResourceLocation jeqo(String n) {
        return new ResourceLocation("fantasticpass", JEQO + n + ".png");
    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int XP_PER_MINUTE = 10;


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
    private int barX, barY, barW, barH;
    private float scrollX, targetScrollX;
    private int maxScroll;

    private Button claimButton;
    private Button muteButton;

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
        this.numberY = centerY - 78;
        this.freeRowY = numberY + 14;
        this.premRowY = freeRowY + SLOT_SIZE + LANE_GAP;
        this.railTop = numberY - 4;
        this.railBottom = premRowY + SLOT_SIZE + 4;

        this.railX = LEFT_GUTTER;
        this.railWidth = this.width - railX - 22;
        this.maxScroll = Math.max(0, PassDefinition.TIER_COUNT * STRIDE - railWidth);


        // progress bar BELOW the reward lanes
        this.barW = Math.min(440, this.width - 140);
        this.barX = (this.width - barW) / 2;
        this.barY = railBottom + 26;
        this.barH = 12;

        this.targetScrollX = clampScroll((selectedTier - 1) * STRIDE - railWidth / 2f + SLOT_SIZE / 2f);
        this.scrollX = targetScrollX;

        // ---- native Minecraft buttons (same look as the admin screens) ----
        int by = barY + barH + 16;
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.literal("\u25c0"), b -> selectTier(selectedTier - 1))
                .bounds(cx - 132, by, 22, 20).build());
        claimButton = addRenderableWidget(Button.builder(Component.literal("CLAIM"), b -> claimSelected())
                .bounds(cx - 105, by, 210, 20).build());
        addRenderableWidget(Button.builder(Component.literal("\u25b6"), b -> selectTier(selectedTier + 1))
                .bounds(cx + 110, by, 22, 20).build());

        muteButton = addRenderableWidget(Button.builder(muteLabel(), b -> toggleMute())
                .bounds(this.width - 30, 8, 22, 18).build());

        startMusic();
    }

    private Component muteLabel() {
        return Component.literal(PassMusicInstance.muted ? "\u00a77\u266a\u2715" : "\u00a7b\u266a");
    }

    private void toggleMute() {
        PassMusicInstance.muted = !PassMusicInstance.muted;
        muteButton.setMessage(muteLabel());
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    private void selectTier(int tier) {
        selectedTier = Math.max(1, Math.min(PassDefinition.TIER_COUNT, tier));
        targetScrollX = clampScroll((selectedTier - 1) * STRIDE - railWidth / 2f + SLOT_SIZE / 2f);
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
        drawProgressBar(g);
        updateClaimButton();
        super.render(g, mouseX, mouseY, partialTick); // draws native buttons on top
    }

    private void drawBackground(GuiGraphics g) {
        float scale = Math.max(this.width / (float) BG_W, this.height / (float) BG_H);
        int dw = Math.round(BG_W * scale);
        int dh = Math.round(BG_H * scale);
        g.blit(BG, (this.width - dw) / 2, (this.height - dh) / 2, dw, dh, 0f, 0f, BG_W, BG_H, BG_W, BG_H);
        g.fill(0, 0, this.width, this.height, 0x3C000000);
    }


    private void drawHeader(GuiGraphics g) {
        String title = (pass.getName() == null || pass.getName().isEmpty())
                ? "BATTLE PASS" : pass.getName().toUpperCase();
        g.pose().pushPose();
        g.pose().translate(this.width / 2f, 16, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawCenteredString(this.font, "\u00a7l" + title, 0, 0, 0xFF000000 | GuiTheme.ACCENT_CYAN);
        g.pose().popPose();

        if (data.isPremium()) {
            String p = "\u2726 PREMIUM";
            int pw = this.font.width(p) + 12;
            int px = 12;
            GuiTheme.drawAccentPanel(g, px, 10, pw, 16, GuiTheme.ACCENT_GOLD);
            g.drawString(this.font, "\u00a7l" + p, px + 6, 14, 0xFF000000 | GuiTheme.ACCENT_GOLD, false);
        }
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY) {
        drawLaneStrip(g, freeRowY, GuiTheme.SILVER, "FREE");
        drawLaneStrip(g, premRowY, GuiTheme.ACCENT_GOLD, "PREMIUM");

        int hovered = tierAt(mouseX, mouseY);
        g.enableScissor(railX - 4, railTop, railX + railWidth + 4, railBottom);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int x = railX + (tier - 1) * STRIDE - Math.round(scrollX);
            if (x + SLOT_SIZE < railX - 4 || x > railX + railWidth + 4) continue;
            drawTier(g, tier, x, tier == hovered);
        }
        g.disableScissor();
    }

    private void drawLaneStrip(GuiGraphics g, int rowY, int accentRgb, String label) {
        int x = railX - 4, w = railWidth + 8, y = rowY - 4, h = SLOT_SIZE + 8;
        g.fill(x, y, x + w, y + h, 0xCC000000 | GuiTheme.PANEL);
        g.fill(x, y, x + 2, y + h, 0xFF000000 | accentRgb);
        g.renderOutline(x, y, w, h, 0xFF000000 | GuiTheme.BORDER);
        int ly = rowY + (SLOT_SIZE - this.font.lineHeight) / 2;
        g.drawString(this.font, "\u00a7l" + label, 8, ly, 0xFF000000 | accentRgb, false);
    }


    private void drawTier(GuiGraphics g, int tier, int x, boolean hovered) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;
        boolean claimable = unlocked && !claimed;

        if (selected) {
            g.fill(x - 3, railTop, x + SLOT_SIZE + 3, railBottom, 0x2600E5FF);
            g.fill(x - 3, railTop, x + SLOT_SIZE + 3, railTop + 1, 0xFF000000 | GuiTheme.ACCENT_CYAN);
            g.fill(x - 3, railBottom - 1, x + SLOT_SIZE + 3, railBottom, 0xFF000000 | GuiTheme.ACCENT_CYAN);
        } else if (hovered) {
            g.fill(x - 3, railTop, x + SLOT_SIZE + 3, railBottom, 0x14FFFFFF);
        }

        int numCol = selected ? (0xFF000000 | GuiTheme.ACCENT_GOLD)
                : (unlocked ? (claimed ? 0xFF000000 | GuiTheme.TEXT_SECONDARY : WHITE) : 0xFF000000 | GuiTheme.LOCKED);
        g.drawCenteredString(this.font, (selected ? "\u00a7l" : "") + tier, x + SLOT_SIZE / 2, numberY, numCol);

        ItemStack free = (def != null && !def.getFreeRewards().isEmpty()) ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        ItemStack prem = (def != null && !def.getPremiumRewards().isEmpty()) ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;

        drawSlot(g, x, freeRowY, free, false, claimed, unlocked, false, claimable);
        boolean premLocked = !prem.isEmpty() && !data.isPremium();
        drawSlot(g, x, premRowY, prem, true, claimed, unlocked, premLocked, claimable && !premLocked);
    }

    private void drawSlot(GuiGraphics g, int x, int y, ItemStack stack, boolean premium,
                          boolean claimed, boolean unlocked, boolean premLocked, boolean claimable) {
        ResourceLocation tex = !unlocked ? SLOT_LOCKED : (claimed ? SLOT_CLAIMED : (premium ? SLOT_PREMIUM : SLOT_UNCLAIMED));
        g.blit(tex, x, y, SLOT_SIZE, SLOT_SIZE, 0f, 0f, 16, 16, 16, 16);

        if (!stack.isEmpty()) {
            int ix = x + (SLOT_SIZE - 16) / 2, iy = y + (SLOT_SIZE - 16) / 2;
            g.renderItem(stack, ix, iy);
            if (unlocked) g.renderItemDecorations(this.font, stack, ix, iy);
            else g.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x99000000);
        }
        if (premLocked || !unlocked) {
            g.blit(LOCK, x + (SLOT_SIZE - 16) / 2, y + (SLOT_SIZE - 16) / 2, 16, 16, 0f, 0f, 16, 16, 16, 16);
        }
        if (claimable) {
            int dx = x + SLOT_SIZE - 8, dy = y + 4;
            g.fill(dx - 1, dy - 1, dx + 5, dy + 5, 0xFF06222B);
            g.fill(dx, dy, dx + 4, dy + 4, 0xFF000000 | GuiTheme.ACCENT_CYAN);
        }
    }


    private void drawProgressBar(GuiGraphics g) {
        int tier = data.getCurrentTier();
        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        int curXp = minutesInto * XP_PER_MINUTE;
        int tierXp = minutesPerTier * XP_PER_MINUTE;
        float frac = tier >= PassDefinition.TIER_COUNT ? 1f : Math.min(1f, curXp / (float) tierXp);

        // track (theme panel) + accent gradient fill
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF000000 | GuiTheme.PANEL);
        g.renderOutline(barX, barY, barW, barH, 0xFF000000 | GuiTheme.BORDER);
        int fillW = Math.round((barW - 2) * frac);
        if (fillW > 0) {
            g.fillGradient(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1,
                    0xFF000000 | GuiTheme.ACCENT_CYAN, 0xFF000000 | GuiTheme.ACCENT_CYAN_DIM);
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 2, 0x66FFFFFF);
        }
        // subtle segment ticks
        for (int i = 1; i < 10; i++) {
            int tx = barX + 1 + (barW - 2) * i / 10;
            g.fill(tx, barY + 1, tx + 1, barY + barH - 1, 0x22FFFFFF);
        }

        g.drawString(this.font, "\u00a7lLVL " + tier, barX, barY - 11, 0xFF000000 | GuiTheme.ACCENT_GOLD, false);
        String xpText = tier >= PassDefinition.TIER_COUNT ? "MAX" : curXp + " / " + tierXp + " XP";
        g.drawString(this.font, xpText, barX + barW - this.font.width(xpText), barY - 11,
                0xFF000000 | GuiTheme.TEXT_SECONDARY, false);

        TierDefinition def = pass.getTier(selectedTier);
        StringBuilder info = new StringBuilder("\u00a7fTier " + selectedTier + "  ");
        if (def != null) {
            int items = def.getFreeRewards().size() + def.getPremiumRewards().size();
            int cmds = def.getFreeCommands().size() + def.getPremiumCommands().size();
            if (items > 0) info.append("\u00a77").append(items).append(" items  ");
            if (cmds > 0) info.append("\u00a7b").append(cmds).append(" cmd  ");
            if (def.hasRankReward()) info.append("\u00a7d\u2756 ").append(def.getRankReward().getRankDisplayText());
        }
        g.drawCenteredString(this.font, info.toString(), this.width / 2, barY + barH + 4, WHITE);
    }


    private void updateClaimButton() {
        boolean unlocked = selectedTier <= data.getCurrentTier();
        boolean claimed = data.isTierClaimed(selectedTier);
        if (!unlocked) {
            claimButton.setMessage(Component.literal("\u00a77TIER LOCKED"));
            claimButton.active = false;
        } else if (claimed) {
            claimButton.setMessage(Component.literal("\u00a7aCLAIMED"));
            claimButton.active = false;
        } else {
            claimButton.setMessage(Component.literal("\u00a7lCLAIM TIER " + selectedTier));
            claimButton.active = true;
        }
    }

    // ============================================================ input

    private int tierAt(double mx, double my) {
        if (my < railTop || my > railBottom) return -1;
        if (mx < railX - 4 || mx > railX + railWidth + 4) return -1;
        int rel = (int) (mx - railX + scrollX);
        if (rel < 0) return -1;
        int tier = rel / STRIDE + 1;
        if (rel % STRIDE <= SLOT_SIZE && tier >= 1 && tier <= PassDefinition.TIER_COUNT) return tier;
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            int tier = tierAt(mouseX, mouseY);
            if (tier > 0) {
                selectTier(tier);
                return true;
            }
        }
        return false;
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
