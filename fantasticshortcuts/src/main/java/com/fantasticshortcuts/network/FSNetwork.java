package com.fantasticshortcuts.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Single {@link SimpleChannel} for Fantastic Shortcuts. All messages are client &rarr;
 * server requests; the server re-validates permissions and conflicts before acting.
 */
public final class FSNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("fantasticshortcuts", "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            // Optional channel: vanilla / mod-less clients may still connect. Aliases work for
            // them server-side (via the vanilla command tree + command interception); only the
            // admin management GUI requires the mod on the client.
            .clientAcceptedVersions(v -> true)
            .serverAcceptedVersions(v -> true)
            .simpleChannel();

    private FSNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, RequestOpenMainPacket.class,
                RequestOpenMainPacket::encode, RequestOpenMainPacket::decode, RequestOpenMainPacket::handle);
        CHANNEL.registerMessage(id++, RequestOpenEditorPacket.class,
                RequestOpenEditorPacket::encode, RequestOpenEditorPacket::decode, RequestOpenEditorPacket::handle);
        CHANNEL.registerMessage(id++, SaveShortcutPacket.class,
                SaveShortcutPacket::encode, SaveShortcutPacket::decode, SaveShortcutPacket::handle);
        CHANNEL.registerMessage(id++, DeleteShortcutPacket.class,
                DeleteShortcutPacket::encode, DeleteShortcutPacket::decode, DeleteShortcutPacket::handle);
    }

    public static void sendToServer(final Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
