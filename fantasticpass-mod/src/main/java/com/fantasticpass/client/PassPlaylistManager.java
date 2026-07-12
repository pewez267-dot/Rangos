package com.fantasticpass.client;

import com.fantasticpass.gui.castle.CastleScreen;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Self-contained streaming music for the Battle Pass UI.
 *
 * <p>Plays the pass's user-defined playlist (a list of http(s) MP3 links) in real
 * time on a background thread using a bundled MP3 decoder (JLayer) piped to a
 * {@link SourceDataLine}. It advances to the next link when a track ends, loops
 * at the end, and stops when the player leaves the pass UI. Volume / mute is
 * controlled by the in-GUI speaker button and persisted between sessions.
 *
 * <p>This depends on <b>no other mod</b> — the decoder is bundled into the jar.
 */
public final class PassPlaylistManager {
   private static final float[] LEVELS = {1.0F, 0.66F, 0.33F}; // + a 4th "muted" state
   private static final Path CONFIG = FMLPaths.CONFIGDIR.get().resolve("fantasticpass-music.properties");

   private static final List<String> PLAYLIST = new ArrayList<>();
   private static String title = "Fantastic Pass";

   private static Thread thread;
   private static volatile boolean stopFlag;
   private static volatile boolean playing;
   private static volatile SourceDataLine activeLine;
   private static volatile int volumeGen;
   private static int lastAppliedGen = -1;

   /** 0..2 = LEVELS index, 3 = muted. Persisted. */
   private static int volumeState = loadVolumeState();

   private PassPlaylistManager() {
   }

   // ---- Playlist control ---------------------------------------------------

   public static void setPlaylist(List<String> urls, String playlistTitle) {
      synchronized (PLAYLIST) {
         PLAYLIST.clear();
         if (urls != null) {
            for (String u : urls) {
               if (isValidUrl(u)) {
                  PLAYLIST.add(u.trim());
               }
            }
         }
      }
      if (playlistTitle != null && !playlistTitle.isBlank()) {
         title = playlistTitle;
      }
   }

   public static boolean hasPlaylist() {
      synchronized (PLAYLIST) {
         return !PLAYLIST.isEmpty();
      }
   }

   /** Start streaming from the first track if not already playing. */
   public static synchronized void ensurePlaying() {
      if (playing || !hasPlaylist()) {
         return;
      }
      stopFlag = false;
      playing = true;
      thread = new Thread(PassPlaylistManager::runLoop, "FantasticPass-Music");
      thread.setDaemon(true);
      thread.start();
   }

   public static synchronized void stop() {
      stopFlag = true;
      playing = false;
      SourceDataLine line = activeLine;
      if (line != null) {
         try {
            line.stop();
            line.flush();
            line.close();
         } catch (Exception ignored) {
         }
      }
      activeLine = null;
      Thread t = thread;
      if (t != null) {
         t.interrupt();
         thread = null;
      }
   }

   /** Stop the music once the player leaves the Battle Pass UI. */
   public static void clientTick() {
      if (playing && !(Minecraft.getInstance().screen instanceof CastleScreen)) {
         stop();
      }
   }

   // ---- Volume / mute (GUI button) ----------------------------------------

   /** Cycle 100% -> 66% -> 33% -> muted -> 100% ... */
   public static void cycleVolume() {
      volumeState = (volumeState + 1) % (LEVELS.length + 1);
      volumeGen++;
      saveVolumeState();
   }

   public static boolean isMuted() {
      return volumeState >= LEVELS.length;
   }

   public static int volumePercent() {
      return isMuted() ? 0 : Math.round(currentVolume() * 100.0F);
   }

   /** Number of filled meter cells (0..3) for the GUI button. */
   public static int volumeBars() {
      return isMuted() ? 0 : (LEVELS.length - volumeState);
   }

   private static float currentVolume() {
      return isMuted() ? 0.0F : LEVELS[volumeState];
   }

   // ---- Streaming worker ---------------------------------------------------

