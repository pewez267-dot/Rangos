package com.fantasticchest.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Single {@link SimpleChannel} for Fantastic Chest. Every server-bound handler validates
 * permissions, distance, block type and authenticated UUID before acting.
 */
public final class PacketHandler {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("fantasticchest", "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private PacketHandler() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CreateChestPacket.class,
                CreateChestPacket::encode, CreateChestPacket::decode, CreateChestPacket::handle);
        CHANNEL.registerMessage(id++, EditChestPacket.class,
                EditChestPacket::encode, EditChestPacket::decode, EditChestPacket::handle);
        CHANNEL.registerMessage(id++, RefreshStockPacket.class,
                RefreshStockPacket::encode, RefreshStockPacket::decode, RefreshStockPacket::handle);
        CHANNEL.registerMessage(id++, UpdatePermissionsPacket.class,
                UpdatePermissionsPacket::encode, UpdatePermissionsPacket::decode, UpdatePermissionsPacket::handle);
        CHANNEL.registerMessage(id++, OpenTerminalPacket.class,
                OpenTerminalPacket::encode, OpenTerminalPacket::decode, OpenTerminalPacket::handle);
        CHANNEL.registerMessage(id++, TerminalPagePacket.class,
                TerminalPagePacket::encode, TerminalPagePacket::decode, TerminalPagePacket::handle);
        CHANNEL.registerMessage(id++, TerminalExtractPacket.class,
                TerminalExtractPacket::encode, TerminalExtractPacket::decode, TerminalExtractPacket::handle);
        CHANNEL.registerMessage(id++, TerminalUpdatePacket.class,
                TerminalUpdatePacket::encode, TerminalUpdatePacket::decode, TerminalUpdatePacket::handle);
    }

    public static void sendToServer(final Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToClient(final ServerPlayer player, final Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    // ---- shared buffer helpers for Map<String, Long> ----

    public static void writeLongMap(final net.minecraft.network.FriendlyByteBuf buf, final java.util.Map<String, Long> map) {
        final java.util.Map<String, Long> safe = map == null ? java.util.Map.of() : map;
        buf.writeVarInt(safe.size());
        for (final java.util.Map.Entry<String, Long> e : safe.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeLong(e.getValue() == null ? 0L : e.getValue());
        }
    }

    public static java.util.Map<String, Long> readLongMap(final net.minecraft.network.FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final java.util.LinkedHashMap<String, Long> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            final String key = buf.readUtf();
            final long value = buf.readLong();
            map.put(key, value);
        }
        return map;
    }

    public static void writeStringList(final net.minecraft.network.FriendlyByteBuf buf, final java.util.List<String> list) {
        final java.util.List<String> safe = list == null ? java.util.List.of() : list;
        buf.writeVarInt(safe.size());
        for (final String s : safe) {
            buf.writeUtf(s == null ? "" : s);
        }
    }

    public static java.util.List<String> readStringList(final net.minecraft.network.FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final java.util.ArrayList<String> list = new java.util.ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }
}
