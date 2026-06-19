package com.fantasticterraform.network;

import com.fantasticterraform.history.HistoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: deshacer ({@code undo=true}) o rehacer ({@code undo=false}). */
public final class UndoRedoPacket {

    private final boolean undo;

    public UndoRedoPacket(boolean undo) {
        this.undo = undo;
    }

    public static void encode(UndoRedoPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.undo);
    }

    public static UndoRedoPacket decode(FriendlyByteBuf buf) {
        return new UndoRedoPacket(buf.readBoolean());
    }

    public static void handle(UndoRedoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            if (msg.undo) {
                HistoryManager.get().undo(player);
            } else {
                HistoryManager.get().redo(player);
            }
        });
        c.setPacketHandled(true);
    }
}
