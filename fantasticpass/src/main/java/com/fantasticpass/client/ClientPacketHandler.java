package com.fantasticpass.client;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.admin.PassAdminScreen;
import com.fantasticpass.gui.player.PassViewScreen;
import com.fantasticpass.nametag.ClientNametagCache;
import com.fantasticpass.network.NametagUpdatePacket;
import com.fantasticpass.network.OpenAdminScreenPacket;
import com.fantasticpass.network.OpenViewScreenPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only packet reactions. Methods here are only ever invoked through
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} so the referenced client
 * classes are never loaded on a dedicated server.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void openViewScreen(OpenViewScreenPacket packet) {
        PassDefinition pass = packet.getPass();
        PlayerPassData data = packet.getPlayerData();
        Minecraft.getInstance().setScreen(new PassViewScreen(pass, data, packet.getMinutesPerTier()));
    }

    public static void openAdminScreen(OpenAdminScreenPacket packet) {
        Minecraft.getInstance().setScreen(new PassAdminScreen(packet.getPass()));
    }

    public static void updateNametag(NametagUpdatePacket packet) {
        ClientNametagCache.put(packet.getPlayerId(), packet.getData());
    }
}
