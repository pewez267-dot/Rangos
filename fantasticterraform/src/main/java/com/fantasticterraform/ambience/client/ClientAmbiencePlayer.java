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
            if (PLAYING.containsKey(packet.zoneId)) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(packet.sound);
            SoundEvent event = id == null ? null : ForgeRegistries.SOUND_EVENTS.getValue(id);
            if (event == null) {
                return;
            }
            FadingSound sound = new FadingSound(event, packet.volume, packet.pitch, packet.loop, packet.fadeSeconds);
            PLAYING.put(packet.zoneId, sound);
            mc.getSoundManager().play(sound);
        } else {
            FadingSound sound = PLAYING.remove(packet.zoneId);
            if (sound != null) {
                sound.fadeOut();
            }
        }
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
