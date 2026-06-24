package com.fantasticterraform.network;

import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C-&gt;S: transforma la region activa sin re-marcar puntos. Equivale a los comandos
 * //expand, //contract, //outset y //shift de WorldEdit.
 */
public final class SelectionTransformPacket {

    /** 0 = EXPAND (todos los ejes), 1 = CONTRACT, 2 = OUTSET (solo X/Z), 3 = SHIFT. */
    private final int mode;
    private final int amount;
    private final int dx;
    private final int dy;
    private final int dz;

    public SelectionTransformPacket(int mode, int amount, int dx, int dy, int dz) {
        this.mode = mode;
        this.amount = amount;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public static void encode(SelectionTransformPacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.mode);
        buf.writeInt(m.amount);
        buf.writeInt(m.dx);
        buf.writeInt(m.dy);
        buf.writeInt(m.dz);
    }

    public static SelectionTransformPacket decode(FriendlyByteBuf buf) {
        return new SelectionTransformPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(SelectionTransformPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            PlayerSelection sel = SelectionManager.get(player);
            if (sel.getShape() == null && m.mode != 3) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7cNecesitas una seleccion valida para transformarla."));
                return;
            }
            int n = Math.max(0, m.amount);
            switch (m.mode) {
                case 0:
                    sel.resize(n, false);
                    break;
                case 1:
                    sel.resize(-n, false);
                    break;
                case 2:
                    sel.resize(n, true);
                    break;
                case 3:
                    sel.shift(m.dx, m.dy, m.dz);
                    break;
                default:
                    break;
            }
            PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(sel));
        });
        c.setPacketHandled(true);
    }
}
