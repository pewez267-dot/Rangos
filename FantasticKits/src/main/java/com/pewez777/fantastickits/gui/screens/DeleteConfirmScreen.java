/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.gui.screens;

import com.pewez777.fantastickits.network.NetworkHandler;
import com.pewez777.fantastickits.network.packets.DeleteKitPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Confirmation screen shown before a kit is permanently deleted. Prevents
 * accidental deletions; the actual delete is only performed once the server
 * re-validates the resulting {@link DeleteKitPacket}.
 */
public final class DeleteConfirmScreen extends Screen {

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 150;

    private final String kitName;
    private final String ownerGroup;
    private int left;
    private int top;

    public DeleteConfirmScreen(String kitName, String ownerGroup) {
        super(Component.translatable("fantastickits.title.delete"));
        this.kitName = kitName == null ? "" : kitName;
        this.ownerGroup = ownerGroup == null ? "" : ownerGroup;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;

        int buttonY = top + PANEL_H - 34;
        addRenderableWidget(Button.builder(
                        Component.literal("Delete permanently").withStyle(ChatFormatting.RED),
                        b -> confirm())
                .bounds(left + 20, buttonY, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("fantastickits.button.cancel"),
                        b -> onClose())
                .bounds(left + PANEL_W - 170, buttonY, 150, 20)
                .build());
    }

    private void confirm() {
        NetworkHandler.sendToServer(new DeleteKitPacket(kitName));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0101018);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF55555F);
        graphics.fill(left, top, left + PANEL_W, top + 24, 0xFF1E1E2A);

        graphics.drawCenteredString(this.font,
                Component.literal("Confirm Kit Deletion").withStyle(ChatFormatting.RED),
                left + PANEL_W / 2, top + 8, 0xFFFFFF);

        graphics.drawCenteredString(this.font,
                Component.literal("Delete kit: " + kitName),
                left + PANEL_W / 2, top + 44, 0xFFE0E0E0);
        graphics.drawCenteredString(this.font,
                Component.literal("Owner group: " + (ownerGroup.isEmpty() ? "(none)" : ownerGroup)),
                left + PANEL_W / 2, top + 60, 0xFFB0B0B0);
        graphics.drawCenteredString(this.font,
                Component.literal("This also removes claims and LuckPerms nodes.")
                        .withStyle(ChatFormatting.GRAY),
                left + PANEL_W / 2, top + 80, 0xFFB0B0B0);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
