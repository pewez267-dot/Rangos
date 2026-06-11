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
 * world (no GUI). Carries the crate position, the animation id, the tier colour,
 * the headline reward (for the floating reveal) and the candidate item icons.
 */
public class PlayAnimationPacket {

    private final BlockPos pos;
    private final String animationId;
    private final int rarityColor;
    private final CompoundTag rewardItem;
    private final CompoundTag candidates;

    public PlayAnimationPacket(BlockPos pos, String animationId, int rarityColor,
                               CompoundTag rewardItem, CompoundTag candidates) {
        this.pos = pos;
        this.animationId = animationId;
        this.rarityColor = rarityColor;
        this.rewardItem = rewardItem;
        this.candidates = candidates;
    }

    public static void encode(PlayAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.animationId);
        buf.writeInt(msg.rarityColor);
        buf.writeNbt(msg.rewardItem);
        buf.writeNbt(msg.candidates);
    }

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readBlockPos(), buf.readUtf(), buf.readInt(),
                buf.readNbt(), buf.readNbt());
    }

    public static void handle(PlayAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.playAnimation(
                                msg.pos, msg.animationId, msg.rarityColor, msg.rewardItem, msg.candidates)));
        context.setPacketHandled(true);
    }
}
