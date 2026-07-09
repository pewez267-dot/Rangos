package com.fsmobs.network;

import com.fsmobs.MobControl;
import com.fsmobs.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: actualiza la configuracion cacheada (refresca la GUI si esta abierta). */
public class SyncConfigPacket {

    private MobControl.Snapshot snapshot;

    public SyncConfigPacket() {}

    private SyncConfigPacket(MobControl.Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(SyncConfigPacket msg, FriendlyByteBuf buf) {
        MobControl.writeSnapshot(buf);
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncConfigPacket(MobControl.Snapshot.read(buf));
    }

    public static void handle(SyncConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientState.updateConfig(msg.snapshot)));
        c.setPacketHandled(true);
    }
}
