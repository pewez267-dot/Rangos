package com.pewez.fantasticshortcuts.client;

import com.pewez.fantasticshortcuts.client.screen.ShortcutEditorScreen;
import com.pewez.fantasticshortcuts.network.OpenEditorPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only entry point for incoming packets. Loaded only on the physical client via
 * {@link net.minecraftforge.fml.DistExecutor}.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void openEditor(OpenEditorPacket packet) {
        Minecraft.getInstance().setScreen(
                new ShortcutEditorScreen(packet.getShortcuts(), packet.getActiveTab()));
    }
}
