package com.fantasticpass.network;

import com.fantasticpass.progression.RewardDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: request to claim a tier's rewards. All validation is server-side.
 */
public final class ClaimTierPacket {

    private final int tier;

    public ClaimTierPacket(int tier) {
        this.tier = tier;
    }

    public static void encode(ClaimTierPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.tier);
    }

    public static ClaimTierPacket decode(FriendlyByteBuf buf) {
        return new ClaimTierPacket(buf.readVarInt());
    }

    public static void handle(ClaimTierPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            RewardDispatcher.ClaimResult result = RewardDispatcher.claim(sender, packet.tier);
            sender.sendSystemMessage(RewardDispatcher.messageFor(result, packet.tier));
        });
        context.setPacketHandled(true);
    }
}
