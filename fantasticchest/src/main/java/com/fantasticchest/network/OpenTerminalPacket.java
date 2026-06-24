package com.fantasticchest.network;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.gui.terminal.ChestTerminalMenu;
import com.fantasticchest.security.PermissionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * C-&gt;S: request a page of the terminal inventory (used by scrolling). The server
 * re-validates access and distance, then replies with a {@link TerminalPagePacket}
 * containing only {@code page_size} entries — never the whole inventory at once.
 */
public final class OpenTerminalPacket {

    private final BlockPos pos;
    private final int pageIndex;

    public OpenTerminalPacket(final BlockPos pos, final int pageIndex) {
        this.pos = pos;
        this.pageIndex = pageIndex;
    }

    public static void encode(final OpenTerminalPacket m, final FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.pageIndex);
    }

    public static OpenTerminalPacket decode(final FriendlyByteBuf buf) {
        return new OpenTerminalPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(final OpenTerminalPacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            final ChestBlockEntity chest = PermissionValidator.resolve(player, m.pos);
            if (chest == null || !chest.canAccess(player.getUUID())) {
                return;
            }
            final List<TerminalEntry> full = ChestTerminalMenu.buildFullList(chest);
            final List<TerminalEntry> pageEntries = ChestTerminalMenu.page(full, m.pageIndex);
            PacketHandler.sendToClient(player, new TerminalPagePacket(m.pageIndex, full.size(), pageEntries));
        });
        context.setPacketHandled(true);
    }
}
