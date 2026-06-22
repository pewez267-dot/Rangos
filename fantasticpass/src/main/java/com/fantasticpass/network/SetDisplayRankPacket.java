package com.fantasticpass.network;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PlayerPassData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: choose which earned pass rank to display (empty string clears it
 * and falls back to Fantastic Ranks, if present). Ownership is verified server-side.
 */
public final class SetDisplayRankPacket {

    private final String rankId;

    public SetDisplayRankPacket(String rankId) {
        this.rankId = rankId == null ? "" : rankId;
    }

    public static void encode(SetDisplayRankPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.rankId);
    }

    public static SetDisplayRankPacket decode(FriendlyByteBuf buf) {
        return new SetDisplayRankPacket(buf.readUtf());
    }

    public static void handle(SetDisplayRankPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            PlayerPassData data = PassCapability.getData(sender);
            if (data == null) {
                return;
            }

            if (packet.rankId.isEmpty()) {
                data.setDisplayedRankId(null);
                NametagSync.syncPlayer(sender);
                return;
            }

            if (!data.hasEarnedRank(packet.rankId)) {
                sender.sendSystemMessage(Component.translatable("fantasticpass.msg.rank_not_owned", packet.rankId));
                return;
            }

            data.setDisplayedRankId(packet.rankId);
            sender.sendSystemMessage(Component.translatable("fantasticpass.msg.rank_set", packet.rankId));
            NametagSync.syncPlayer(sender);
        });
        context.setPacketHandled(true);
    }
}
