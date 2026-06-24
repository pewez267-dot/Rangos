package com.fantasticranks.network;

import com.fantasticranks.FantasticRanks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central network channel and registration for all Fantastic Ranks packets.
 */
public final class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(FantasticRanks.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private PacketHandler() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(SavePackagePacket.class, id++)
                .encoder(SavePackagePacket::encode)
                .decoder(SavePackagePacket::decode)
                .consumerMainThread(SavePackagePacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenAdminScreenPacket.class, id++)
                .encoder(OpenAdminScreenPacket::encode)
                .decoder(OpenAdminScreenPacket::decode)
                .consumerMainThread(OpenAdminScreenPacket::handle)
                .add();

        CHANNEL.messageBuilder(NametagUpdatePacket.class, id++)
                .encoder(NametagUpdatePacket::encode)
                .decoder(NametagUpdatePacket::decode)
                .consumerMainThread(NametagUpdatePacket::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }
}
