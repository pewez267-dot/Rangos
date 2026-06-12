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

public class OpenEditorPacket
{
    private final CompoundTag configNbt;
    
    public OpenEditorPacket(final CompoundTag configNbt) {
        this.configNbt = configNbt;
    }
    
    public static void encode(final OpenEditorPacket msg, final FriendlyByteBuf buf) {
        buf.m_130079_(msg.configNbt);
    }
    
    public static OpenEditorPacket decode(final FriendlyByteBuf buf) {
        return new OpenEditorPacket(buf.m_130260_());
    }
    
    public static void handle(final OpenEditorPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openEditor(msg.configNbt)));
        context.setPacketHandled(true);
    }
}
