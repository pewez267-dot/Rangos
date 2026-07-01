package com.fantasticpass.client;

import com.fantasticpass.gui.castle.CastleScreen;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;

/**
 * Streams the pass's user-defined music playlist (a list of http(s) links) in
 * real time while the player is inside the Battle Pass UI, advancing to the next
 * link when a track finishes and looping back to the first at the end.
 *
 * <p>Playback is delegated to the {@link EtchedBridge} (the Etched mod). If Etched
 * is not installed, or the playlist is empty, this manager is silently inert.
 * This fully replaces the old baked-in {@code pass_music.ogg} loop.
 */
public final class PassPlaylistManager {
   private static final List<String> PLAYLIST = new ArrayList<>();
   private static String title = "Fantastic Pass";
   private static int index;
   private static SoundInstance current;
   private static boolean active;
   private static long trackStart;
   private static int failStreak;

   private PassPlaylistManager() {
   }

   /** Replace the active playlist (keeps only valid http/https links). */
   public static void setPlaylist(List<String> urls, String playlistTitle) {
      PLAYLIST.clear();
      if (urls != null) {
         for (String u : urls) {
            if (isValidUrl(u)) {
               PLAYLIST.add(u.trim());
            }
         }
      }
      if (playlistTitle != null && !playlistTitle.isBlank()) {
         title = playlistTitle;
      }
   }

   public static boolean hasPlaylist() {
      return !PLAYLIST.isEmpty();
   }

   /** Begin playback from the first track if not already playing. Called when a castle screen opens. */
   public static void ensurePlaying() {
      if (active || PLAYLIST.isEmpty() || !EtchedBridge.isAvailable()) {
         return;
      }
      active = true;
      index = 0;
      failStreak = 0;
      playCurrent();
   }

   public static void stop() {
      active = false;
      if (current != null) {
         // Detach the stop-listener first so stopping doesn't trigger an advance.
         EtchedBridge.stopListening(current);
         Minecraft.getInstance().getSoundManager().stop(current);
         current = null;
      }
   }

   private static void playCurrent() {
      if (!active || PLAYLIST.isEmpty()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         active = false;
         return;
      }
      String url = PLAYLIST.get(Math.floorMod(index, PLAYLIST.size()));
      trackStart = System.currentTimeMillis();
      SoundInstance track = EtchedBridge.createTrack(url, Component.literal("\u266b " + title), mc.player, PassPlaylistManager::onTrackEnd);
      if (track == null) {
         onTrackEnd();
         return;
      }
      current = track;
      mc.getSoundManager().play(track);
   }

   /** Called by the Etched stop-listener when a track ends or fails. */
   private static void onTrackEnd() {
      Minecraft.getInstance().execute(() -> {
         if (!active) {
            return;
         }
         // If the track ended almost immediately it most likely failed to load.
         if (System.currentTimeMillis() - trackStart < 3000L) {
            failStreak++;
         } else {
            failStreak = 0;
         }
         // Every link failing back-to-back: give up instead of looping forever.
         if (failStreak >= Math.max(1, PLAYLIST.size())) {
            active = false;
            current = null;
            return;
         }
         index = Math.floorMod(index + 1, PLAYLIST.size());
         current = null;
         playCurrent();
      });
   }

   /** Client tick: stop the music once the player leaves the Battle Pass UI. */
   public static void clientTick() {
      if (active && !(Minecraft.getInstance().screen instanceof CastleScreen)) {
         stop();
      }
   }

   private static boolean isValidUrl(String url) {
      if (url == null || url.isBlank()) {
         return false;
      }
      try {
         String scheme = new URI(url.trim()).getScheme();
         return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
      } catch (Exception e) {
         return false;
      }
   }
}
