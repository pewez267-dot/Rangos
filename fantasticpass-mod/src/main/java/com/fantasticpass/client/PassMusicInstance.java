package com.fantasticpass.client;

import com.fantasticpass.sound.PassSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class PassMusicInstance extends AbstractTickableSoundInstance {
   private static final float TARGET_VOLUME = 0.85F;
   public static boolean muted = false;

   public PassMusicInstance() {
      super((SoundEvent)PassSounds.PASS_MUSIC.get(), SoundSource.MUSIC, RandomSource.create());
      this.looping = true;
      this.delay = 0;
      this.relative = true;
      this.attenuation = Attenuation.NONE;
      this.volume = muted ? 1.0E-4F : 0.45F;
   }

   public void tick() {
      if (muted) {
         this.volume = Math.max(1.0E-4F, this.volume - 0.06F);
      } else if (this.volume < 0.85F) {
         this.volume = Math.min(0.85F, this.volume + 0.03F);
      }
   }
}
