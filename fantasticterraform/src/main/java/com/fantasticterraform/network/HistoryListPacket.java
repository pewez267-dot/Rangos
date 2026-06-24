package com.fantasticterraform.network;

import com.fantasticterraform.history.HistoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * S-&gt;C: lista del historial de edicion para mostrarlo de forma visual (etiquetas +
 * numero de bloques de cada operacion de deshacer, mas la profundidad de rehacer).
 */
public final class HistoryListPacket {

    public final List<String> labels;
    public final int[] sizes;
    public final int redoDepth;

    public HistoryListPacket(List<String> labels, int[] sizes, int redoDepth) {
        this.labels = labels;
        this.sizes = sizes;
        this.redoDepth = redoDepth;
    }

    public static HistoryListPacket fromPlayer(ServerPlayer player) {
        HistoryManager h = HistoryManager.get();
        List<String> labels = h.undoLabels(player.getUUID());
        List<Integer> sz = h.undoSizes(player.getUUID());
        int[] sizes = new int[sz.size()];
        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = sz.get(i);
        }
        return new HistoryListPacket(labels, sizes, h.redoDepth(player.getUUID()));
    }

    public static void encode(HistoryListPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.labels.size());
        for (int i = 0; i < m.labels.size(); i++) {
            buf.writeUtf(m.labels.get(i));
            buf.writeVarInt(m.sizes[i]);
        }
        buf.writeVarInt(m.redoDepth);
    }

    public static HistoryListPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        java.util.List<String> labels = new java.util.ArrayList<>(n);
        int[] sizes = new int[n];
        for (int i = 0; i < n; i++) {
            labels.add(buf.readUtf());
            sizes[i] = buf.readVarInt();
        }
        int redo = buf.readVarInt();
        return new HistoryListPacket(labels, sizes, redo);
    }

    public static void handle(HistoryListPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientHistoryState.update(m)));
        c.setPacketHandled(true);
    }
}
