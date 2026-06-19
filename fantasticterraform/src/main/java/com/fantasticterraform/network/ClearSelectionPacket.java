package com.fantasticterraform.network;

import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: limpia por completo la seleccion activa (boton del HUD). */
public final class ClearSelectionPacket {

    public ClearSelectionPacket() {
    }

    public static void encode(ClearSelectionPacket msg, FriendlyByteBuf buf) {
    }

    public static ClearSelectionPacket decode(FriendlyByteBuf buf) {
        return new ClearSelectionPacket();
    }

    public static void handle(ClearSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            PlayerSelection sel = SelectionManager.get(player);
            sel.clear();
            PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(sel));
        });
        c.setPacketHandled(true);
    }
}
