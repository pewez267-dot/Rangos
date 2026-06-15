package com.fantastic.kits.network;

import com.fantastic.kits.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Single Forge {@link SimpleChannel} for Fantastic Kits, mirroring the exact
 * pattern used by FantasticCrates and FantasticSpawners.
 *
 * <p>Every administrative interaction goes through this channel:
 * <ul>
 *     <li>Server -> Client: instructions to open the appropriate
 *         {@link net.minecraft.client.gui.screens.Screen} with pre-loaded data.</li>
 *     <li>Client -> Server: edited kit NBT, deletions and test claims, all
 *         re-validated server-side before being applied.</li>
 * </ul>
 */
public final class FKNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Reference.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private FKNetwork() {}

    public static void register() {
        int id = 0;
        // S -> C
        CHANNEL.registerMessage(id++, OpenKitListPacket.class,
                OpenKitListPacket::encode, OpenKitListPacket::decode, OpenKitListPacket::handle);
        CHANNEL.registerMessage(id++, OpenKitEditorPacket.class,
                OpenKitEditorPacket::encode, OpenKitEditorPacket::decode, OpenKitEditorPacket::handle);

        // C -> S
        CHANNEL.registerMessage(id++, SaveKitPacket.class,
                SaveKitPacket::encode, SaveKitPacket::decode, SaveKitPacket::handle);
        CHANNEL.registerMessage(id++, DeleteKitPacket.class,
                DeleteKitPacket::encode, DeleteKitPacket::decode, DeleteKitPacket::handle);
        CHANNEL.registerMessage(id++, TestKitPacket.class,
                TestKitPacket::encode, TestKitPacket::decode, TestKitPacket::handle);
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
