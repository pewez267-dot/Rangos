package com.fantasticterraform.network;

import com.fantasticterraform.schematics.SchematicFormat;
import com.fantasticterraform.schematics.SchematicManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** C->S: solicita la lista de schematics disponibles (filtro: -1 = todos). */
public final class SchematicListRequestPacket {

    private final int formatFilter;

    public SchematicListRequestPacket(int formatFilter) {
        this.formatFilter = formatFilter;
    }

    public static void encode(SchematicListRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.formatFilter);
    }

    public static SchematicListRequestPacket decode(FriendlyByteBuf buf) {
        return new SchematicListRequestPacket(buf.readInt());
    }

    public static void handle(SchematicListRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SchematicFormat filter = null;
            SchematicFormat[] values = SchematicFormat.values();
            if (msg.formatFilter >= 0 && msg.formatFilter < values.length) {
                filter = values[msg.formatFilter];
            }
            List<String> files = SchematicManager.list(filter);
            PacketHandler.sendToClient(player, new SchematicListPacket(files));
        });
        c.setPacketHandled(true);
    }
}
