package com.revivemod.network;

import com.revivemod.client.RevivemodClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class DownStartPacket {
    public final int selfCost;

    public DownStartPacket(int selfCost) {
        this.selfCost = selfCost;
    }

    public static void encode(DownStartPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.selfCost);
    }

    public static DownStartPacket decode(FriendlyByteBuf buf) {
        return new DownStartPacket(buf.readVarInt());
    }

    public static void handle(DownStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> RevivemodClient.onDownStart(msg.selfCost)));
        c.setPacketHandled(true);
    }
}

