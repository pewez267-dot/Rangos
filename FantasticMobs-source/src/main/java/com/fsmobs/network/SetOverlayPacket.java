package com.fsmobs.network;

import com.fsmobs.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: enciende/apaga el panel de estadisticas en pantalla (comando /fsmobs stats). */
public class SetOverlayPacket {

    private final boolean on;

    public SetOverlayPacket(boolean on) {
        this.on = on;
    }

    public static void encode(SetOverlayPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.on);
    }

    public static SetOverlayPacket decode(FriendlyByteBuf buf) {
        return new SetOverlayPacket(buf.readBoolean());
    }

    public static void handle(SetOverlayPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientState.setOverlay(msg.on)));
        c.setPacketHandled(true);
    }
}
