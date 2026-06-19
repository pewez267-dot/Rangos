package com.fantasticterraform.network;

import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: ajusta la altura del cilindro de seleccion (slider del HUD). */
public final class SetCylinderHeightPacket {

    private final int height;

    public SetCylinderHeightPacket(int height) {
        this.height = height;
    }

    public static void encode(SetCylinderHeightPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.height);
    }

    public static SetCylinderHeightPacket decode(FriendlyByteBuf buf) {
        return new SetCylinderHeightPacket(buf.readInt());
    }

    public static void handle(SetCylinderHeightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            PlayerSelection sel = SelectionManager.get(player);
            sel.setCylinderHeight(msg.height);
            PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(sel));
        });
        c.setPacketHandled(true);
    }
}
