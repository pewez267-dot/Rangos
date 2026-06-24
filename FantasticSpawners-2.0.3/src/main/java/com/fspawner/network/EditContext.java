// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

public final class EditContext
{
    public final Source source;
    public final int slot;
    public final BlockPos pos;
    
    private EditContext(final Source source, final int slot, final BlockPos pos) {
        this.source = source;
        this.slot = slot;
        this.pos = pos;
    }
    
    public static EditContext newSession() {
        return new EditContext(Source.NEW, -1, null);
    }
    
    public static EditContext mainHand(final int slot) {
        return new EditContext(Source.MAIN_HAND, slot, null);
    }
    
    public static EditContext offHand() {
        return new EditContext(Source.OFF_HAND, -1, null);
    }
    
    public static EditContext block(final BlockPos pos) {
        return new EditContext(Source.BLOCK, -1, pos);
    }
    
    public void encode(final FriendlyByteBuf buf) {
        buf.writeEnum((Enum)this.source);
        buf.writeInt(this.slot);
        buf.writeBoolean(this.pos != null);
        if (this.pos != null) {
            buf.writeBlockPos(this.pos);
        }
    }
    
    public static EditContext decode(final FriendlyByteBuf buf) {
        final Source src = (Source)buf.readEnum(Source.class);
        final int slot = buf.readInt();
        final BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new EditContext(src, slot, pos);
    }
    
    public enum Source
    {
        NEW, 
        MAIN_HAND, 
        OFF_HAND, 
        BLOCK;
    }
}
