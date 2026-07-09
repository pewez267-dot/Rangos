package com.fsmobs.network;

import com.fsmobs.client.ClientState;
import com.fsmobs.stats.ServerStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: instantanea de estadisticas para mostrar en la GUI o el panel. */
public class StatsPacket {

    private final ServerStats stats;

    public StatsPacket(ServerStats stats) {
        this.stats = stats;
    }

    public static void encode(StatsPacket msg, FriendlyByteBuf buf) {
        msg.stats.write(buf);
    }

    public static StatsPacket decode(FriendlyByteBuf buf) {
        return new StatsPacket(ServerStats.read(buf));
    }

    public static void handle(StatsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientState.setStats(msg.stats)));
        c.setPacketHandled(true);
    }
}
