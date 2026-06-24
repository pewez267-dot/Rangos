package com.fantasticpass.gui.player;

import com.fantasticpass.client.PassMusicInstance;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.gui.widgets.ThemedButton;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Player Battle Pass screen ({@code /fspass view}).
 *
 * <p>Built directly on the Jeqo "Battle Pass UI" artwork that ships in the repo's
 * <em>Crates and Stuff Model Pack</em>: {@code rewards.png} is blitted as the real GUI
 * frame (header bar, two recessed reward strips, progress band, footer) and the reward
 * slots use the pack's {@code reward_*}/{@code lock} textures. Because Jeqo's frame is a
 * fixed ~10-slot page, the mod's 100 tiers are <strong>paginated</strong> (10 per page,
 * like the pack's "week" pages) instead of a free-scroll rail.</p>
 *
 * <p>Layout coordinates below are in the texture's native 192x170 space and are mapped to
 * screen pixels through {@link #sx(int)} / {@link #sy(int)} using an integer scale so the
 * pixel art stays crisp.</p>
 */
public class PassViewScreen extends Screen {

    // ---- Jeqo battlepass textures (192x170 frame + 16px reward icons) ----
    private static final ResourceLocation FRAME = jeqo("rewards");
    private static final ResourceLocation SLOT_UNCLAIMED = jeqo("slot_unclaimed");
    private static final ResourceLocation SLOT_CLAIMED = jeqo("slot_claimed");
    private static final ResourceLocation SLOT_LOCKED = jeqo("slot_locked");
    private static final ResourceLocation SLOT_PREMIUM = jeqo("slot_premium");
    private static final ResourceLocation LOCK = jeqo("lock");

    private static ResourceLocation jeqo(String n) {
        return new ResourceLocation("fantasticpass", "textures/gui/jeqo/" + n + ".png");
    }

    private static final int FRAME_W = 192, FRAME_H = 170;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int XP_PER_MINUTE = 10;

    // native-space layout (measured from rewards.png)
    private static final int CONTENT_X = 16;     // left edge of the 10-column reward grid
    private static final int SLOT = 16;          // native slot size
    private static final int COLS = 10;          // tiers per page (100 tiers -> 10 pages)
    private static final int FREE_ROW_Y = 56;    // top of FREE reward strip
    private static final int PREM_ROW_Y = 88;    // top of PREMIUM reward strip
    private static final int BAR_Y = 120;        // progress bar top
    private static final int BAR_H = 8;
    private static final int FOOTER_Y = 145;     // controls row

    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int minutesPerTier;

    private int scale = 2;
    private int left, top;

    private int page;                 // 0-based page index
    private int selectedTier;         // 1..TIER_COUNT
    private final int pageCount;

    private ThemedButton prevButton;
    private ThemedButton nextButton;
    private ThemedButton claimButton;
    private ThemedButton muteButton;

    private PassMusicInstance music;
    private float pulse;

    public PassViewScreen(PassDefinition pass, PlayerPassData data, int minutesPerTier) {
        super(Component.translatable("fantasticpass.gui.view.title"));
        this.pass = pass;
        this.data = data;
        this.minutesPerTier = Math.max(1, minutesPerTier);
        this.pageCount = (PassDefinition.TIER_COUNT + COLS - 1) / COLS;
        int cur = data.getCurrentTier();
        this.selectedTier = Math.max(1, Math.min(PassDefinition.TIER_COUNT, cur == 0 ? 1 : cur));
        this.page = (selectedTier - 1) / COLS;
    }

    @Override
    protected void init() {
        // integer scale that fits the screen with margins, clamped for crisp pixels
        int s = Math.min((this.width - 40) / FRAME_W, (this.height - 50) / FRAME_H);
        this.scale = Math.max(2, Math.min(4, s));
        this.left = (this.width - FRAME_W * scale) / 2;
        this.top = (this.height - FRAME_H * scale) / 2;

        int navW = SLOT * scale + 4;
        prevButton = addRenderableWidget(new ThemedButton(
                sx(CONTENT_X), sy(FOOTER_Y), navW, 18,
                Component.literal("\u25c0"), GuiTheme.ACCENT_CYAN, b -> changePage(-1)));

        nextButton = addRenderableWidget(new ThemedButton(
                sx(CONTENT_X + COLS * SLOT) - navW, sy(FOOTER_Y), navW, 18,
                Component.literal("\u25b6"), GuiTheme.ACCENT_CYAN, b -> changePage(1)));

        int claimX = sx(CONTENT_X) + navW + 6;
        int claimW = (sx(CONTENT_X + COLS * SLOT) - navW - 6) - claimX;
        claimButton = addRenderableWidget(new ThemedButton(
                claimX, sy(FOOTER_Y), claimW, 18,
                Component.literal("CLAIM"), GuiTheme.ACCENT_GOLD, b -> claimSelected()));

        muteButton = addRenderableWidget(new ThemedButton(
                this.width - 30, 8, 24, 18,
                muteLabel(), GuiTheme.ACCENT_CYAN, b -> toggleMute()));

        startMusic();
    }

    // native -> screen helpers
    private int sx(int nx) {
        return left + nx * scale;
    }

    private int sy(int ny) {
        return top + ny * scale;
    }

    private Component muteLabel() {
        return Component.literal(PassMusicInstance.muted ? "\u00a77\u266a\u2715" : "\u00a7b\u266a");
    }

    private void toggleMute() {
        PassMusicInstance.muted = !PassMusicInstance.muted;
        muteButton.setMessage(muteLabel());
        click(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
    }

    private void changePage(int delta) {
        int np = Math.max(0, Math.min(pageCount - 1, page + delta));
        if (np != page) {
            page = np;
            int t = page * COLS + 1;
            if (selectedTier < t || selectedTier > t + COLS - 1) selectedTier = t;
            click(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        }
    }

    private void click(net.minecraft.sounds.SoundEvent e, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch));
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

    // ============================================================ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        pulse += partialTick * 0.12f;
        this.renderBackground(g);

        // Jeqo frame
        g.blit(FRAME, left, top, FRAME_W * scale, FRAME_H * scale, 0f, 0f, FRAME_W, FRAME_H, FRAME_W, FRAME_H);

        drawHeader(g);
        drawRewardGrid(g, mouseX, mouseY);
        drawProgress(g);
        updateClaimButton();
        super.render(g, mouseX, mouseY, partialTick); // themed buttons + tooltips
    }

    private void drawHeader(GuiGraphics g) {
        String title = (pass.getName() == null || pass.getName().isEmpty())
                ? "BATTLE PASS" : pass.getName().toUpperCase();
        g.pose().pushPose();
        g.pose().translate(this.width / 2f, sy(20), 0);
        float ts = Math.max(1f, scale * 0.66f);
        g.pose().scale(ts, ts, 1f);
        g.drawCenteredString(this.font, "\u00a7l" + title, 0, 0, 0xFF000000 | GuiTheme.ACCENT_CYAN);
        g.pose().popPose();

        if (data.isPremium()) {
            String p = "\u00a7l\u2726 PREMIUM";
            g.drawString(this.font, p, sx(CONTENT_X), sy(8), 0xFF000000 | GuiTheme.ACCENT_GOLD, true);
        }
    }

    private void drawRewardGrid(GuiGraphics g, int mouseX, int mouseY) {
        // lane labels in the left gutter
        drawRotatedLabel(g, "FREE", FREE_ROW_Y, GuiTheme.SILVER);
        drawRotatedLabel(g, "PREM", PREM_ROW_Y, GuiTheme.ACCENT_GOLD);

        int hovered = tierAt(mouseX, mouseY);
        int base = page * COLS;
        for (int c = 0; c < COLS; c++) {
            int tier = base + c + 1;
            if (tier > PassDefinition.TIER_COUNT) break;
            int nx = CONTENT_X + c * SLOT;
            drawColumn(g, tier, nx, tier == hovered);
        }
    }

    private void drawRotatedLabel(GuiGraphics g, String text, int rowNy, int rgb) {
        // small label centered vertically on a row, drawn just left of the grid
        int x = sx(CONTENT_X) - 2 - this.font.width(text);
        if (x < 2) x = 2;
        int y = sy(rowNy + SLOT / 2) - this.font.lineHeight / 2;
        g.drawString(this.font, "\u00a7l" + text, x, y, 0xFF000000 | rgb, true);
    }

    private void drawColumn(GuiGraphics g, int tier, int nx, boolean hovered) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();
        boolean selected = tier == selectedTier;
        boolean claimable = unlocked && !claimed;

        int x0 = sx(nx);
        int topY = sy(FREE_ROW_Y) - scale * 2;
        int botY = sy(PREM_ROW_Y + SLOT) + scale * 2;

        if (selected) {
            int a = 0xFF000000 | GuiTheme.ACCENT_CYAN;
            g.renderOutline(x0 - scale, topY, SLOT * scale + scale * 2, botY - topY, a);
            g.fill(x0 - scale, topY, x0 + SLOT * scale + scale, topY + scale, a);
            g.fill(x0 - scale, botY - scale, x0 + SLOT * scale + scale, botY, a);
        } else if (hovered) {
            g.fill(x0 - scale, topY, x0 + SLOT * scale + scale, botY, 0x18FFFFFF);
        }

        // tier number between the two rows
        int numCol = selected ? GuiTheme.ACCENT_GOLD : (unlocked ? (claimed ? GuiTheme.TEXT_SECONDARY : 0xFFFFFF) : GuiTheme.LOCKED);
        g.drawCenteredString(this.font, String.valueOf(tier),
                x0 + SLOT * scale / 2, sy(FREE_ROW_Y + SLOT + 3), 0xFF000000 | numCol);

        ItemStack free = (def != null && !def.getFreeRewards().isEmpty()) ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        ItemStack prem = (def != null && !def.getPremiumRewards().isEmpty()) ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;

        drawSlot(g, nx, FREE_ROW_Y, free, false, claimed, unlocked, false, claimable);
        boolean premLocked = !prem.isEmpty() && !data.isPremium();
        drawSlot(g, nx, PREM_ROW_Y, prem, true, claimed, unlocked, premLocked, claimable && !premLocked);
    }

    private void drawSlot(GuiGraphics g, int nx, int ny, ItemStack stack, boolean premium,
                          boolean claimed, boolean unlocked, boolean premLocked, boolean claimable) {
        int x = sx(nx), y = sy(ny), sz = SLOT * scale;
        ResourceLocation tex = !unlocked ? SLOT_LOCKED
                : (claimed ? SLOT_CLAIMED : (premium ? SLOT_PREMIUM : SLOT_UNCLAIMED));
        g.blit(tex, x, y, sz, sz, 0f, 0f, 16, 16, 16, 16);

        if (!stack.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1f);
            g.renderItem(stack, 0, 0);
            if (unlocked) g.renderItemDecorations(this.font, stack, 0, 0);
            g.pose().popPose();
            if (!unlocked) g.fill(x + scale, y + scale, x + sz - scale, y + sz - scale, 0x88000000);
        }
        if (premLocked || !unlocked) {
            int lx = x + (sz - 16 * scale) / 2, ly = y + (sz - 16 * scale) / 2;
            g.blit(LOCK, lx, ly, 16 * scale, 16 * scale, 0f, 0f, 16, 16, 16, 16);
        }
        if (claimable) {
            float a = 0.55f + 0.45f * (float) Math.abs(Math.sin(pulse));
            int alpha = (int) (a * 255) << 24;
            g.fill(x, y, x + sz, y + scale, alpha | (GuiTheme.ACCENT_CYAN & 0xFFFFFF));
        }
    }

    private void drawProgress(GuiGraphics g) {
        int tier = data.getCurrentTier();
        int minutesInto = Math.max(0, data.getMinutesActive() - tier * minutesPerTier);
        int curXp = minutesInto * XP_PER_MINUTE;
        int tierXp = minutesPerTier * XP_PER_MINUTE;
        float frac = tier >= PassDefinition.TIER_COUNT ? 1f : Math.min(1f, curXp / (float) tierXp);

        int bx = sx(CONTENT_X), bw = COLS * SLOT * scale, by = sy(BAR_Y), bh = BAR_H * scale / 2;
        if (bh < 6) bh = 6;

        g.drawString(this.font, "\u00a7lLVL " + tier, bx, by - 11, 0xFF000000 | GuiTheme.ACCENT_GOLD, true);
        String xp = tier >= PassDefinition.TIER_COUNT ? "MAX" : curXp + " / " + tierXp + " XP";
        g.drawString(this.font, xp, bx + bw - this.font.width(xp), by - 11, 0xFF000000 | GuiTheme.TEXT_SECONDARY, true);
        String pg = "PAGE " + (page + 1) + "/" + pageCount;
        g.drawCenteredString(this.font, pg, bx + bw / 2, by - 11, 0xFF000000 | GuiTheme.ACCENT_CYAN);

        g.fill(bx, by, bx + bw, by + bh, 0xFF000000 | GuiTheme.BACKGROUND);
        g.renderOutline(bx, by, bw, bh, 0xFF000000 | GuiTheme.BORDER);
        int fw = Math.round((bw - 2) * frac);
        if (fw > 0) {
            g.fillGradient(bx + 1, by + 1, bx + 1 + fw, by + bh - 1,
                    0xFF000000 | GuiTheme.ACCENT_CYAN, 0xFF000000 | GuiTheme.ACCENT_CYAN_DIM);
            g.fill(bx + 1, by + 1, bx + 1 + fw, by + 2, 0x66FFFFFF);
        }

        // selected tier summary
        TierDefinition def = pass.getTier(selectedTier);
        StringBuilder info = new StringBuilder("\u00a7fTIER " + selectedTier + "  ");
        if (def != null) {
            int items = def.getFreeRewards().size() + def.getPremiumRewards().size();
            int cmds = def.getFreeCommands().size() + def.getPremiumCommands().size();
            if (items > 0) info.append("\u00a77").append(items).append(" items  ");
            if (cmds > 0) info.append("\u00a7b").append(cmds).append(" cmd  ");
            if (def.hasRankReward()) info.append("\u00a7d\u2756 ").append(def.getRankReward().getRankDisplayText());
        }
        g.drawCenteredString(this.font, info.toString(), bx + bw / 2, by + bh + 4, WHITE);
    }

    private void updateClaimButton() {
        boolean unlocked = selectedTier <= data.getCurrentTier();
        boolean claimed = data.isTierClaimed(selectedTier);
        if (!unlocked) {
            claimButton.setMessage(Component.literal("\u00a77TIER LOCKED"));
            claimButton.setAccent(GuiTheme.BORDER);
            claimButton.active = false;
        } else if (claimed) {
            claimButton.setMessage(Component.literal("\u00a7aCLAIMED \u2714"));
            claimButton.setAccent(0x55FF55);
            claimButton.active = false;
        } else {
            claimButton.setMessage(Component.literal("\u00a7lCLAIM TIER " + selectedTier));
            claimButton.setAccent(GuiTheme.ACCENT_GOLD);
            claimButton.active = true;
        }
        prevButton.active = page > 0;
        nextButton.active = page < pageCount - 1;
    }

    // ============================================================ input

    private int tierAt(double mx, double my) {
        int gridTop = sy(FREE_ROW_Y) - scale * 2;
        int gridBot = sy(PREM_ROW_Y + SLOT) + scale * 2;
        if (my < gridTop || my > gridBot) return -1;
        int relX = (int) (mx - sx(CONTENT_X));
        if (relX < 0) return -1;
        int col = relX / (SLOT * scale);
        if (col < 0 || col >= COLS) return -1;
        int tier = page * COLS + col + 1;
        return tier <= PassDefinition.TIER_COUNT ? tier : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            int tier = tierAt(mouseX, mouseY);
            if (tier > 0) {
                selectedTier = tier;
                click(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f);
                return true;
            }
        }
        return false;
    }

    private void claimSelected() {
        if (selectedTier <= data.getCurrentTier() && !data.isTierClaimed(selectedTier)) {
            PacketHandler.sendToServer(new ClaimTierPacket(selectedTier));
            data.markClaimed(selectedTier);
            click(SoundEvents.PLAYER_LEVELUP, 0.7f);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        changePage(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
