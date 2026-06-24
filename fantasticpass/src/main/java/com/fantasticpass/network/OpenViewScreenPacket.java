package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: open the player Battle Pass screen with the active pass, the
 * player's progress snapshot, and the effective minutes-per-tier.
 */
public final class OpenViewScreenPacket {

    private final PassDefinition pass;
    private final CompoundTag playerData;
    private final int minutesPerTier;

    public OpenViewScreenPacket(PassDefinition pass, PlayerPassData playerData, int minutesPerTier) {
        this.pass = pass;
        this.playerData = playerData.toNbt();
        this.minutesPerTier = minutesPerTier;
    }

    private OpenViewScreenPacket(PassDefinition pass, CompoundTag playerData, int minutesPerTier) {
        this.pass = pass;
        this.playerData = playerData;
        this.minutesPerTier = minutesPerTier;
    }

    public PassDefinition getPass() {
        return pass;
    }

    public PlayerPassData getPlayerData() {
        PlayerPassData data = new PlayerPassData();
        data.fromNbt(playerData);
        return data;
    }

    public int getMinutesPerTier() {
        return minutesPerTier;
    }

    public static void encode(OpenViewScreenPacket packet, FriendlyByteBuf buf) {
        packet.pass.toBuf(buf);
        buf.writeNbt(packet.playerData);
        buf.writeVarInt(packet.minutesPerTier);
    }

    public static OpenViewScreenPacket decode(FriendlyByteBuf buf) {
        PassDefinition pass = PassDefinition.fromBuf(buf);
        CompoundTag tag = buf.readNbt();
        int minutesPerTier = buf.readVarInt();
        return new OpenViewScreenPacket(pass, tag == null ? new CompoundTag() : tag, minutesPerTier);
    }

    public static void handle(OpenViewScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openViewScreen(packet)));
        context.setPacketHandled(true);
    }
}
