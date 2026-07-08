package com.fantasticnametags.net;

import com.fantasticnametags.FantasticNametags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NametagNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FantasticNametags.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        CHANNEL.registerMessage(0, SyncNametagPacket.class,
            SyncNametagPacket::encode, SyncNametagPacket::decode, SyncNametagPacket::handle);
    }

    /** Envia los valores actuales a TODOS los clientes conectados (en vivo). */
    public static void syncToAll(double height, boolean playersOnly) {
        if (CHANNEL != null) {
            CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncNametagPacket(height, playersOnly));
        }
    }

    private NametagNetwork() {
    }
}
