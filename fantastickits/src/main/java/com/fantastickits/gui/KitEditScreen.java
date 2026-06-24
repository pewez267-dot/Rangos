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
 * Client-side screen for the Kit Editor.
 * Replicates the FantasticCrates/FantasticSpawners visual style:
 * - Dark themed background with subtle borders
 * - Tab navigation along the top (Items, Group, Commands, NBT)
 * - Action buttons along the bottom action bar
 * - Clean, non-overlapping layout
 */
public class KitEditScreen extends AbstractContainerScreen<KitEditMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FantasticKits.MOD_ID, "textures/gui/kit_edit.png");

    // Tab identifiers
    private static final int TAB_ITEMS = 0;
    private static final int TAB_GROUP = 1;
    private static final int TAB_COMMANDS = 2;
    private static final int TAB_NBT = 3;

    private int currentTab = TAB_ITEMS;
    private final String kitName;

    // Tab buttons
    private Button tabItemsBtn;
    private Button tabGroupBtn;
    private Button tabCommandsBtn;
    private Button tabNbtBtn;

    // Action buttons
    private Button saveBtn;
    private Button closeBtn;

    public KitEditScreen(KitEditMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.kitName = menu.getKitName();
        this.imageWidth = 176;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int tabY = this.topPos - 20;
        int tabWidth = 42;
        int startX = this.leftPos + 2;

        // Tab navigation buttons (FantasticCrates style - top tabs)
        tabItemsBtn = Button.builder(Component.literal("Items"), b -> switchTab(TAB_ITEMS))
                .bounds(startX, tabY, tabWidth, 18).build();
        tabGroupBtn = Button.builder(Component.literal("Group"), b -> switchTab(TAB_GROUP))
                .bounds(startX + tabWidth + 2, tabY, tabWidth, 18).build();
        tabCommandsBtn = Button.builder(Component.literal("Cmds"), b -> switchTab(TAB_COMMANDS))
                .bounds(startX + (tabWidth + 2) * 2, tabY, tabWidth, 18).build();
        tabNbtBtn = Button.builder(Component.literal("NBT"), b -> switchTab(TAB_NBT))
                .bounds(startX + (tabWidth + 2) * 3, tabY, tabWidth, 18).build();

        this.addRenderableWidget(tabItemsBtn);
        this.addRenderableWidget(tabGroupBtn);
        this.addRenderableWidget(tabCommandsBtn);
        this.addRenderableWidget(tabNbtBtn);

        // Action bar buttons
        int actionY = this.topPos + 104;
        saveBtn = Button.builder(Component.literal("§aSave"), b -> {
            // Trigger save via clicking action slot 0 (server handles it)
            if (this.menu != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(this.leftPos + 8, actionY, 36, 16).build();

        closeBtn = Button.builder(Component.literal("§cClose"), b -> {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 8);
            this.onClose();
        }).bounds(this.leftPos + 132, actionY, 36, 16).build();

        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(closeBtn);

        updateTabVisibility();
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        // Update tab button active states (visual feedback)
        tabItemsBtn.active = currentTab != TAB_ITEMS;
        tabGroupBtn.active = currentTab != TAB_GROUP;
        tabCommandsBtn.active = currentTab != TAB_COMMANDS;
        tabNbtBtn.active = currentTab != TAB_NBT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw main background (dark themed like FantasticCrates)
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF1A1A2E);

        // Draw border (subtle highlight)
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFF4A4A6A);
        graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF4A4A6A);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF4A4A6A);
        graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF4A4A6A);

        // Draw slot backgrounds for kit item area
        if (currentTab == TAB_ITEMS || currentTab == TAB_NBT) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int x = this.leftPos + 7 + col * 18;
                    int y = this.topPos + 17 + row * 18;
                    graphics.fill(x, y, x + 18, y + 18, 0xFF2A2A4A);
                    graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0F0F1F);
                }
            }
        }

        // Action bar background
        graphics.fill(this.leftPos + 4, this.topPos + 100, this.leftPos + this.imageWidth - 4, this.topPos + 122, 0xFF2A2A4A);

        // Draw separator line between kit area and player inventory
        graphics.fill(this.leftPos + 4, this.topPos + 124, this.leftPos + this.imageWidth - 4, this.topPos + 125, 0xFF4A4A6A);

        // Draw player inventory slot backgrounds
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

        // Tab-specific overlays
        renderTabContent(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int contentX = this.leftPos + 8;
        int contentY = this.topPos + 20;

        switch (currentTab) {
            case TAB_ITEMS:
                // Items are rendered via normal slot rendering
                break;
            case TAB_GROUP:
                renderGroupTab(graphics, contentX, contentY);
                break;
            case TAB_COMMANDS:
                renderCommandsTab(graphics, contentX, contentY);
                break;
            case TAB_NBT:
                renderNbtTab(graphics, contentX, contentY);
                break;
        }
    }

    private void renderGroupTab(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 160, y + 70, 0xFF16213E);
        graphics.drawString(this.font, "§6Group Assignment", x + 4, y + 4, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7Kit: §f" + kitName, x + 4, y + 16, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7Click slot to assign group", x + 4, y + 28, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7Groups are read from LuckPerms", x + 4, y + 40, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7in real-time.", x + 4, y + 52, 0xAAAAAA, false);
    }

    private void renderCommandsTab(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 160, y + 70, 0xFF16213E);
        graphics.drawString(this.font, "§6Command Associations", x + 4, y + 4, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7Assign commands to this", x + 4, y + 16, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7kit's group. Players in the", x + 4, y + 28, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7group can use these commands.", x + 4, y + 40, 0xAAAAAA, false);
    }

    private void renderNbtTab(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 160, y + 70, 0xFF16213E);
        graphics.drawString(this.font, "§6NBT Editor", x + 4, y + 4, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7Click an item in the slots", x + 4, y + 16, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7above to edit its NBT data:", x + 4, y + 28, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7Name, Lore, Enchants,", x + 4, y + 40, 0xAAAAAA, false);
        graphics.drawString(this.font, "§7Attributes, Flags, CMD", x + 4, y + 52, 0xAAAAAA, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title with colored formatting (FantasticCrates style)
        graphics.drawString(this.font, "§6§lKit Editor: §f" + kitName, 8, 6, 0xFFFFFF, false);
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
