package com.fantasticranks.client;

import com.fantasticranks.gui.admin.RanksAdminScreen;
import com.fantasticranks.nametag.ClientNametagCache;
import com.fantasticranks.network.NametagUpdatePacket;
import com.fantasticranks.network.OpenAdminScreenPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only packet reactions. Only ever invoked through
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} so the referenced client classes
 * are never loaded on a dedicated server.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void openAdminScreen(OpenAdminScreenPacket packet) {
        Minecraft.getInstance().setScreen(new RanksAdminScreen(packet.getPackage()));
    }

    public static void updateNametag(NametagUpdatePacket packet) {
        ClientNametagCache.put(packet.getPlayerId(), packet.getData());
    }
}
