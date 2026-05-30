package com.revivemod.network;

import com.revivemod.RevivemodForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * SimpleChannel replacement for Fabric's PayloadTypeRegistry / ServerPlayNetworking.
 *
 * Packets:
 *  - C2S {@link SurrenderTogglePacket} : player holding E requested surrender.
 *  - C2S {@link SelfReviveTogglePacket}: player holding F requested self-revive.
 *  - S2C {@link DownStartPacket}       : tells the client it is downed (+ self-revive cost) -> HUD on.
 *  - S2C {@link DownEndPacket}         : tells the client it is no longer downed -> HUD off.
 */
public final class ReviveNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private ReviveNetwork() {
    }

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(RevivemodForge.MOD_ID, "main"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();

        int id = 0;
        CHANNEL.registerMessage(id++, SurrenderTogglePacket.class,
                SurrenderTogglePacket::encode, SurrenderTogglePacket::decode, SurrenderTogglePacket::handle);
        CHANNEL.registerMessage(id++, SelfReviveTogglePacket.class,
                SelfReviveTogglePacket::encode, SelfReviveTogglePacket::decode, SelfReviveTogglePacket::handle);
        CHANNEL.registerMessage(id++, DownStartPacket.class,
                DownStartPacket::encode, DownStartPacket::decode, DownStartPacket::handle);
        CHANNEL.registerMessage(id++, DownEndPacket.class,
                DownEndPacket::encode, DownEndPacket::decode, DownEndPacket::handle);
    }

    /**
     * Sends a packet to one player. Wrapped so a vanilla / channel-less client never throws.
     */
    public static void sendToPlayer(ServerPlayer player, Object msg) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
        } catch (Throwable ignored) {
            // Vanilla client without the channel - HUD packets are simply skipped.
        }
    }
}
