package com.fscrates.network;

import com.fscrates.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: play a crate-opening animation. Carries the animation id,
 * the crate rarity colour, the final reward item (for the reveal) and the list
 * of "spinning" candidate item names so the reel/roulette looks rich.
 */
public class PlayAnimationPacket {

    private final String animationId;
    private final int rarityColor;
    private final CompoundTag rewardItem;
    private final CompoundTag candidates;
    private final boolean allowSkip;

    public PlayAnimationPacket(String animationId, int rarityColor, CompoundTag rewardItem,
                               CompoundTag candidates, boolean allowSkip) {
        this.animationId = animationId;
        this.rarityColor = rarityColor;
        this.rewardItem = rewardItem;
        this.candidates = candidates;
        this.allowSkip = allowSkip;
    }

    public static void encode(PlayAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.animationId);
        buf.writeInt(msg.rarityColor);
        buf.writeNbt(msg.rewardItem);
        buf.writeNbt(msg.candidates);
        buf.writeBoolean(msg.allowSkip);
    }

    public static PlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readUtf(), buf.readInt(), buf.readNbt(), buf.readNbt(), buf.readBoolean());
    }

    public static void handle(PlayAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientPacketHandler.playAnimation(
                                msg.animationId, msg.rarityColor, msg.rewardItem, msg.candidates, msg.allowSkip)));
        context.setPacketHandled(true);
    }
}
