package com.fantasticpass.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

/**
 * Soft, reflection-only bridge to the <b>Etched</b> mod's online-audio engine.
 *
 * <p>The Fantastic Pass never depends on Etched at compile time: if the mod is
 * present we drive its proven streaming/decoding pipeline to play http(s) audio
 * links; if it is absent every call is a graceful no-op so nothing breaks.
 *
 * <p>Entry points used (all stable, non-obfuscated Etched API names):
 * <ul>
 *   <li>{@code SoundTracker.getEtchedRecord(String url, Component title, Entity entity,
 *       int attenuationDistance, boolean stream)} &rarr; a {@code SoundInstance}.</li>
 *   <li>{@code StopListeningSound.create(SoundInstance, SoundStopListener)} which fires
 *       {@code onStop()} when a track finishes &mdash; exactly how Etched's own album
 *       jukebox advances to the next track. We use it to advance our playlist.</li>
 * </ul>
 */
public final class EtchedBridge {
   private static Boolean available;
   private static Method getEtchedRecord;   // (String, Component, Entity, int, boolean) -> AbstractOnlineSoundInstance
   private static Method stopCreate;          // (SoundInstance, SoundStopListener) -> StopListeningSound
   private static Method stopListening;       // StopListeningSound#stopListening()
   private static Class<?> stopListenerClass; // SoundStopListener (functional interface)
   private static Class<?> stopListeningClass;

   private EtchedBridge() {
   }

   public static boolean isAvailable() {
      if (available == null) {
         available = ModList.get() != null && ModList.get().isLoaded("etched") && tryInit();
      }
      return available;
   }

   private static boolean tryInit() {
      try {
         Class<?> tracker = Class.forName("gg.moonflower.etched.api.sound.SoundTracker");
         getEtchedRecord = tracker.getMethod("getEtchedRecord", String.class, Component.class, Entity.class, int.class, boolean.class);
         stopListeningClass = Class.forName("gg.moonflower.etched.api.sound.StopListeningSound");
         stopListenerClass = Class.forName("gg.moonflower.etched.api.sound.SoundStopListener");
         stopCreate = stopListeningClass.getMethod("create", SoundInstance.class, stopListenerClass);
         stopListening = stopListeningClass.getMethod("stopListening");
         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   /**
    * Build a streaming sound instance for {@code url} that invokes {@code onEnd}
    * when the track finishes (or fails), so a playlist can advance.
    *
    * @return the ready-to-play instance, or {@code null} if Etched is unavailable
    *         or the instance could not be created.
    */
   public static SoundInstance createTrack(String url, Component title, Entity entity, Runnable onEnd) {
      if (!isAvailable() || entity == null) {
         return null;
      }
      try {
         // stream=false (FILE): finite tracks end cleanly so the playlist can advance;
         // a generous attenuation distance keeps it audible at the listener.
         Object online = getEtchedRecord.invoke(null, url, title, entity, 64, false);
         Object listener = Proxy.newProxyInstance(
            stopListenerClass.getClassLoader(), new Class[]{stopListenerClass}, new StopHandler(onEnd));
         Object wrapped = stopCreate.invoke(null, online, listener);
         return (SoundInstance)wrapped;
      } catch (Throwable t) {
         return null;
      }
   }

   /** Detach a track's stop-listener so stopping it won't advance the playlist. */
   public static void stopListening(SoundInstance sound) {
      try {
         if (stopListeningClass != null && stopListeningClass.isInstance(sound)) {
            stopListening.invoke(sound);
         }
      } catch (Throwable ignored) {
      }
   }

   /** Handles the single-method {@code SoundStopListener#onStop()} via a dynamic proxy. */
   private record StopHandler(Runnable onEnd) implements InvocationHandler {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) {
         switch (method.getName()) {
            case "onStop" -> {
               this.onEnd.run();
               return null;
            }
            case "hashCode" -> {
               return System.identityHashCode(proxy);
            }
            case "equals" -> {
               return proxy == (args == null ? null : args[0]);
            }
            case "toString" -> {
               return "FantasticPassStopListener";
            }
            default -> {
               return null;
            }
         }
      }
   }
}
