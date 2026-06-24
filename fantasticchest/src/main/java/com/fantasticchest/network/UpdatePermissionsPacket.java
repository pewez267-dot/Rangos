package com.fantasticchest.network;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.security.PermissionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** C-&gt;S: OP replaces the chest's permitted-players list (resolved server-side to UUIDs). */
public final class UpdatePermissionsPacket {

    private final BlockPos pos;
    private final List<String> permitted;

    public UpdatePermissionsPacket(final BlockPos pos, final List<String> permitted) {
        this.pos = pos;
        this.permitted = permitted;
    }

    public static void encode(final UpdatePermissionsPacket m, final FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        PacketHandler.writeStringList(buf, m.permitted);
    }

    public static UpdatePermissionsPacket decode(final FriendlyByteBuf buf) {
        return new UpdatePermissionsPacket(buf.readBlockPos(), PacketHandler.readStringList(buf));
    }

    public static void handle(final UpdatePermissionsPacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null || !PermissionValidator.isOp(player)) {
                return;
            }
            final ChestBlockEntity chest = PermissionValidator.resolve(player, m.pos);
            if (chest == null) {
                player.sendSystemMessage(Component.literal("§cNo se encontro el cofre o estas demasiado lejos."));
                return;
            }
            final Set<UUID> resolved = new HashSet<>();
            for (final String s : m.permitted) {
                final UUID uuid = PermissionValidator.resolveUuid(player.getServer(), s);
                if (uuid != null) {
                    resolved.add(uuid);
                }
            }
            chest.setPermitted(resolved);
            player.sendSystemMessage(Component.literal("§aPermisos actualizados (" + resolved.size() + " jugador(es))."));
        });
        context.setPacketHandled(true);
    }
}
