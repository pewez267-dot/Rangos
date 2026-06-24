package com.fantasticpass.client;

import com.fantasticpass.sound.PassSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Looping, non-positional UI music instance for the Battle Pass screen. Plays in the MUSIC
 * sound category so it respects the player's music volume slider, fades in via a soft start
 * volume, and loops until explicitly stopped when the screen closes.
 */
public final class PassMusicInstance extends AbstractTickableSoundInstance {

    private static final float TARGET_VOLUME = 0.7f;

    public PassMusicInstance() {
        super(PassSounds.PASS_MUSIC.get(), SoundSource.MUSIC, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.0f; // ramps up in tick() for a smooth fade-in
    }

    @Override
    public void tick() {
        if (this.volume < TARGET_VOLUME) {
            this.volume = Math.min(TARGET_VOLUME, this.volume + 0.02f);
        }
    }
}
