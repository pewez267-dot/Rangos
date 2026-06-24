package com.fantasticchest.network;

import com.fantasticchest.gui.terminal.ChestTerminalScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S-&gt;C: one page of terminal entries (at most {@code page_size}). */
public final class TerminalPagePacket {

    private final int pageIndex;
    private final int total;
    private final List<TerminalEntry> entries;

    public TerminalPagePacket(final int pageIndex, final int total, final List<TerminalEntry> entries) {
        this.pageIndex = pageIndex;
        this.total = total;
        this.entries = entries;
    }

    public static void encode(final TerminalPagePacket m, final FriendlyByteBuf buf) {
        buf.writeVarInt(m.pageIndex);
        buf.writeVarInt(m.total);
        buf.writeVarInt(m.entries.size());
        for (final TerminalEntry e : m.entries) {
            e.write(buf);
        }
    }

    public static TerminalPagePacket decode(final FriendlyByteBuf buf) {
        final int pageIndex = buf.readVarInt();
        final int total = buf.readVarInt();
        final int count = buf.readVarInt();
        final List<TerminalEntry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(TerminalEntry.read(buf));
        }
        return new TerminalPagePacket(pageIndex, total, list);
    }

    public static void handle(final TerminalPagePacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ChestTerminalScreen.acceptPage(m.pageIndex, m.total, m.entries)));
        context.setPacketHandled(true);
    }
}
