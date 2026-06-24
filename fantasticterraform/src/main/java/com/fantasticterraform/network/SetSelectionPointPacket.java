package com.fantasticterraform.network;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C->S: marca un punto de seleccion. {@code left=true} es click izquierdo (marca/
 * reemplaza P1 o agrega vertice); {@code left=false} es click derecho (marca P2 o
 * cierra poligono/freehand). El punto viene del raycasting manual del cliente.
 */
public final class SetSelectionPointPacket {

    private final boolean left;
    private final BlockPos pos;

    public SetSelectionPointPacket(boolean left, BlockPos pos) {
        this.left = left;
        this.pos = pos;
    }

    public static void encode(SetSelectionPointPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.left);
        buf.writeBlockPos(msg.pos);
    }

    public static SetSelectionPointPacket decode(FriendlyByteBuf buf) {
        return new SetSelectionPointPacket(buf.readBoolean(), buf.readBlockPos());
    }

    public static void handle(SetSelectionPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            PlayerSelection sel = SelectionManager.get(player);
            if (msg.left) {
                sel.leftClick(msg.pos,
                        TerraformConfig.GENERAL.maxPolygonVertices.get(),
                        TerraformConfig.GENERAL.maxFreehandPoints.get());
            } else {
                sel.rightClick(msg.pos);
            }
            PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(sel));
        });
        c.setPacketHandled(true);
    }
}
