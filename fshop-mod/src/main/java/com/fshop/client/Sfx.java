package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, chill "brillitos" UI sounds built on the enchanted-bookshelf chimes
 * (a smooth magical twinkle, not a percussive note-block/piano tone) and the
 * toast pop for page changes. Everything is played quiet and slightly
 * pitched-up so it feels light and aesthetic instead of harsh. No amethyst,
 * no note-block/piano, no book-page paper rustle.
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

   /** A single soft twinkle at the given pitch (used for the open melody). */
   public static void spark(float pitch) {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, pitch, 0.4F));
   }

   /** Soft twinkle tap for buttons, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, 1.5F, 0.32F));
   }

   /** Even softer/higher twinkle for rapid +/- stepping (hold to repeat). */
   public static void step() {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, 1.85F, 0.2F));
   }

   /** Quiet, soft pop when changing pages (no paper rustle). */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 1.35F, 0.35F));
   }

   /** Gentle twinkly chime for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED, 1.15F, 0.4F));
   }

   /** Soft select tone for opening or picking inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, 1.7F, 0.3F));
   }
}
