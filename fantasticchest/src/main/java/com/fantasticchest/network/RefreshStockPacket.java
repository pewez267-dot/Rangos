package com.fantasticchest.network;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.security.PermissionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C-&gt;S: OP triggers Refresh Stock (atomic restore to the configured original stock). */
public final class RefreshStockPacket {

    private final BlockPos pos;

    public RefreshStockPacket(final BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(final RefreshStockPacket m, final FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
    }

    public static RefreshStockPacket decode(final FriendlyByteBuf buf) {
        return new RefreshStockPacket(buf.readBlockPos());
    }

    public static void handle(final RefreshStockPacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || !PermissionValidator.isOp(player)) {
                return;
            }
            final ChestBlockEntity chest = PermissionValidator.resolve(player, m.pos);
            if (chest == null) {
                player.sendSystemMessage(Component.literal("§cNo se encontro el cofre o estas demasiado lejos."));
                return;
            }
            chest.refreshStock();
            player.sendSystemMessage(Component.literal("§aStock restaurado a los valores originales configurados."));
        });
        context.setPacketHandled(true);
    }
}
