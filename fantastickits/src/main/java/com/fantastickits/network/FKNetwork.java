package com.fantastickits.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Single {@link SimpleChannel} for Fantastic Kits.
 *
 * <ul>
 *     <li>{@link OpenKitEditorPacket} — server &rarr; client, opens the editor with the
 *     kit data, the live LuckPerms group list and the commands assigned to the group.</li>
 *     <li>{@link SaveKitPacket} — client &rarr; server, persists the edited kit. The
 *     server re-validates permissions before acting; the client is never trusted.</li>
 * </ul>
 */
public final class FKNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("fantastickits", "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private FKNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenKitEditorPacket.class,
                OpenKitEditorPacket::encode, OpenKitEditorPacket::decode, OpenKitEditorPacket::handle);
        CHANNEL.registerMessage(id++, SaveKitPacket.class,
                SaveKitPacket::encode, SaveKitPacket::decode, SaveKitPacket::handle);
    }

    public static void sendToClient(final ServerPlayer player, final Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(final Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
