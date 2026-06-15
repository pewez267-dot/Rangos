package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network channel for the Fantastic Shortcuts editor screen.
 */
public final class FSShortcutsNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(FantasticShortcutsMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private FSShortcutsNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenEditorPacket.class,
                OpenEditorPacket::encode, OpenEditorPacket::decode, OpenEditorPacket::handle);
        CHANNEL.registerMessage(id++, CreateShortcutPacket.class,
                CreateShortcutPacket::encode, CreateShortcutPacket::decode, CreateShortcutPacket::handle);
        CHANNEL.registerMessage(id++, SaveShortcutPacket.class,
                SaveShortcutPacket::encode, SaveShortcutPacket::decode, SaveShortcutPacket::handle);
        CHANNEL.registerMessage(id++, DeleteShortcutPacket.class,
                DeleteShortcutPacket::encode, DeleteShortcutPacket::decode, DeleteShortcutPacket::handle);
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
