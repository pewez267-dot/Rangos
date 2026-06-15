/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.gui.client;

import com.pewez777.fantastickits.gui.screens.DeleteConfirmScreen;
import com.pewez777.fantastickits.gui.screens.KitEditorScreen;
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.network.packets.OpenDeleteConfirmPacket;
import com.pewez777.fantastickits.network.packets.OpenEditorPacket;

import net.minecraft.client.Minecraft;

/**
 * Client-only entry point invoked (via {@code DistExecutor}) when the server
 * asks the client to open a screen. This class lives in the client package and
 * is never class-loaded on a dedicated server.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void openEditor(OpenEditorPacket msg) {
        Kit kit = Kit.fromNbt(msg.getKitTag());
        EditorContext context = new EditorContext(
                msg.isEditMode(),
                msg.isLuckPermsAvailable(),
                kit,
                msg.getGroups(),
                msg.getCommandCatalog());
        Minecraft.getInstance().setScreen(new KitEditorScreen(context));
    }

    public static void openDeleteConfirm(OpenDeleteConfirmPacket msg) {
        Minecraft.getInstance().setScreen(new DeleteConfirmScreen(msg.getKitName(), msg.getOwnerGroup()));
    }
}
