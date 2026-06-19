package com.fantasticterraform.network;

import com.fantasticterraform.ambience.AmbienceZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S->C: inicia o detiene la reproduccion del sonido de una zona de ambiente. */
public final class AmbienceTriggerPacket {

    public final boolean start;
    public final String zoneId;
    public final String sound;
    public final float volume;
    public final float pitch;
    public final boolean loop;
    public final double fadeSeconds;

    public AmbienceTriggerPacket(boolean start, String zoneId, String sound, float volume,
                                 float pitch, boolean loop, double fadeSeconds) {
        this.start = start;
        this.zoneId = zoneId;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;
        this.fadeSeconds = fadeSeconds;
    }

    public static AmbienceTriggerPacket start(AmbienceZone zone) {
        return new AmbienceTriggerPacket(true, zone.id, zone.sound, zone.volume, zone.pitch, zone.loop, zone.fadeSeconds);
    }

    public static AmbienceTriggerPacket stop(String zoneId) {
        return new AmbienceTriggerPacket(false, zoneId, "", 0F, 1F, false, 1.0D);
    }

    public static void encode(AmbienceTriggerPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.start);
        buf.writeUtf(m.zoneId);
        buf.writeUtf(m.sound);
        buf.writeFloat(m.volume);
        buf.writeFloat(m.pitch);
        buf.writeBoolean(m.loop);
        buf.writeDouble(m.fadeSeconds);
    }

    public static AmbienceTriggerPacket decode(FriendlyByteBuf buf) {
        return new AmbienceTriggerPacket(buf.readBoolean(), buf.readUtf(), buf.readUtf(),
                buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readDouble());
    }

    public static void handle(AmbienceTriggerPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.ambience.client.ClientAmbiencePlayer.handle(m)));
        c.setPacketHandled(true);
    }
}
