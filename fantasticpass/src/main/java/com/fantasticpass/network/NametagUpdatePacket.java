package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.nametag.NametagData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: update one player's cached nametag state so the extra rank line
 * re-renders in real time on every client that can see them.
 */
public final class NametagUpdatePacket {

    private final UUID playerId;
    private final NametagData data;

    public NametagUpdatePacket(UUID playerId, NametagData data) {
        this.playerId = playerId;
        this.data = data;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public NametagData getData() {
        return data;
    }

    public static void encode(NametagUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        packet.data.toBuf(buf);
    }

    public static NametagUpdatePacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        NametagData data = NametagData.fromBuf(buf);
        return new NametagUpdatePacket(id, data);
    }

    public static void handle(NametagUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.updateNametag(packet)));
        context.setPacketHandled(true);
    }
}
