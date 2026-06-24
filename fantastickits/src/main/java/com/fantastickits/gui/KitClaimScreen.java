package com.fantastickits.gui;

import com.fantastickits.FantasticKits;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for claiming a kit.
 * Visual style matches FantasticCrates/FantasticSpawners:
 * - Dark themed background
 * - Preview of kit items (non-interactable)
 * - Prominent "Claim" button in action bar
 * - Clean, single-purpose layout
 */
public class KitClaimScreen extends AbstractContainerScreen<KitClaimMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FantasticKits.MOD_ID, "textures/gui/kit_claim.png");

    private final String kitName;
    private Button claimButton;

    public KitClaimScreen(KitClaimMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.kitName = menu.getKitName();
        this.imageWidth = 176;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Claim button (centered in action bar area)
        int btnX = this.leftPos + (this.imageWidth / 2) - 40;
        int btnY = this.topPos + 102;
        claimButton = Button.builder(Component.literal("§a§lCLAIM KIT"), b -> {
            // Trigger claim action via inventory button click (slot center of action bar)
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
            }
        }).bounds(btnX, btnY, 80, 18).build();

        this.addRenderableWidget(claimButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Main background (dark themed like FantasticCrates)
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF1A1A2E);

        // Border
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFF6A4AC4);
        graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF6A4AC4);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF6A4AC4);
        graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF6A4AC4);

        // Header bar
        graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + 14, 0xFF16213E);

        // Kit preview slots (4 rows)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = this.leftPos + 7 + col * 18;
                int y = this.topPos + 17 + row * 18;
                graphics.fill(x, y, x + 18, y + 18, 0xFF2A2A4A);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0F0F1F);
            }
        }

        // Action bar area with claim button background
        graphics.fill(this.leftPos + 4, this.topPos + 98, this.leftPos + this.imageWidth - 4, this.topPos + 122, 0xFF16213E);

        // Separator
        graphics.fill(this.leftPos + 4, this.topPos + 124, this.leftPos + this.imageWidth - 4, this.topPos + 125, 0xFF6A4AC4);

        // Player inventory backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = this.leftPos + 7 + col * 18;
                int y = this.topPos + 133 + row * 18;
                graphics.fill(x, y, x + 18, y + 18, 0xFF2A2A4A);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0F0F1F);
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            int x = this.leftPos + 7 + col * 18;
            int y = this.topPos + 191;
            graphics.fill(x, y, x + 18, y + 18, 0xFF2A2A4A);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0F0F1F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title in header (purple/gold themed like FantasticCrates)
        graphics.drawString(this.font, "§d§lKit: §f" + kitName, 8, 4, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xAAAAAA, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
