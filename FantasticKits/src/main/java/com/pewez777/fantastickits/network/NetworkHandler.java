/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.network;

import com.pewez777.fantastickits.Reference;
import com.pewez777.fantastickits.network.packets.DeleteKitPacket;
import com.pewez777.fantastickits.network.packets.OpenDeleteConfirmPacket;
import com.pewez777.fantastickits.network.packets.OpenEditorPacket;
import com.pewez777.fantastickits.network.packets.SaveKitPacket;
import com.pewez777.fantastickits.network.packets.TestKitPacket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Owns the mod's {@link SimpleChannel} and registers every packet.
 *
 * <p>Flow: the server opens the client editor with {@link OpenEditorPacket}
 * (S-&gt;C) carrying all data; the client edits a local copy and sends back
 * {@link SaveKitPacket}/{@link DeleteKitPacket}/{@link TestKitPacket} (C-&gt;S),
 * all of which are fully re-validated server-side.</p>
 */
public final class NetworkHandler {

    private static final String PROTOCOL = Reference.NETWORK_PROTOCOL;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Reference.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static boolean registered = false;

    private NetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;

        CHANNEL.messageBuilder(OpenEditorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenEditorPacket::encode)
                .decoder(OpenEditorPacket::decode)
                .consumerMainThread(OpenEditorPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenDeleteConfirmPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDeleteConfirmPacket::encode)
                .decoder(OpenDeleteConfirmPacket::decode)
                .consumerMainThread(OpenDeleteConfirmPacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveKitPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveKitPacket::encode)
                .decoder(SaveKitPacket::decode)
                .consumerMainThread(SaveKitPacket::handle)
                .add();

        CHANNEL.messageBuilder(DeleteKitPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteKitPacket::encode)
                .decoder(DeleteKitPacket::decode)
                .consumerMainThread(DeleteKitPacket::handle)
                .add();

        CHANNEL.messageBuilder(TestKitPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TestKitPacket::encode)
                .decoder(TestKitPacket::decode)
                .consumerMainThread(TestKitPacket::handle)
                .add();
    }

    /** Sends a packet to a specific player (server -&gt; client). */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Sends a packet to the server (client -&gt; server). */
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
