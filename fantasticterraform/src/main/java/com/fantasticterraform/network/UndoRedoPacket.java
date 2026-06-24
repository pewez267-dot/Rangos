package com.fantasticterraform.network;

import com.fantasticterraform.history.HistoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: deshacer ({@code undo=true}) o rehacer ({@code undo=false}), {@code count} veces. */
public final class UndoRedoPacket {

    private final boolean undo;
    private final int count;

    public UndoRedoPacket(boolean undo) {
        this(undo, 1);
    }

    public UndoRedoPacket(boolean undo, int count) {
        this.undo = undo;
        this.count = count;
    }

    public static void encode(UndoRedoPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.undo);
        buf.writeVarInt(msg.count);
    }

    public static UndoRedoPacket decode(FriendlyByteBuf buf) {
        return new UndoRedoPacket(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(UndoRedoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            int n = Math.max(1, Math.min(64, msg.count));
            for (int i = 0; i < n; i++) {
                boolean ok = msg.undo ? HistoryManager.get().undo(player) : HistoryManager.get().redo(player);
                if (!ok) {
                    break;
                }
            }
            PacketHandler.sendToClient(player, HistoryListPacket.fromPlayer(player));
        });
        c.setPacketHandled(true);
    }
}
