package com.fantasticterraform.network;

import com.fantasticterraform.schematics.SchematicFormat;
import com.fantasticterraform.schematics.SchematicManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: guarda la seleccion activa como schematic en el formato indicado. */
public final class SaveSchematicPacket {

    private final SchematicFormat format;
    private final String name;

    public SaveSchematicPacket(SchematicFormat format, String name) {
        this.format = format;
        this.name = name;
    }

    public static void encode(SaveSchematicPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.format);
        buf.writeUtf(msg.name);
    }

    public static SaveSchematicPacket decode(FriendlyByteBuf buf) {
        return new SaveSchematicPacket(buf.readEnum(SchematicFormat.class), buf.readUtf());
    }

    public static void handle(SaveSchematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SchematicManager.save(player, msg.format, msg.name);
        });
        c.setPacketHandled(true);
    }
}
