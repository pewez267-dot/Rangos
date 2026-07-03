package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, chill UI sounds: a gentle "pib" pickup tone for taps/steps, a quiet
 * paper flip for pages and a soft orb chime for success. Low volume so it stays
 * aesthetic and never harsh (no piano/note-block, no amethyst).
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

   /** Soft tap for buttons, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.4F, 0.5F));
   }

   /** Even softer/higher tick for rapid +/- stepping (hold to repeat). */
   public static void step() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.7F, 0.32F));
   }

   /** Quiet paper flip when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2F, 0.55F));
   }

   /** Gentle orb chime for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.35F, 0.5F));
   }

   /** Soft select tone for opening or picking inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.75F, 0.45F));
   }
}
