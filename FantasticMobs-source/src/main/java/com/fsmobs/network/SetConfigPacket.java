package com.fsmobs.network;

import com.fsmobs.MobControl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Cliente -> servidor: cambia un valor de la configuracion. El servidor valida que sea OP, aplica
 * el cambio (que se usa en vivo por el manejador de spawns), guarda y devuelve la config actualizada.
 *
 * op: 0=radio(value), 1=multiplicador(value), 2=tope categoria(id,value), 3=tope mob(id,value<0 quita), 4=reset.
 */
public class SetConfigPacket {

    public static final int OP_RADIUS = 0;
    public static final int OP_MULT = 1;
    public static final int OP_CATEGORY = 2;
    public static final int OP_TYPE = 3;
    public static final int OP_RESET = 4;

    private final int op;
    private final String id;
    private final double value;

    public SetConfigPacket(int op, String id, double value) {
        this.op = op;
        this.id = id == null ? "" : id;
        this.value = value;
    }

    public static void encode(SetConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.op);
        buf.writeUtf(msg.id);
        buf.writeDouble(msg.value);
    }

    public static SetConfigPacket decode(FriendlyByteBuf buf) {
        return new SetConfigPacket(buf.readVarInt(), buf.readUtf(), buf.readDouble());
    }

    public static void handle(SetConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null || !sp.hasPermissions(2)) {
                return;
            }
            switch (msg.op) {
                case OP_RADIUS -> MobControl.setRadius((int) Math.round(msg.value));
                case OP_MULT -> MobControl.setMultiplier(msg.value);
                case OP_CATEGORY -> MobControl.setCategoryCap(msg.id, (int) Math.round(msg.value));
                case OP_TYPE -> MobControl.setTypeCap(msg.id, (int) Math.round(msg.value));
                case OP_RESET -> MobControl.reset();
                default -> {
                    return;
                }
            }
            MobControl.save();
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncConfigPacket());
        });
        c.setPacketHandled(true);
    }
}
