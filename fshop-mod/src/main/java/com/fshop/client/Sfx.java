package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Neutral, soft UI sounds. The old family (EXPERIENCE_ORB_PICKUP / ITEM_PICKUP)
 * was reported as too shrill/tinny ("agudisimo"), so it has been dropped
 * entirely. The new palette is built ONLY on cloth/leather textures that are
 * naturally low-key and rounded instead of bright or bell-like:
 *
 * <ul>
 *   <li>{@link net.minecraft.sounds.SoundEvents#ARMOR_EQUIP_GENERIC} - a soft
 *       cloth/leather shuffle, used for plain clicks and quick repeat ticks.</li>
 *   <li>{@link net.minecraft.sounds.SoundEvents#BUNDLE_INSERT} - a soft, muted
 *       "stash" pop, used for selecting/opening and the entrance cue.</li>
 *   <li>{@link net.minecraft.sounds.SoundEvents#BUNDLE_REMOVE_ONE} - the same
 *       family with a slightly different texture, used to confirm actions
 *       (buy, sell, save, collect).</li>
 *   <li>{@link net.minecraft.sounds.SoundEvents#UI_TOAST_OUT} - a quiet swoosh,
 *       kept for page turns.</li>
 * </ul>
 *
 * No book/bookshelf sounds, no note-block/piano, no amethyst, no orb chime.
 * Pitches stay narrow (0.8-1.25) and volumes stay quiet (0.14-0.45) so
 * everything reads as chill/neutral instead of harsh, tinny or "agudo".
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

   /** A single soft "stash" note at the given pitch (used for the entrance cue). */
   public static void spark(float pitch) {
      play(SimpleSoundInstance.forUI(SoundEvents.BUNDLE_INSERT, pitch, 0.22F));
   }

   /** Soft, neutral cloth click for buttons, coins and navigation. */
   public static void click() {
      play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_GENERIC, 1.1F, 0.25F));
   }

   /** Quick, quiet tick for rapid +/- stepping (hold to repeat). */
   public static void step() {
      play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_GENERIC, 1.25F, 0.15F));
   }

   /** Quiet, soft swoosh when changing pages. */
   public static void page() {
      play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 0.85F, 0.25F));
   }

   /** Warm, muted "stash" pop for a successful purchase, sale, save or collect. */
   public static void success() {
      play(SimpleSoundInstance.forUI(SoundEvents.BUNDLE_REMOVE_ONE, 0.85F, 0.45F));
   }

   /** Soft pop when opening or picking inside a menu. */
   public static void select() {
      play(SimpleSoundInstance.forUI(SoundEvents.BUNDLE_INSERT, 1.0F, 0.28F));
   }
}
