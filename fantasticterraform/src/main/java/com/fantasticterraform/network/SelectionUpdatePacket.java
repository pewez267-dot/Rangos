package com.fantasticterraform.network;

import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionShape;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S->C: sincroniza el estado de la seleccion al cliente para el wireframe y el
 * indicador del HUD. Solo se envia cuando los puntos cambian, nunca por frame.
 */
public final class SelectionUpdatePacket {

    public final int typeOrdinal;
    public final int cylinderHeight;
    public final boolean closed;
    public final boolean valid;
    public final long volume;
    public final List<BlockPos> points;

    public SelectionUpdatePacket(int typeOrdinal, int cylinderHeight, boolean closed,
                                 boolean valid, long volume, List<BlockPos> points) {
        this.typeOrdinal = typeOrdinal;
        this.cylinderHeight = cylinderHeight;
        this.closed = closed;
        this.valid = valid;
        this.volume = volume;
        this.points = points;
    }

    public static SelectionUpdatePacket fromSelection(PlayerSelection sel) {
        SelectionShape shape = sel.getShape();
        long vol = shape != null ? shape.getVolume() : 0L;
        return new SelectionUpdatePacket(sel.getType().ordinal(), sel.getCylinderHeight(),
                sel.isClosed(), shape != null, vol, new ArrayList<>(sel.getPoints()));
    }

    public static void encode(SelectionUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.typeOrdinal);
        buf.writeInt(msg.cylinderHeight);
        buf.writeBoolean(msg.closed);
        buf.writeBoolean(msg.valid);
        buf.writeLong(msg.volume);
        buf.writeInt(msg.points.size());
        for (BlockPos p : msg.points) {
            buf.writeBlockPos(p);
        }
    }

    public static SelectionUpdatePacket decode(FriendlyByteBuf buf) {
        int type = buf.readInt();
        int height = buf.readInt();
        boolean closed = buf.readBoolean();
        boolean valid = buf.readBoolean();
        long volume = buf.readLong();
        int count = buf.readInt();
        List<BlockPos> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pts.add(buf.readBlockPos());
        }
        return new SelectionUpdatePacket(type, height, closed, valid, volume, pts);
    }

    public SelectionType type() {
        SelectionType[] values = SelectionType.values();
        return typeOrdinal >= 0 && typeOrdinal < values.length ? values[typeOrdinal] : SelectionType.CUBOID;
    }

    public static void handle(SelectionUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientSelectionState.update(msg)));
        c.setPacketHandled(true);
    }
}
