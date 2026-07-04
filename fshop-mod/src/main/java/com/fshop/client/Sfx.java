package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, pleasant UI sounds. Pitches stay in a natural, low-shrill range
 * (0.85-1.3) instead of squeaky highs, and volumes stay quiet (0.25-0.5) so
 * everything feels chill and aesthetic rather than harsh or tinny. No
 * amethyst, no note-block/piano, no paper rustle, no screechy high pitches.
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

   /** A single soft chime note at the given pitch (used for the open melody). */
   public static void spark(float pitch) {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED, pitch, 0.42F));
   }

   /** Soft, rounded tap for buttons, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.05F, 0.4F));
   }

   /** Quick, quiet tick for rapid +/- stepping (hold to repeat). */
   public static void step() {
      play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.25F, 0.22F));
   }

   /** Quiet, soft pop when changing pages (no paper rustle). */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 1.05F, 0.35F));
   }

   /** Warm, satisfying chime for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.95F, 0.5F));
   }

   /** Soft, slightly brighter chime for opening or picking inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, 1.1F, 0.38F));
   }
}
