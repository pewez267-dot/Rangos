package com.fantasticpass.client;

import com.fantasticpass.gui.castle.CastleScreen;
import net.minecraft.client.Minecraft;

/**
 * Keeps the Battle Pass music looping seamlessly while the player navigates
 * between the castle screens (hub, rewards, info...) and stops it automatically
 * once they leave the Battle Pass UI entirely. Driven by the client tick.
 */
public final class PassMusicManager {
   private static PassMusicInstance instance;

   private PassMusicManager() {
   }

   public static void ensurePlaying() {
      Minecraft mc = Minecraft.getInstance();
      if (instance == null || mc.getSoundManager().isActive(instance) == false) {
         instance = new PassMusicInstance();
         mc.getSoundManager().play(instance);
      }
   }

   public static void stop() {
      if (instance != null) {
         Minecraft.getInstance().getSoundManager().stop(instance);
         instance = null;
      }
   }

   /** Called every client tick: stop the music when no castle screen is open. */
   public static void clientTick() {
      if (instance != null && !(Minecraft.getInstance().screen instanceof CastleScreen)) {
         stop();
      }
   }
}
