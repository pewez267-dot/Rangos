package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * A single, calm UI sound for the whole shop. The user asked for ONE soft,
 * chill, mid-range tone (not sharp/high, not annoying) reused everywhere, so
 * every interaction plays the same warm note-block GUITAR pluck at a mid pitch
 * and low volume. The +/- auto-repeat stays silent so holding a button is
 * never noisy.
 */
public final class Sfx {
   private Sfx() {
   }

   /** The one and only shop sound: warm, mid, soft. */
   private static void tone() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getSoundManager() != null) {
         SoundInstance s = SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_GUITAR.value(), 1.0F, 0.16F);
         mc.getSoundManager().play(s);
      }
   }

   public static void spark(float pitch) {
      tone();
   }

   public static void click() {
      tone();
   }

   public static void step() {
      tone();
   }

   public static void page() {
      tone();
   }

   public static void success() {
      tone();
   }

   public static void select() {
      tone();
   }
}
