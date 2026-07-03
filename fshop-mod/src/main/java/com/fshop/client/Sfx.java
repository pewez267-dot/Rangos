package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, twinkly UI sounds ("estrellitas"): gentle sparkle tones for taps/steps,
 * a quiet paper flip for pages and a short ascending sparkle melody when the
 * market opens. Low volume, chill and aesthetic (no piano/note-block, no
 * amethyst).
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

   /** A single soft sparkle note at the given pitch (used for the open melody). */
   public static void spark(float pitch) {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, 0.5F));
   }

   /** Soft sparkle tap for buttons, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6F, 0.4F));
   }

   /** Softer/higher twinkle for rapid +/- stepping (hold to repeat). */
   public static void step() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.95F, 0.26F));
   }

   /** Quiet paper flip when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.25F, 0.55F));
   }

   /** Gentle chime for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.35F, 0.55F));
   }

   /** Soft select tone for opening or picking inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 2.0F, 0.42F));
   }
}
