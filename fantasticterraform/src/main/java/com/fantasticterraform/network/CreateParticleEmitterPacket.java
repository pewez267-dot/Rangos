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
    private final int curve;
    private final int shape;
    private final double shapeRadius;
    private final double shapeHeight;

    public CreateParticleEmitterPacket(double x, double y, double z, String particleType, double rate,
                                       double vx, double vy, double vz, float r, float g, float b,
                                       float size, double radius, long duration,
                                       int curve, int shape, double shapeRadius, double shapeHeight) {
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
        this.curve = curve;
        this.shape = shape;
        this.shapeRadius = shapeRadius;
        this.shapeHeight = shapeHeight;
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
        buf.writeInt(m.curve);
        buf.writeInt(m.shape);
        buf.writeDouble(m.shapeRadius);
        buf.writeDouble(m.shapeHeight);
    }

    public static CreateParticleEmitterPacket decode(FriendlyByteBuf buf) {
        return new CreateParticleEmitterPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readDouble(), buf.readLong(),
                buf.readInt(), buf.readInt(), buf.readDouble(), buf.readDouble());
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
            e.emissionCurve = m.curve;
            e.shape = m.shape;
            e.shapeRadius = m.shapeRadius;
            e.shapeHeight = m.shapeHeight;

            // Si hay seleccion activa, el emisor cubre toda esa area.
            com.fantasticterraform.selection.SelectionShape sel =
                    com.fantasticterraform.selection.SelectionManager.get(player).getShape();
            if (sel != null) {
                e.hasRegion = true;
                e.minX = sel.getMin().getX();
                e.minY = sel.getMin().getY();
                e.minZ = sel.getMin().getZ();
                e.maxX = sel.getMax().getX();
                e.maxY = sel.getMax().getY();
                e.maxZ = sel.getMax().getZ();
                e.x = (e.minX + e.maxX) / 2.0D;
                e.y = (e.minY + e.maxY) / 2.0D;
                e.z = (e.minZ + e.maxZ) / 2.0D;
                double dx = e.maxX - e.minX;
                double dy = e.maxY - e.minY;
                double dz = e.maxZ - e.minZ;
                double halfDiag = 0.5D * Math.sqrt(dx * dx + dy * dy + dz * dz);
                e.visibilityRadius = Math.max(m.radius, halfDiag + 16.0D);
            }
            ParticleEmitterManager.get().add(player, e);
        });
        c.setPacketHandled(true);
    }
}
