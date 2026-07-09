package com.fsmobs.network;

import com.fsmobs.FSMobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private Net() {}

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FSMobs.MODID, "main"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();

        int id = 0;
        CHANNEL.registerMessage(id++, OpenConfigPacket.class,
                OpenConfigPacket::encode, OpenConfigPacket::decode, OpenConfigPacket::handle);
        CHANNEL.registerMessage(id++, SyncConfigPacket.class,
                SyncConfigPacket::encode, SyncConfigPacket::decode, SyncConfigPacket::handle);
        CHANNEL.registerMessage(id++, SetConfigPacket.class,
                SetConfigPacket::encode, SetConfigPacket::decode, SetConfigPacket::handle);
        CHANNEL.registerMessage(id++, ToggleStatsPacket.class,
                ToggleStatsPacket::encode, ToggleStatsPacket::decode, ToggleStatsPacket::handle);
        CHANNEL.registerMessage(id++, StatsPacket.class,
                StatsPacket::encode, StatsPacket::decode, StatsPacket::handle);
        CHANNEL.registerMessage(id++, SetOverlayPacket.class,
                SetOverlayPacket::encode, SetOverlayPacket::decode, SetOverlayPacket::handle);
    }
}
