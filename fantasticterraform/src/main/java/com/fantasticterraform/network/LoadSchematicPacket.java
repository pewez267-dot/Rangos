package com.fantasticterraform.network;

import com.fantasticterraform.schematics.SchematicManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: carga un schematic al portapapeles del jugador. */
public final class LoadSchematicPacket {

    private final String fileName;

    public LoadSchematicPacket(String fileName) {
        this.fileName = fileName;
    }

    public static void encode(LoadSchematicPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.fileName);
    }

    public static LoadSchematicPacket decode(FriendlyByteBuf buf) {
        return new LoadSchematicPacket(buf.readUtf());
    }

    public static void handle(LoadSchematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SchematicManager.loadIntoClipboard(player, msg.fileName);
        });
        c.setPacketHandled(true);
    }
}
