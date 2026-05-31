package com.revivemod.network;

import com.revivemod.network.DownEndPacket;
import com.revivemod.network.DownStartPacket;
import com.revivemod.network.SelfReviveTogglePacket;
import com.revivemod.network.SurrenderTogglePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ReviveNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private ReviveNetwork() {
    }

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder.named((ResourceLocation)new ResourceLocation("revivemod", "main")).networkProtocolVersion(() -> PROTOCOL).clientAcceptedVersions(PROTOCOL::equals).serverAcceptedVersions(PROTOCOL::equals).simpleChannel();
        int id = 0;
        CHANNEL.registerMessage(id++, SurrenderTogglePacket.class, SurrenderTogglePacket::encode, SurrenderTogglePacket::decode, SurrenderTogglePacket::handle);
        CHANNEL.registerMessage(id++, SelfReviveTogglePacket.class, SelfReviveTogglePacket::encode, SelfReviveTogglePacket::decode, SelfReviveTogglePacket::handle);
        CHANNEL.registerMessage(id++, DownStartPacket.class, DownStartPacket::encode, DownStartPacket::decode, DownStartPacket::handle);
        CHANNEL.registerMessage(id++, DownEndPacket.class, DownEndPacket::encode, DownEndPacket::decode, DownEndPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

