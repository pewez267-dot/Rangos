package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft UI sounds for the shop screens: a light button click, a paper page flip
 * and a gentle orb "ding" for success. No note-block/piano or amethyst tones.
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

   /** Light UI click for buttons, steppers, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F));
   }

   /** Soft paper flip when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2F));
   }

   /** Gentle "ding" for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.3F));
   }

   /** Subtle click for opening/selecting inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.6F));
   }
}
