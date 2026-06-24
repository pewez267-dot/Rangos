package com.fantasticterraform.network;

import com.fantasticterraform.schematics.SchematicManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C-&gt;S: carga un schematic y lo pega en {@code origin} con rotacion, espejo y escala. */
public final class PasteSchematicPacket {

    private final String fileName;
    private final BlockPos origin;
    private final int rotation;
    private final boolean mirrorX;
    private final boolean mirrorY;
    private final boolean mirrorZ;
    private final int scale;

    public PasteSchematicPacket(String fileName, BlockPos origin, int rotation) {
        this(fileName, origin, rotation, false, false, false, 1);
    }

    public PasteSchematicPacket(String fileName, BlockPos origin, int rotation,
                                boolean mirrorX, boolean mirrorY, boolean mirrorZ, int scale) {
        this.fileName = fileName;
        this.origin = origin;
        this.rotation = rotation;
        this.mirrorX = mirrorX;
        this.mirrorY = mirrorY;
        this.mirrorZ = mirrorZ;
        this.scale = scale;
    }

    public static void encode(PasteSchematicPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.fileName);
        buf.writeBlockPos(msg.origin);
        buf.writeInt(msg.rotation);
        buf.writeBoolean(msg.mirrorX);
        buf.writeBoolean(msg.mirrorY);
        buf.writeBoolean(msg.mirrorZ);
        buf.writeInt(msg.scale);
    }

    public static PasteSchematicPacket decode(FriendlyByteBuf buf) {
        return new PasteSchematicPacket(buf.readUtf(), buf.readBlockPos(), buf.readInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readInt());
    }

    public static void handle(PasteSchematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SchematicManager.loadAndPaste(player, msg.fileName, msg.origin, rotationFromIndex(msg.rotation),
                    msg.mirrorX, msg.mirrorY, msg.mirrorZ, msg.scale);
        });
        c.setPacketHandled(true);
    }

    private static Rotation rotationFromIndex(int index) {
        switch (((index % 4) + 4) % 4) {
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                return Rotation.NONE;
        }
    }
}