   private static void runLoop() {
      int index = 0;
      int fails = 0;
      while (!stopFlag) {
         String url;
         int size;
         synchronized (PLAYLIST) {
            size = PLAYLIST.size();
            if (size == 0) {
               break;
            }
            url = PLAYLIST.get(Math.floorMod(index, size));
         }

         long started = System.currentTimeMillis();
         try {
            streamTrack(url);
         } catch (Throwable t) {
            // Bad link / unsupported format / no audio device: skip to the next.
         }

         if (stopFlag) {
            break;
         }
         // A track that "ended" almost instantly almost certainly failed.
         if (System.currentTimeMillis() - started < 2000L) {
            fails++;
            sleepQuietly(400L);
         } else {
            fails = 0;
         }
         if (fails >= Math.max(1, size)) {
            break; // every link failed; give up rather than spin forever
         }
         index = Math.floorMod(index + 1, size);
      }
      playing = false;
   }

   private static void streamTrack(String url) throws Exception {
      SourceDataLine line = null;
      try (InputStream in = openAudioStream(url)) {
         Bitstream bitstream = new Bitstream(in);
         Decoder decoder = new Decoder();
         Header header;
         while (!stopFlag && (header = bitstream.readFrame()) != null) {
            SampleBuffer output = (SampleBuffer)decoder.decodeFrame(header, bitstream);
            if (line == null) {
               AudioFormat fmt = new AudioFormat(output.getSampleFrequency(), 16, output.getChannelCount(), true, false);
               DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
               line = (SourceDataLine)AudioSystem.getLine(info);
               line.open(fmt);
               line.start();
               activeLine = line;
               lastAppliedGen = -1;
               applyGain(line);
            }
            if (lastAppliedGen != volumeGen) {
               applyGain(line);
               lastAppliedGen = volumeGen;
            }
            byte[] bytes = toLittleEndian(output.getBuffer(), output.getBufferLength());
            line.write(bytes, 0, bytes.length);
            bitstream.closeFrame();
         }
         if (line != null && !stopFlag) {
            line.drain();
         }
         bitstream.close();
      } finally {
         if (line != null) {
            try {
               line.stop();
               line.close();
            } catch (Exception ignored) {
            }
         }
         if (activeLine == line) {
            activeLine = null;
         }
      }
   }

   private static byte[] toLittleEndian(short[] samples, int len) {
      byte[] out = new byte[len * 2];
      for (int i = 0; i < len; i++) {
         short s = samples[i];
         out[i * 2] = (byte)(s & 0xFF);
         out[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
      }
      return out;
   }

   private static void applyGain(SourceDataLine line) {
      try {
         if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
            float v = currentVolume();
            float db = v <= 0.0001F ? gain.getMinimum() : (float)(20.0 * Math.log10(v));
            gain.setValue(Mth.clamp(db, gain.getMinimum(), gain.getMaximum()));
         }
      } catch (Exception ignored) {
      }
   }

   /** Open a streaming input for the URL, following http/https redirects. */
   private static InputStream openAudioStream(String url) throws IOException {
      String current = url.trim();
      for (int i = 0; i < 6; i++) {
         URL u = new URL(current);
         HttpURLConnection conn = (HttpURLConnection)u.openConnection();
         conn.setInstanceFollowRedirects(false);
         conn.setRequestProperty("User-Agent", "Mozilla/5.0 (FantasticPass)");
         conn.setConnectTimeout(10000);
         conn.setReadTimeout(15000);
         int code = conn.getResponseCode();
         if (code / 100 == 3) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (location == null) {
               throw new IOException("redirect without Location");
            }
            current = new URL(u, location).toString();
            continue;
         }
         return new BufferedInputStream(conn.getInputStream(), 16384);
      }
      throw new IOException("too many redirects");
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

   private static void sleepQuietly(long ms) {
      try {
         Thread.sleep(ms);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   // ---- Persistence --------------------------------------------------------

   private static int loadVolumeState() {
      try {
         if (Files.exists(CONFIG)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG)) {
               p.load(in);
            }
            int s = Integer.parseInt(p.getProperty("volumeState", "0"));
            return Mth.clamp(s, 0, LEVELS.length);
         }
      } catch (Exception ignored) {
      }
      return 0;
   }

   private static void saveVolumeState() {
      try {
         Properties p = new Properties();
         p.setProperty("volumeState", Integer.toString(volumeState));
         Files.createDirectories(CONFIG.getParent());
         try (var out = Files.newOutputStream(CONFIG)) {
            p.store(out, "Fantastic Pass music settings");
         }
      } catch (Exception ignored) {
      }
   }
}
