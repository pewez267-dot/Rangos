package com.fantasticterraform.network;

import com.fantasticterraform.particles.ParticleEmitter;
import com.fantasticterraform.particles.ParticleEmitterManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: crea un emisor de particulas con los parametros del HUD. */
public final class CreateParticleEmitterPacket {

    private final double x;
    private final double y;
    private final double z;
    private final String particleType;
    private final double rate;
    private final double vx;
    private final double vy;
    private final double vz;
    private final float r;
    private final float g;
    private final float b;
    private final float size;
    private final double radius;
    private final long duration;

    public CreateParticleEmitterPacket(double x, double y, double z, String particleType, double rate,
                                       double vx, double vy, double vz, float r, float g, float b,
                                       float size, double radius, long duration) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.particleType = particleType;
        this.rate = rate;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.r = r;
        this.g = g;
        this.b = b;
        this.size = size;
        this.radius = radius;
        this.duration = duration;
    }

    public static void encode(CreateParticleEmitterPacket m, FriendlyByteBuf buf) {
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
        buf.writeUtf(m.particleType);
        buf.writeDouble(m.rate);
        buf.writeDouble(m.vx);
        buf.writeDouble(m.vy);
        buf.writeDouble(m.vz);
        buf.writeFloat(m.r);
        buf.writeFloat(m.g);
        buf.writeFloat(m.b);
        buf.writeFloat(m.size);
        buf.writeDouble(m.radius);
        buf.writeLong(m.duration);
    }

    public static CreateParticleEmitterPacket decode(FriendlyByteBuf buf) {
        return new CreateParticleEmitterPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readDouble(), buf.readLong());
    }

    public static void handle(CreateParticleEmitterPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            ParticleEmitter e = new ParticleEmitter();
            e.dimension = player.level().dimension().location().toString();
            e.x = m.x;
            e.y = m.y;
            e.z = m.z;
            e.particleType = m.particleType;
            e.emissionRate = m.rate;
            e.vx = m.vx;
            e.vy = m.vy;
            e.vz = m.vz;
            e.red = m.r;
            e.green = m.g;
            e.blue = m.b;
            e.size = m.size;
            e.visibilityRadius = m.radius;
            e.durationTicks = m.duration;
            ParticleEmitterManager.get().add(player, e);
        });
        c.setPacketHandled(true);
    }
}
