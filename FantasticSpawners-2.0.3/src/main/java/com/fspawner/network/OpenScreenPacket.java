// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.network;

import net.minecraftforge.fml.DistExecutor;
import com.fspawner.client.ClientPacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;

public class OpenScreenPacket
{
    private final CompoundTag configNbt;
    private final EditContext context;
    
    public OpenScreenPacket(final CompoundTag configNbt, final EditContext context) {
        this.configNbt = configNbt;
        this.context = context;
    }
    
    public OpenScreenPacket(final CompoundTag configNbt) {
        this(configNbt, EditContext.newSession());
    }
    
    public static void encode(final OpenScreenPacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
        msg.context.encode(buf);
    }
    
    public static OpenScreenPacket decode(final FriendlyByteBuf buf) {
        final CompoundTag tag = buf.readNbt();
        final EditContext ctx = EditContext.decode(buf);
        return new OpenScreenPacket(tag, ctx);
    }
    
    public static void handle(final OpenScreenPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openScreen(msg.configNbt, msg.context)));
        context.setPacketHandled(true);
    }
}
