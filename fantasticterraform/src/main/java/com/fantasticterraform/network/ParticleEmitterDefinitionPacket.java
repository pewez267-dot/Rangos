package com.fantasticterraform.network;

import com.fantasticterraform.particles.ParticleEmitter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S->C: definicion completa de un emisor. Se envia una sola vez cuando el jugador
 * entra en rango; el cliente genera las particulas localmente a partir de ella.
 */
public final class ParticleEmitterDefinitionPacket {

    public final ParticleEmitter emitter;

    public ParticleEmitterDefinitionPacket(ParticleEmitter emitter) {
        this.emitter = emitter;
    }

    public static ParticleEmitterDefinitionPacket of(ParticleEmitter emitter) {
        return new ParticleEmitterDefinitionPacket(emitter.copy());
    }

    public static void encode(ParticleEmitterDefinitionPacket m, FriendlyByteBuf buf) {
        ParticleEmitter e = m.emitter;
        buf.writeUtf(e.id);
        buf.writeUtf(e.dimension);
        buf.writeDouble(e.x);
        buf.writeDouble(e.y);
        buf.writeDouble(e.z);
        buf.writeUtf(e.particleType);
        buf.writeDouble(e.emissionRate);
        buf.writeDouble(e.vx);
        buf.writeDouble(e.vy);
        buf.writeDouble(e.vz);
        buf.writeFloat(e.red);
        buf.writeFloat(e.green);
        buf.writeFloat(e.blue);
        buf.writeFloat(e.size);
        buf.writeDouble(e.visibilityRadius);
        buf.writeLong(e.durationTicks);
        buf.writeBoolean(e.hasRegion);
        buf.writeInt(e.minX);
        buf.writeInt(e.minY);
        buf.writeInt(e.minZ);
        buf.writeInt(e.maxX);
        buf.writeInt(e.maxY);
        buf.writeInt(e.maxZ);
    }

    public static ParticleEmitterDefinitionPacket decode(FriendlyByteBuf buf) {
        ParticleEmitter e = new ParticleEmitter();
        e.id = buf.readUtf();
        e.dimension = buf.readUtf();
        e.x = buf.readDouble();
        e.y = buf.readDouble();
        e.z = buf.readDouble();
        e.particleType = buf.readUtf();
        e.emissionRate = buf.readDouble();
        e.vx = buf.readDouble();
        e.vy = buf.readDouble();
        e.vz = buf.readDouble();
        e.red = buf.readFloat();
        e.green = buf.readFloat();
        e.blue = buf.readFloat();
        e.size = buf.readFloat();
        e.visibilityRadius = buf.readDouble();
        e.durationTicks = buf.readLong();
        e.hasRegion = buf.readBoolean();
        e.minX = buf.readInt();
        e.minY = buf.readInt();
        e.minZ = buf.readInt();
        e.maxX = buf.readInt();
        e.maxY = buf.readInt();
        e.maxZ = buf.readInt();
        return new ParticleEmitterDefinitionPacket(e);
    }

    public static void handle(ParticleEmitterDefinitionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.particles.client.ClientParticleRenderer.addEmitter(m.emitter)));
        c.setPacketHandled(true);
    }
}
