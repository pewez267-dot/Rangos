package com.pewez.fantasticshortcuts.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal de red del mod (cliente <-> servidor).
 *
 * <p>La GUI es una {@code Screen} de cliente, pero los atajos viven en el servidor. Este canal
 * transporta:
 * <ul>
 *     <li>S -> C: {@link OpenEditorPacket} (abre/refresca el editor con la lista actual).</li>
 *     <li>C -> S: {@link CreateShortcutPacket}, {@link SaveShortcutPacket}, {@link DeleteShortcutPacket}
 *     (operaciones CRUD; el servidor valida permiso 4, aplica, audita, sincroniza y responde con un
 *     {@link OpenEditorPacket} para refrescar la pantalla).</li>
 * </ul>
 */
public final class FSNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("fantasticshortcuts", "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private FSNetwork() {}

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
