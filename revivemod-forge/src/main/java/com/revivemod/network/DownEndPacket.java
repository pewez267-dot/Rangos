package com.revivemod.network;

import com.revivemod.client.RevivemodClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: client is no longer downed; clears the HUD. */
public class DownEndPacket {
    public DownEndPacket() {
    }

    public static void encode(DownEndPacket msg, FriendlyByteBuf buf) {
    }

    public static DownEndPacket decode(FriendlyByteBuf buf) {
        return new DownEndPacket();
    }

    public static void handle(DownEndPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RevivemodClient::onDownEnd));
        c.setPacketHandled(true);
    }
}
