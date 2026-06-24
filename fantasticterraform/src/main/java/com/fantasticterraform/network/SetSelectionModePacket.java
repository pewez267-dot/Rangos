package com.fantasticterraform.network;

import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: cambia el modo de seleccion activo (limpia la seleccion previa). */
public final class SetSelectionModePacket {

    private final int modeOrdinal;

    public SetSelectionModePacket(SelectionType type) {
        this.modeOrdinal = type.ordinal();
    }

    private SetSelectionModePacket(int ordinal) {
        this.modeOrdinal = ordinal;
    }

    public static void encode(SetSelectionModePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modeOrdinal);
    }

    public static SetSelectionModePacket decode(FriendlyByteBuf buf) {
        return new SetSelectionModePacket(buf.readInt());
    }

    public static void handle(SetSelectionModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SelectionType[] values = SelectionType.values();
            if (msg.modeOrdinal < 0 || msg.modeOrdinal >= values.length) {
                return;
            }
            PlayerSelection sel = SelectionManager.get(player);
            sel.setType(values[msg.modeOrdinal]);
            PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(sel));
        });
        c.setPacketHandled(true);
    }
}
