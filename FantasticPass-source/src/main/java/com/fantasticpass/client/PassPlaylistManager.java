/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.client;

import com.fantasticpass.gui.castle.CastleScreen;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
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

public final class PassPlaylistManager {
    private static final float[] LEVELS = new float[]{1.0f, 0.66f, 0.33f};
    private static final Path CONFIG = FMLPaths.CONFIGDIR.get().resolve("fantasticpass-music.properties");
    private static final List<String> PLAYLIST = new ArrayList<String>();
    private static String title = "Fantastic Pass";
    private static Thread thread;
    private static volatile boolean stopFlag;
    private static volatile boolean playing;
    private static volatile SourceDataLine activeLine;
    private static volatile int volumeGen;
    private static int lastAppliedGen;
    private static int volumeState;
    private static int lastIndex = -1;

    private PassPlaylistManager() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void setPlaylist(List<String> urls, String playlistTitle) {
        List<String> list = PLAYLIST;
        synchronized (list) {
            PLAYLIST.clear();
            if (urls != null) {
                for (String u : urls) {
                    if (!PassPlaylistManager.isValidUrl(u)) continue;
                    PLAYLIST.add(u.trim());
                }
            }
        }
        if (playlistTitle != null && !playlistTitle.isBlank()) {
            title = playlistTitle;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean hasPlaylist() {
        List<String> list = PLAYLIST;
        synchronized (list) {
            return !PLAYLIST.isEmpty();
        }
    }

    public static synchronized void ensurePlaying() {
        if (playing || !PassPlaylistManager.hasPlaylist()) {
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
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        activeLine = null;
        Thread t = thread;
        if (t != null) {
            t.interrupt();
            thread = null;
        }
    }

    public static void clientTick() {
        if (playing && !(Minecraft.getInstance().screen instanceof CastleScreen)) {
            PassPlaylistManager.stop();
        }
    }

    public static void cycleVolume() {
        volumeState = (volumeState + 1) % (LEVELS.length + 1);
        ++volumeGen;
        PassPlaylistManager.saveVolumeState();
    }

    public static boolean isMuted() {
        return volumeState >= LEVELS.length;
    }

    public static int volumePercent() {
        return PassPlaylistManager.isMuted() ? 0 : Math.round(PassPlaylistManager.currentVolume() * 100.0f);
    }

    public static int volumeBars() {
        return PassPlaylistManager.isMuted() ? 0 : LEVELS.length - volumeState;
    }

    private static float currentVolume() {
        return PassPlaylistManager.isMuted() ? 0.0f : LEVELS[volumeState];
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void runLoop() {
        int initialSize;
        List<String> pl = PLAYLIST;
        synchronized (pl) {
            initialSize = PLAYLIST.size();
        }
        // Arranque aleatorio: la primera cancion nunca es siempre la misma.
        // Con mas de 1 cancion evitamos repetir la ultima que sono (nunca 2 veces seguidas,
        // ni siquiera al reabrir la GUI). Con 1 sola cancion, se repite.
        int index = 0;
        if (initialSize > 1) {
            index = java.util.concurrent.ThreadLocalRandom.current().nextInt(initialSize);
            if (index == lastIndex) {
                index = Math.floorMod(index + 1, initialSize);
            }
        }
        int fails = 0;
        while (!stopFlag) {
            String url;
            int size;
            List<String> list = PLAYLIST;
            synchronized (list) {
                size = PLAYLIST.size();
                if (size == 0) {
                    break;
                }
                index = Math.floorMod(index, size);
                url = PLAYLIST.get(index);
            }
            lastIndex = index;
            long started = System.currentTimeMillis();
            try {
                PassPlaylistManager.streamTrack(url);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (stopFlag) break;
            if (System.currentTimeMillis() - started < 2000L) {
                ++fails;
                PassPlaylistManager.sleepQuietly(400L);
            } else {
                fails = 0;
            }
            if (fails >= Math.max(1, size)) break;
            index = Math.floorMod(index + 1, size);
        }
        playing = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void streamTrack(String url) throws Exception {
        SourceDataLine line = null;
        try (InputStream in = PassPlaylistManager.openAudioStream(url);){
            Header header;
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
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
                    PassPlaylistManager.applyGain((SourceDataLine)line);
                }
                if (lastAppliedGen != volumeGen) {
                    PassPlaylistManager.applyGain((SourceDataLine)line);
                    lastAppliedGen = volumeGen;
                }
                byte[] bytes = PassPlaylistManager.toLittleEndian(output.getBuffer(), output.getBufferLength());
                line.write(bytes, 0, bytes.length);
                bitstream.closeFrame();
            }
            if (line != null && !stopFlag) {
                line.drain();
            }
            bitstream.close();
        }
        finally {
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                }
                catch (Exception exception) {}
            }
            if (activeLine == line) {
                activeLine = null;
            }
        }
    }

    private static byte[] toLittleEndian(short[] samples, int len) {
        byte[] out = new byte[len * 2];
        for (int i = 0; i < len; ++i) {
            short s = samples[i];
            out[i * 2] = (byte)(s & 0xFF);
            out[i * 2 + 1] = (byte)(s >> 8 & 0xFF);
        }
        return out;
    }

    private static void applyGain(SourceDataLine line) {
        try {
            if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
                float v = PassPlaylistManager.currentVolume();
                float db = v <= 1.0E-4f ? gain.getMinimum() : (float)(20.0 * Math.log10(v));
                gain.setValue(Mth.clamp((float)db, (float)gain.getMinimum(), (float)gain.getMaximum()));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static InputStream openAudioStream(String url) throws IOException {
        String current = url.trim();
        for (int i = 0; i < 6; ++i) {
            String location;
            URL u = new URL(current);
            HttpURLConnection conn = (HttpURLConnection)u.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (FantasticPass)");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            if (code / 100 == 3) {
                location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) {
                    throw new IOException("redirect without Location");
                }
            } else {
                return new BufferedInputStream(conn.getInputStream(), 16384);
            }
            current = new URL(u, location).toString();
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
        }
        catch (Exception e) {
            return false;
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int loadVolumeState() {
        block8: {
            try {
                if (!Files.exists(CONFIG, new LinkOption[0])) break block8;
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(CONFIG, new OpenOption[0]);){
                    p.load(in);
                }
                int s = Integer.parseInt(p.getProperty("volumeState", "0"));
                return Mth.clamp((int)s, (int)0, (int)LEVELS.length);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return 0;
    }

    private static void saveVolumeState() {
        try {
            Properties p = new Properties();
            p.setProperty("volumeState", Integer.toString(volumeState));
            Files.createDirectories(CONFIG.getParent(), new FileAttribute[0]);
            try (OutputStream out = Files.newOutputStream(CONFIG, new OpenOption[0]);){
                p.store(out, "Fantastic Pass music settings");
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    static {
        lastAppliedGen = -1;
        volumeState = PassPlaylistManager.loadVolumeState();
    }
}

