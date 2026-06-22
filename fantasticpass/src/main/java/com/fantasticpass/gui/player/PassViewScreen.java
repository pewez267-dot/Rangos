package com.fantasticpass.gui.player;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Player-facing Battle Pass screen: a progress header, a horizontally scrollable rail of
 * 100 tiers with per-state visuals, and a side detail panel with a working claim button.
 */
public class PassViewScreen extends Screen {

    private static final int SLOT_WIDTH = 44;
    private static final int SLOT_HEIGHT = 64;

    private final PassDefinition pass;
    private final PlayerPassData data;
    private final int minutesPerTier;

    private int railX;
    private int railY;
    private int railWidth;
    private int scrollX;
    private int maxScroll;

    private int panelX;
    private int panelY;
    private int panelWidth;

    private int selectedTier;
    private Button claimButton;

    public PassViewScreen(PassDefinition pass, PlayerPassData data, int minutesPerTier) {
        super(Component.translatable("fantasticpass.gui.view.title"));
        this.pass = pass;
        this.data = data;
        this.minutesPerTier = Math.max(1, minutesPerTier);
        this.selectedTier = Math.max(1, Math.min(PassDefinition.TIER_COUNT, data.getCurrentTier() == 0 ? 1 : data.getCurrentTier()));
    }

    @Override
    protected void init() {
        panelWidth = 150;
        panelX = this.width - panelWidth - 12;
        panelY = 70;

        railX = 12;
        railY = 84;
        railWidth = panelX - railX - 12;

        maxScroll = Math.max(0, PassDefinition.TIER_COUNT * SLOT_WIDTH - railWidth);
        scrollX = Math.max(0, Math.min(maxScroll, (selectedTier - 1) * SLOT_WIDTH - railWidth / 2));

        claimButton = addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.claim"), b -> claimSelected())
                .bounds(panelX + 10, panelY + 150, panelWidth - 20, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());

        updateClaimButton();
    }

    private void updateClaimButton() {
        boolean unlocked = selectedTier <= data.getCurrentTier();
        boolean claimed = data.isTierClaimed(selectedTier);
        claimButton.active = unlocked && !claimed;
        claimButton.setMessage(claimed
                ? Component.translatable("fantasticpass.gui.claimed")
                : (unlocked ? Component.translatable("fantasticpass.gui.claim")
                : Component.translatable("fantasticpass.gui.locked")));
    }

