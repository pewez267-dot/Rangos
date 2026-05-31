package com.revivemod.network;

import com.revivemod.state.DownManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class SelfReviveTogglePacket {
    public static void encode(SelfReviveTogglePacket msg, FriendlyByteBuf buf) {
    }

    public static SelfReviveTogglePacket decode(FriendlyByteBuf buf) {
        return new SelfReviveTogglePacket();
    }

    public static void handle(SelfReviveTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                DownManager.requestSelfToggle(sender.getUUID());
            }
        });
        c.setPacketHandled(true);
    }
}

