package com.fantasticterraform.network;

import com.fantasticterraform.ambience.AmbienceZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S-&gt;C: inicia o detiene la reproduccion de una zona de ambiente. Soporta MEZCLA de
 * hasta 3 capas de sonido con volumenes independientes (se reproducen simultaneamente).
 */
public final class AmbienceTriggerPacket {

    public final boolean start;
    public final String zoneId;
    public final String sound;
    public final float volume;
    public final float pitch;
    public final boolean loop;
    public final double fadeSeconds;
    public final String sound2;
    public final float volume2;
    public final String sound3;
    public final float volume3;

    public AmbienceTriggerPacket(boolean start, String zoneId, String sound, float volume,
                                 float pitch, boolean loop, double fadeSeconds,
                                 String sound2, float volume2, String sound3, float volume3) {
        this.start = start;
        this.zoneId = zoneId;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;
        this.fadeSeconds = fadeSeconds;
        this.sound2 = sound2 == null ? "" : sound2;
        this.volume2 = volume2;
        this.sound3 = sound3 == null ? "" : sound3;
        this.volume3 = volume3;
    }

    public static AmbienceTriggerPacket start(AmbienceZone zone) {
        return new AmbienceTriggerPacket(true, zone.id, zone.sound, zone.volume, zone.pitch, zone.loop,
                zone.fadeSeconds, zone.sound2, zone.volume2, zone.sound3, zone.volume3);
    }

    public static AmbienceTriggerPacket stop(String zoneId) {
        return new AmbienceTriggerPacket(false, zoneId, "", 0F, 1F, false, 1.0D, "", 0F, "", 0F);
    }

    public static void encode(AmbienceTriggerPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.start);
        buf.writeUtf(m.zoneId);
        buf.writeUtf(m.sound);
        buf.writeFloat(m.volume);
        buf.writeFloat(m.pitch);
        buf.writeBoolean(m.loop);
        buf.writeDouble(m.fadeSeconds);
        buf.writeUtf(m.sound2);
        buf.writeFloat(m.volume2);
        buf.writeUtf(m.sound3);
        buf.writeFloat(m.volume3);
    }

    public static AmbienceTriggerPacket decode(FriendlyByteBuf buf) {
        return new AmbienceTriggerPacket(buf.readBoolean(), buf.readUtf(), buf.readUtf(),
                buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readDouble(),
                buf.readUtf(), buf.readFloat(), buf.readUtf(), buf.readFloat());
    }

    public static void handle(AmbienceTriggerPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.ambience.client.ClientAmbiencePlayer.handle(m)));
        c.setPacketHandled(true);
    }
}
