package com.fantasticnametags.net;

import com.fantasticnametags.client.ClientNametagState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Paquete server -> cliente con los ajustes de altura del nametag. */
public final class SyncNametagPacket {
    private final double height;
    private final boolean playersOnly;

    public SyncNametagPacket(double height, boolean playersOnly) {
        this.height = height;
        this.playersOnly = playersOnly;
    }

    public static void encode(SyncNametagPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.height);
        buf.writeBoolean(msg.playersOnly);
    }

    public static SyncNametagPacket decode(FriendlyByteBuf buf) {
        return new SyncNametagPacket(buf.readDouble(), buf.readBoolean());
    }

    public static void handle(SyncNametagPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNametagState.apply(msg.height, msg.playersOnly)));
        ctx.get().setPacketHandled(true);
    }
}
