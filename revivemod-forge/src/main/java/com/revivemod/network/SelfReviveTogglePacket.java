package com.revivemod.network;

import com.revivemod.state.DownManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: the player held the self-revive key (F) for the full window. */
public class SelfReviveTogglePacket {
    public SelfReviveTogglePacket() {
    }

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
