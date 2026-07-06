package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Very soft, mellow UI sounds for the shop, built on the note-block FLUTE (the
 * warmest, gentlest instrument) plus a quiet toast swoosh for page turns. All
 * volumes are deliberately tiny (0.06-0.18) and the rapid +/- auto-repeat no
 * longer plays a sound at all, so nothing is ever loud, harsh or "noisy".
 * No chime/bell, harp/piano, orb, amethyst or books.
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

   private static void ui(Holder<SoundEvent> sound, float pitch, float volume) {
      play(SimpleSoundInstance.forUI(sound.value(), pitch, volume));
   }

   /** A single soft flute note (used once when the shop opens). */
   public static void spark(float pitch) {
      ui(SoundEvents.NOTE_BLOCK_FLUTE, pitch, 0.14F);
   }

   /** Soft, low flute tap for buttons, coins and navigation. */
   public static void click() {
      ui(SoundEvents.NOTE_BLOCK_FLUTE, 1.2F, 0.12F);
   }

   /** Barely-there tick for a single +/- press (auto-repeat is silent). */
   public static void step() {
      ui(SoundEvents.NOTE_BLOCK_FLUTE, 1.35F, 0.08F);
   }

   /** Very quiet, soft swoosh when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 0.9F, 0.14F));
   }

   /** Gentle, warm flute note confirming a purchase, sale, save or collect. */
   public static void success() {
      ui(SoundEvents.NOTE_BLOCK_FLUTE, 1.0F, 0.2F);
   }

   /** Soft flute note when opening or picking inside a menu. */
   public static void select() {
      ui(SoundEvents.NOTE_BLOCK_FLUTE, 1.1F, 0.12F);
   }
}
