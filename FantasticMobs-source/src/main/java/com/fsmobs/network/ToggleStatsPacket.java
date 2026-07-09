package com.fsmobs.network;

import com.fsmobs.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: avisa si la pestana de estadisticas de la GUI esta abierta (para recibir stats). */
public class ToggleStatsPacket {

    private final boolean guiOpen;

    public ToggleStatsPacket(boolean guiOpen) {
        this.guiOpen = guiOpen;
    }

    public static void encode(ToggleStatsPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.guiOpen);
    }

    public static ToggleStatsPacket decode(FriendlyByteBuf buf) {
        return new ToggleStatsPacket(buf.readBoolean());
    }

    public static void handle(ToggleStatsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp != null) {
                StatsManager.setGuiWatching(sp.getUUID(), msg.guiOpen);
            }
        });
        c.setPacketHandled(true);
    }
}
