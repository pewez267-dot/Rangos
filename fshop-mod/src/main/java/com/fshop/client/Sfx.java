package com.fshop.client;

/**
 * Shop UI sounds are intentionally disabled: the user asked for the shop to be
 * completely silent. Every method is a no-op so all the existing call sites
 * keep working without playing anything. To re-enable sounds later, just fill
 * these methods in again.
 */
public final class Sfx {
   private Sfx() {
   }

   public static void spark(float pitch) {
   }

   public static void click() {
   }

   public static void step() {
   }

   public static void page() {
   }

   public static void success() {
   }

   public static void select() {
   }
}
