package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, aesthetic UI sounds for the shop screens (no amethyst). Played through
 * the UI sound channel so they stay quiet and pleasant.
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

   /** Light click for buttons, steppers, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.35F));
   }

   /** Soft page flip when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.1F));
   }

   /** Gentle chime for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.25F));
   }

   /** Muted note for opening/selecting inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP, 1.5F));
   }
}
