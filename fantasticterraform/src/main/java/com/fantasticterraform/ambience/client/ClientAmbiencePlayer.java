package com.fantasticterraform.ambience.client;

import com.fantasticterraform.network.AmbienceTriggerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reproduce el sonido de las zonas de ambiente del lado cliente, con fade in/out real
 * mediante una instancia de sonido tickable cuyo volumen se rampa cada tick.
 */
public final class ClientAmbiencePlayer {

    private static final Map<String, FadingSound> PLAYING = new ConcurrentHashMap<>();

    private ClientAmbiencePlayer() {
    }

    public static void handle(AmbienceTriggerPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (packet.start) {
            startLayer(mc, packet.zoneId, 0, packet.sound, packet.volume, packet.pitch, packet.loop, packet.fadeSeconds);
            startLayer(mc, packet.zoneId, 1, packet.sound2, packet.volume2, packet.pitch, packet.loop, packet.fadeSeconds);
            startLayer(mc, packet.zoneId, 2, packet.sound3, packet.volume3, packet.pitch, packet.loop, packet.fadeSeconds);
        } else {
            // Detiene (con fade) todas las capas de la zona.
            for (int layer = 0; layer < 3; layer++) {
                FadingSound sound = PLAYING.remove(key(packet.zoneId, layer));
                if (sound != null) {
                    sound.fadeOut();
                }
            }
        }
    }

    private static void startLayer(Minecraft mc, String zoneId, int layer, String soundId, float volume,
                                   float pitch, boolean loop, double fadeSeconds) {
        if (soundId == null || soundId.isEmpty() || volume <= 0.0F) {
            return;
        }
        String key = key(zoneId, layer);
        if (PLAYING.containsKey(key)) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        SoundEvent event = id == null ? null : ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (event == null) {
            return;
        }
        FadingSound sound = new FadingSound(event, volume, pitch, loop, fadeSeconds);
        PLAYING.put(key, sound);
        mc.getSoundManager().play(sound);
    }

    private static String key(String zoneId, int layer) {
        return zoneId + "#" + layer;
    }

    public static void stopAll() {
        for (FadingSound s : PLAYING.values()) {
            s.fadeOut();
        }
        PLAYING.clear();
    }

    /** Instancia de sonido con fade in/out por rampa de volumen. */
    private static final class FadingSound extends AbstractTickableSoundInstance {

        private final float targetVolume;
        private final float step;
        private boolean fadingOut;

        FadingSound(SoundEvent event, float targetVolume, float pitch, boolean loop, double fadeSeconds) {
            super(event, SoundSource.AMBIENT, RandomSource.create());
            this.targetVolume = Math.max(0.0F, targetVolume);
            this.pitch = pitch;
            this.looping = loop;
            this.volume = 0.01F;
            this.relative = true;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
            this.attenuation = Attenuation.NONE;
            double fadeTicks = Math.max(1.0D, fadeSeconds * 20.0D);
            this.step = (float) (this.targetVolume / fadeTicks);
        }

        @Override
        public void tick() {
            if (fadingOut) {
                this.volume -= step;
                if (this.volume <= 0.0F) {
                    this.volume = 0.0F;
                    this.stop();
                }
            } else if (this.volume < targetVolume) {
                this.volume = Math.min(targetVolume, this.volume + step);
            }
        }

        void fadeOut() {
            this.fadingOut = true;
            if (step <= 0.0F) {
                this.stop();
            }
        }
    }
}
