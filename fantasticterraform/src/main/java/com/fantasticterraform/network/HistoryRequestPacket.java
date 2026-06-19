package com.fantasticterraform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C-&gt;S: pide el estado actual del historial para mostrarlo en el panel. */
public final class HistoryRequestPacket {

    public HistoryRequestPacket() {
    }

    public static void encode(HistoryRequestPacket m, FriendlyByteBuf buf) {
    }

    public static HistoryRequestPacket decode(FriendlyByteBuf buf) {
        return new HistoryRequestPacket();
    }

    public static void handle(HistoryRequestPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            PacketHandler.sendToClient(player, HistoryListPacket.fromPlayer(player));
        });
        c.setPacketHandled(true);
    }
}
