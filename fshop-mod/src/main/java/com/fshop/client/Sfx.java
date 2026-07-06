package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Soft, pretty UI sounds for the shop, built on the note-block CHIME
 * (glockenspiel/bell - the game's gentle "sparkle") and BELL (a warm resolve
 * for confirmations), with a quiet toast swoosh for page turns. This replaces
 * the earlier flat cloth/pickup palette that felt too plain: pitches stay in a
 * pleasant twinkly range and volumes stay low so it reads as chill and
 * aesthetic, never harsh or repetitive. No harp/piano, orb, amethyst or books.
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

   /** A single soft chime note at the given pitch (used for the open twinkle). */
   public static void spark(float pitch) {
      ui(SoundEvents.NOTE_BLOCK_CHIME, pitch, 0.34F);
   }

   /** Soft, bright chime tick for buttons, coins and navigation. */
   public static void click() {
      ui(SoundEvents.NOTE_BLOCK_CHIME, 1.35F, 0.22F);
   }

   /** Very quiet high chime for rapid +/- stepping (hold to repeat). */
   public static void step() {
      ui(SoundEvents.NOTE_BLOCK_CHIME, 1.6F, 0.12F);
   }

   /** Quiet, soft swoosh when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 0.9F, 0.28F));
   }

   /** Warm, satisfying bell chime for a successful purchase, sale, save or collect. */
   public static void success() {
      ui(SoundEvents.NOTE_BLOCK_BELL, 1.1F, 0.4F);
   }

   /** Soft chime when opening or picking inside a menu. */
   public static void select() {
      ui(SoundEvents.NOTE_BLOCK_CHIME, 1.15F, 0.28F);
   }
}
