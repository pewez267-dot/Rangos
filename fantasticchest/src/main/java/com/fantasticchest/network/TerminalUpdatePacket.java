package com.fantasticchest.network;

import com.fantasticchest.gui.terminal.ChestTerminalScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S-&gt;C: confirms an extraction with the item's new quantity and the updated total count. */
public final class TerminalUpdatePacket {

    private final String itemId;
    private final long newQuantity;
    private final int total;

    public TerminalUpdatePacket(final String itemId, final long newQuantity, final int total) {
        this.itemId = itemId;
        this.newQuantity = newQuantity;
        this.total = total;
    }

    public static void encode(final TerminalUpdatePacket m, final FriendlyByteBuf buf) {
        buf.writeUtf(m.itemId);
        buf.writeLong(m.newQuantity);
        buf.writeVarInt(m.total);
    }

    public static TerminalUpdatePacket decode(final FriendlyByteBuf buf) {
        return new TerminalUpdatePacket(buf.readUtf(), buf.readLong(), buf.readVarInt());
    }

    public static void handle(final TerminalUpdatePacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ChestTerminalScreen.acceptUpdate(m.itemId, m.newQuantity, m.total)));
        context.setPacketHandled(true);
    }
}
