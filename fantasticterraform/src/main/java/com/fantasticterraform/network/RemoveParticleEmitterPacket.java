package com.fantasticterraform.network;

import com.fantasticterraform.particles.ParticleEmitterManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Bidireccional: C->S para que un OP elimine un emisor; S->C para que el cliente deje
 * de renderizarlo. El sentido se distingue por la presencia de remitente.
 */
public final class RemoveParticleEmitterPacket {

    private final String id;

    public RemoveParticleEmitterPacket(String id) {
        this.id = id;
    }

    public static void encode(RemoveParticleEmitterPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.id);
    }

    public static RemoveParticleEmitterPacket decode(FriendlyByteBuf buf) {
        return new RemoveParticleEmitterPacket(buf.readUtf());
    }

    public static void handle(RemoveParticleEmitterPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sender = c.getSender();
        if (sender != null) {
            // C->S: solo OP puede eliminar definitivamente.
            c.enqueueWork(() -> {
                ServerPlayer player = PacketHandler.requireOp(c);
                if (player != null) {
                    ParticleEmitterManager.get().remove(m.id);
                }
            });
        } else {
            // S->C: el cliente deja de renderizar el emisor.
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.fantasticterraform.particles.client.ClientParticleRenderer.removeEmitter(m.id)));
        }
        c.setPacketHandled(true);
    }
}
