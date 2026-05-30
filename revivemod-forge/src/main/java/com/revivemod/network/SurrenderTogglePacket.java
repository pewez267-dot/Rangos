package com.revivemod.network;

import com.revivemod.state.DownManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: the player held the surrender key (E) for the full window. */
public class SurrenderTogglePacket {
    public SurrenderTogglePacket() {
    }

    public static void encode(SurrenderTogglePacket msg, FriendlyByteBuf buf) {
    }

    public static SurrenderTogglePacket decode(FriendlyByteBuf buf) {
        return new SurrenderTogglePacket();
    }

    public static void handle(SurrenderTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                DownManager.requestSurrenderToggle(sender.getUUID());
            }
        });
        c.setPacketHandled(true);
    }
}
