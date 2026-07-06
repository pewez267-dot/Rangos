package com.fshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * One chill shop sound: the amethyst block CHIME (the soft, resonant crystal
 * "ting" - NOT the block-breaking sound), reused for every action at a low
 * volume and gentle pitch. The +/- auto-repeat stays silent so holding a
 * button never turns into noise.
 */
public final class Sfx {
   private Sfx() {
   }

   private static void chime(float pitch, float volume) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getSoundManager() != null) {
         SoundInstance s = SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch, volume);
         mc.getSoundManager().play(s);
      }
   }

   /** Soft crystal ting when the shop opens. */
   public static void spark(float pitch) {
      chime(1.0F, 0.35F);
   }

   /** Soft crystal ting for buttons, coins and navigation. */
   public static void click() {
      chime(1.0F, 0.3F);
   }

   /** Quiet ting for a single +/- press (auto-repeat is silent). */
   public static void step() {
      chime(1.15F, 0.2F);
   }

   /** Soft ting when changing pages. */
   public static void page() {
      chime(0.9F, 0.25F);
   }

   /** Slightly brighter, warm ting confirming a purchase, sale, save or collect. */
   public static void success() {
      chime(1.2F, 0.38F);
   }

   /** Soft ting when opening or picking inside a menu. */
   public static void select() {
      chime(1.1F, 0.3F);
   }
}
