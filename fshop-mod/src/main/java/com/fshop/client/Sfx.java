package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, pleasant UI sounds built ONLY on the coin/orb pickup family
 * (EXPERIENCE_ORB_PICKUP) and a soft item pop (ITEM_PICKUP), plus a quiet
 * toast pop for page changes. No book/bookshelf sounds, no note-block/piano,
 * no amethyst, no paper rustle. Pitches stay low-to-mid (0.8-1.3) and volumes
 * stay quiet (0.2-0.45) to keep everything chill instead of harsh or tinny.
 */
public final class Sfx {
   private Sfx() {
   }

   private static void play(SoundInstance sound) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getSoundManager() != null) {
         mc.getSoundManager().play(sound);
      }
   }

   /** A single soft coin-sparkle note at the given pitch (used for the open melody). */
   public static void spark(float pitch) {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, 0.32F));
   }

   /** Soft, rounded pop for buttons, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0F, 0.35F));
   }

   /** Quick, quiet tick for rapid +/- stepping (hold to repeat). */
   public static void step() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.2F, 0.2F));
   }

   /** Quiet, soft pop when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 1.0F, 0.3F));
   }

   /** Warm, satisfying coin chime for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.85F, 0.45F));
   }

   /** Soft, slightly brighter coin chime for opening or picking inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.05F, 0.3F));
   }
}
