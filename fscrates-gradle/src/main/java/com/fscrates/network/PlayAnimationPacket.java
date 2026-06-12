// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.network;

import net.minecraftforge.fml.DistExecutor;
import com.fscrates.client.ClientPacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

public class PlayAnimationPacket
{
    private final BlockPos pos;
    private final String animationId;
    private final int rarityColor;
    private final int winnerIndex;
    private final int winnerRarity;
    private final CompoundTag candidates;
    
    public PlayAnimationPacket(final BlockPos pos, final String animationId, final int rarityColor, final int winnerIndex, final int winnerRarity, final CompoundTag candidates) {
        this.pos = pos;
        this.animationId = animationId;
        this.rarityColor = rarityColor;
        this.winnerIndex = winnerIndex;
        this.winnerRarity = winnerRarity;
        this.candidates = candidates;
    }
    
    public static void encode(final PlayAnimationPacket msg, final FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.animationId);
        buf.writeInt(msg.rarityColor);
        buf.writeInt(msg.winnerIndex);
        buf.writeInt(msg.winnerRarity);
        buf.writeNbt(msg.candidates);
    }
    
    public static PlayAnimationPacket decode(final FriendlyByteBuf buf) {
        return new PlayAnimationPacket(buf.readBlockPos(), buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readNbt());
    }
    
    public static void handle(final PlayAnimationPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.playAnimation(msg.pos, msg.animationId, msg.rarityColor, msg.winnerIndex, msg.winnerRarity, msg.candidates)));
        context.setPacketHandled(true);
    }
}
