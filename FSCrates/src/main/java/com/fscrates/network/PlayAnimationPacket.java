package com.fscrates.network;

import com.fscrates.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: play the crate-opening animation ON the crate block in the
 * world. Carries the crate position, the animation id, the tier colour, the
 * candidate icons and the EXPLICIT winning index so the reel lands on exactly
 * the reward that will be delivered.
 */
public class PlayAnimationPacket {

    private final BlockPos pos;
    private final String animationId;
    private final int rarityColor;
    private final int winnerIndex;
    private final CompoundTag candidates;

    public PlayAnimationPacket(BlockPos pos, String animationId, int rarityColor,
                               int winnerIndex, CompoundTag candidates) {
        this.pos = pos;
        this.animationId = animationId;
        this.rarityColor = rarityColor;
        this.winnerIndex = winnerIndex;
        this.candidates = candidates;
    }

    public static void encode(PlayAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.animationId);
        buf.writeInt(msg.rarityColor);
        buf.writeInt(msg.winnerIndex);
        buf.writeNbt(msg.candidates);
    }

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readBlockPos(), buf.readUtf(), buf.readInt(),
                buf.readInt(), buf.readNbt());
    }

    public static void handle(PlayAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.playAnimation(
                                msg.pos, msg.animationId, msg.rarityColor, msg.winnerIndex, msg.candidates)));
        context.setPacketHandled(true);
    }
}
