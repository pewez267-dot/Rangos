package com.fsmobs.network;

import com.fsmobs.MobControl;
import com.fsmobs.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: abre la GUI con la configuracion actual. */
public class OpenConfigPacket {

    private MobControl.Snapshot snapshot;

    public OpenConfigPacket() {}

    private OpenConfigPacket(MobControl.Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(OpenConfigPacket msg, FriendlyByteBuf buf) {
        MobControl.writeSnapshot(buf);
    }

    public static OpenConfigPacket decode(FriendlyByteBuf buf) {
        return new OpenConfigPacket(MobControl.Snapshot.read(buf));
    }

    public static void handle(OpenConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientState.openConfig(msg.snapshot)));
        c.setPacketHandled(true);
    }
}
