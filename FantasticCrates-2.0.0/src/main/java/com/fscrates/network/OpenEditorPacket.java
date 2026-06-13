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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class OpenEditorPacket
{
    private final CompoundTag configNbt;
    private final BlockPos pos;
    
    public OpenEditorPacket(final CompoundTag configNbt) {
        this(configNbt, null);
    }
    
    public OpenEditorPacket(final CompoundTag configNbt, final BlockPos pos) {
        this.configNbt = configNbt;
        this.pos = pos;
    }
    
    public static void encode(final OpenEditorPacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
        final boolean hasPos = msg.pos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeBlockPos(msg.pos);
        }
    }
    
    public static OpenEditorPacket decode(final FriendlyByteBuf buf) {
        final CompoundTag nbt = buf.readNbt();
        final BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new OpenEditorPacket(nbt, pos);
    }
    
    public static void handle(final OpenEditorPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openEditor(msg.configNbt, msg.pos)));
        context.setPacketHandled(true);
    }
}