    private void claimSelected() {
        if (selectedTier <= data.getCurrentTier() && !data.isTierClaimed(selectedTier)) {
            PacketHandler.sendToServer(new ClaimTierPacket(selectedTier));
            // Optimistic local update; the server is authoritative and validated the claim.
            data.markClaimed(selectedTier);
            updateClaimButton();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawBackground(graphics, this.width, this.height);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFF00E5FF);

        renderHeader(graphics);
        renderRail(graphics, mouseX, mouseY);
        renderDetailPanel(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics) {
        int x = 12;
        int y = 36;
        int w = this.width - 24;
        GuiTheme.drawPanel(graphics, x, y, w, 30);

        int tier = data.getCurrentTier();
        graphics.drawString(this.font, Component.translatable("fantasticpass.gui.tier", tier),
                x + 8, y + 6, 0xFFFFFFFF, false);

        if (data.isPremium()) {
            Component badge = Component.translatable("fantasticpass.gui.premium");
            int bw = this.font.width(badge) + 10;
            graphics.fill(x + w - bw - 8, y + 4, x + w - 8, y + 16, 0xFF000000 | GuiTheme.ACCENT_GOLD_DIM);
            graphics.renderOutline(x + w - bw - 8, y + 4, bw, 12, 0xFF000000 | GuiTheme.ACCENT_GOLD);
            graphics.drawString(this.font, badge, x + w - bw - 3, y + 6, 0xFF000000 | GuiTheme.ACCENT_GOLD, false);
        }

        int minutesInto = data.getMinutesActive() - tier * minutesPerTier;
        if (minutesInto < 0) {
            minutesInto = 0;
        }
        int required = minutesPerTier;
        float progress = tier >= PassDefinition.TIER_COUNT ? 1.0F : Math.min(1.0F, (float) minutesInto / required);

        int barX = x + 8;
        int barY = y + 20;
        int barW = w - 16;
        graphics.fill(barX, barY, barX + barW, barY + 5, 0xFF202028);
        graphics.fill(barX, barY, barX + (int) (barW * progress), barY + 5, 0xFF000000 | GuiTheme.ACCENT_CYAN);
        graphics.renderOutline(barX, barY, barW, 5, 0xFF000000 | GuiTheme.BORDER);

        String minutesText = tier >= PassDefinition.TIER_COUNT
                ? "MAX"
                : minutesInto + " / " + required + " min";
        int mw = this.font.width(minutesText);
        graphics.drawString(this.font, minutesText, barX + barW - mw, y + 6, 0xFFAAAAAA, false);
    }

    private void renderRail(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiTheme.drawPanel(graphics, railX, railY, railWidth, SLOT_HEIGHT + 8);

        graphics.enableScissor(railX + 1, railY + 1, railX + railWidth - 1, railY + SLOT_HEIGHT + 7);
        for (int tier = 1; tier <= PassDefinition.TIER_COUNT; tier++) {
            int slotX = railX + 4 + (tier - 1) * SLOT_WIDTH - scrollX;
            if (slotX + SLOT_WIDTH < railX || slotX > railX + railWidth) {
                continue; // off-screen
            }
            renderTierSlot(graphics, tier, slotX, railY + 4);
        }
        graphics.disableScissor();
    }

    private void renderTierSlot(GuiGraphics graphics, int tier, int x, int y) {
        TierDefinition def = pass.getTier(tier);
        boolean claimed = data.isTierClaimed(tier);
        boolean unlocked = tier <= data.getCurrentTier();

        int border;
        if (claimed) {
            border = GuiTheme.SILVER;
        } else if (unlocked) {
            border = GuiTheme.cyanPulse();
        } else {
            border = GuiTheme.LOCKED;
        }

        int slotW = SLOT_WIDTH - 6;
        graphics.fill(x, y, x + slotW, y + SLOT_HEIGHT, claimed ? 0xFF15151D : 0xFF101018);
        if (tier == selectedTier) {
            graphics.renderOutline(x - 1, y - 1, slotW + 2, SLOT_HEIGHT + 2, 0xFF000000 | GuiTheme.ACCENT_GOLD);
        }
        graphics.renderOutline(x, y, slotW, SLOT_HEIGHT, 0xFF000000 | border);

        graphics.drawCenteredString(this.font, String.valueOf(tier), x + slotW / 2, y + 2, 0xFFFFFFFF);

        // Free reward icon.
        ItemStack freeIcon = def != null && !def.getFreeRewards().isEmpty()
                ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
        int iconX = x + slotW / 2 - 8;
        graphics.renderItem(freeIcon, iconX, y + 14);
        graphics.renderItemDecorations(this.font, freeIcon, iconX, y + 14);

        // Premium reward icon (with lock if not premium).
        ItemStack premiumIcon = def != null && !def.getPremiumRewards().isEmpty()
                ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;
        graphics.renderItem(premiumIcon, iconX, y + 34);
        graphics.renderItemDecorations(this.font, premiumIcon, iconX, y + 34);
        if (!premiumIcon.isEmpty() && !data.isPremium()) {
            graphics.fill(iconX, y + 34, iconX + 16, y + 50, 0x99000000);
            graphics.drawString(this.font, "\u26BF", iconX + 4, y + 38, 0xFF000000 | GuiTheme.ACCENT_GOLD, false);
        }

        if (claimed) {
            graphics.drawString(this.font, "\u2714", x + slotW - 8, y + 2, 0xFF55FF55, false);
        }
    }

    private void renderDetailPanel(GuiGraphics graphics) {
        GuiTheme.drawAccentPanel(graphics, panelX, panelY, panelWidth, 200, GuiTheme.ACCENT_CYAN);

        TierDefinition def = pass.getTier(selectedTier);
        int textX = panelX + 8;
        int y = panelY + 6;

        graphics.drawString(this.font, Component.translatable("fantasticpass.gui.tier", selectedTier),
                textX, y, 0xFF00E5FF, false);
        y += 14;

        graphics.drawString(this.font, Component.translatable("fantasticpass.gui.free_rewards"),
                textX, y, 0xFFC0C0C8, false);
        y += 12;
        List<ItemStack> freeItems = def == null ? Collections.emptyList() : def.getFreeRewards();
        List<String> freeCommands = def == null ? Collections.emptyList() : def.getFreeCommands();
        y = renderRewardLines(graphics, freeItems, freeCommands, textX, y);

        y += 4;
        graphics.drawString(this.font, Component.translatable("fantasticpass.gui.premium_rewards"),
                textX, y, 0xFF000000 | GuiTheme.ACCENT_GOLD, false);
        y += 12;
        List<ItemStack> premiumItems = def == null ? Collections.emptyList() : def.getPremiumRewards();
        List<String> premiumCommands = def == null ? Collections.emptyList() : def.getPremiumCommands();
        renderRewardLines(graphics, premiumItems, premiumCommands, textX, y);

        if (def != null && def.hasRankReward()) {
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.rank_reward")
                    .append(": ").append(def.getRankReward().getRankDisplayText()),
                    textX, panelY + 132, 0xFF00E5FF, false);
        }
    }

    private int renderRewardLines(GuiGraphics graphics, List<ItemStack> items, List<String> commands, int x, int y) {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            graphics.renderItem(stack, x, y - 1);
            graphics.renderItemDecorations(this.font, stack, x, y - 1);
            graphics.drawString(this.font, stack.getCount() + "x " + stack.getHoverName().getString(),
                    x + 20, y + 3, 0xFFFFFFFF, false);
            y += 18;
        }
        for (String command : commands) {
            graphics.drawString(this.font, "> " + command, x, y, 0xFFAAAAAA, false);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= railX && mouseX <= railX + railWidth
                && mouseY >= railY + 4 && mouseY <= railY + 4 + SLOT_HEIGHT) {
            int relative = (int) (mouseX - (railX + 4) + scrollX);
            int tier = relative / SLOT_WIDTH + 1;
            if (tier >= 1 && tier <= PassDefinition.TIER_COUNT) {
                selectedTier = tier;
                updateClaimButton();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= railX && mouseX <= railX + railWidth && mouseY >= railY && mouseY <= railY + SLOT_HEIGHT + 8) {
            scrollX = Math.max(0, Math.min(maxScroll, scrollX - (int) (delta * SLOT_WIDTH)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
