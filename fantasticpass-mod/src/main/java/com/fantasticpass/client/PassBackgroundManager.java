package com.fantasticpass.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Downloads and cycles the pass's background wallpapers (a list of http(s) image
 * links) in real time, like a Windows slideshow, cross-fading between them and
 * looping. Images are decoded with {@link ImageIO} (PNG / JPG / GIF / BMP) at
 * full resolution and uploaded as {@link DynamicTexture}s for crisp, high-quality
 * rendering. Self-contained: depends on no other mod.
 */
public final class PassBackgroundManager {
   private static final long INTERVAL_MS = 12000L; // time each wallpaper is shown
   private static final long FADE_MS = 700L;       // cross-fade duration
   private static final int MAX_SIDE = 3840;        // cap longest side (memory safety, still 4K)

   private record Loaded(ResourceLocation id, int width, int height, DynamicTexture texture) {
   }

   private static final List<String> URLS = new ArrayList<>();
   private static final Map<Integer, Loaded> LOADED = new ConcurrentHashMap<>();
   private static volatile boolean loadingStarted;
   private static int texSeq;

   private static int index;
   private static int prevIndex = -1;
   private static long lastSwitch;
   private static long fadeStart;

   private PassBackgroundManager() {
   }

   public static boolean hasBackgrounds() {
      synchronized (URLS) {
         return !URLS.isEmpty();
      }
   }

   /** Set the wallpaper list. Idempotent for an unchanged list. */
   public static void setBackgrounds(List<String> urls) {
      List<String> valid = new ArrayList<>();
      if (urls != null) {
         for (String u : urls) {
            if (isValidUrl(u)) {
               valid.add(u.trim());
            }
         }
      }
      synchronized (URLS) {
         if (URLS.equals(valid)) {
            return; // unchanged: keep the already-loaded textures
         }
         releaseInternal();
         URLS.clear();
         URLS.addAll(valid);
      }
      index = 0;
      prevIndex = -1;
      lastSwitch = System.currentTimeMillis();
      fadeStart = 0L;
      loadingStarted = false;
   }

   /**
    * Render the current wallpaper (with cross-fade) covering the whole screen.
    *
    * @return {@code true} if a dynamic wallpaper was drawn; {@code false} when
    *         nothing is ready yet (caller should draw its static fallback).
    */
   public static boolean render(GuiGraphics g, int width, int height) {
      if (!hasBackgrounds()) {
         return false;
      }
      ensureLoading();

      long now = System.currentTimeMillis();
      if (!LOADED.containsKey(index)) {
         Integer any = firstLoaded();
         if (any == null) {
            return false; // still downloading the first image
         }
         index = any;
         prevIndex = -1;
         lastSwitch = now;
         fadeStart = now - FADE_MS;
      }

      if (now - lastSwitch >= INTERVAL_MS) {
         Integer next = nextLoaded(index);
         if (next != null && next != index) {
            prevIndex = index;
            index = next;
            fadeStart = now;
         }
         lastSwitch = now;
      }

      Loaded cur = LOADED.get(index);
      if (cur == null) {
         return false;
      }
      float alpha = Mth.clamp((now - fadeStart) / (float)FADE_MS, 0.0F, 1.0F);
      Loaded prev = prevIndex >= 0 ? LOADED.get(prevIndex) : null;
      if (prev != null && alpha < 1.0F) {
         blitCover(g, prev, width, height, 1.0F);
      }
      blitCover(g, cur, width, height, prev != null ? alpha : 1.0F);
      return true;
   }

   private static void blitCover(GuiGraphics g, Loaded t, int width, int height, float alpha) {
      float cover = Math.max((float)width / t.width(), (float)height / t.height());
      int dw = Math.round(t.width() * cover);
      int dh = Math.round(t.height() * cover);
      int x = (width - dw) / 2;
      int y = (height - dh) / 2;
      g.setColor(1.0F, 1.0F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F));
      g.blit(t.id(), x, y, dw, dh, 0.0F, 0.0F, t.width(), t.height(), t.width(), t.height());
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static Integer firstLoaded() {
      int size = urlCount();
      for (int i = 0; i < size; i++) {
         if (LOADED.containsKey(i)) {
            return i;
         }
      }
      return null;
   }

   private static Integer nextLoaded(int from) {
      int size = urlCount();
      for (int step = 1; step <= size; step++) {
         int i = Math.floorMod(from + step, size);
         if (LOADED.containsKey(i)) {
            return i;
         }
      }
      return null;
   }

   private static int urlCount() {
      synchronized (URLS) {
         return URLS.size();
      }
   }

   private static void ensureLoading() {
      if (loadingStarted) {
         return;
      }
      loadingStarted = true;
      List<String> snapshot;
      synchronized (URLS) {
         snapshot = new ArrayList<>(URLS);
      }
      Thread t = new Thread(() -> {
         for (int i = 0; i < snapshot.size(); i++) {
            try {
               downloadInto(i, snapshot.get(i));
            } catch (Throwable ignored) {
               // Bad link / unsupported format: skip; it just won't be in the rotation.
            }
         }
      }, "FantasticPass-Wallpaper");
      t.setDaemon(true);
      t.start();
   }

   private static void downloadInto(int slot, String url) throws IOException {
      byte[] bytes = readAll(openStream(url));
      BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
      if (img == null) {
         return; // not a decodable image (e.g. Google Drive HTML page)
      }
      img = clampSize(img);
      final NativeImage nativeImage = toNativeImage(img);
      final int w = img.getWidth();
      final int h = img.getHeight();
      Minecraft mc = Minecraft.getInstance();
      mc.execute(() -> {
         try {
            // Guard against a playlist swap that happened while downloading.
            synchronized (URLS) {
               if (slot >= URLS.size()) {
                  nativeImage.close();
                  return;
               }
            }
            DynamicTexture tex = new DynamicTexture(nativeImage);
            ResourceLocation id = new ResourceLocation("fantasticpass", "dynbg/" + (texSeq++));
            mc.getTextureManager().register(id, tex);
            LOADED.put(slot, new Loaded(id, w, h, tex));
         } catch (Throwable t) {
            nativeImage.close();
         }
      });
   }

   private static BufferedImage clampSize(BufferedImage src) {
      int w = src.getWidth();
      int h = src.getHeight();
      int longest = Math.max(w, h);
      if (longest <= MAX_SIDE) {
         return src;
      }
      float s = (float)MAX_SIDE / longest;
      int nw = Math.max(1, Math.round(w * s));
      int nh = Math.max(1, Math.round(h * s));
      BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = dst.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.drawImage(src, 0, 0, nw, nh, null);
      g.dispose();
      return dst;
   }

   private static NativeImage toNativeImage(BufferedImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      NativeImage out = new NativeImage(NativeImage.Format.RGBA, w, h, false);
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int argb = img.getRGB(x, y);
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int gg = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            // NativeImage packs bytes as R,G,B,A (little-endian int = 0xAABBGGRR).
            int abgr = (a << 24) | (b << 16) | (gg << 8) | r;
            out.setPixelRGBA(x, y, abgr);
         }
      }
      return out;
   }

   /** Release all GPU textures (call on logout). */
   public static void release() {
      Minecraft.getInstance().execute(PassBackgroundManager::releaseInternal);
   }

   private static void releaseInternal() {
      Minecraft mc = Minecraft.getInstance();
      for (Loaded l : LOADED.values()) {
         try {
            mc.getTextureManager().release(l.id());
            l.texture().close();
         } catch (Throwable ignored) {
         }
      }
      LOADED.clear();
      prevIndex = -1;
      index = 0;
   }

   // ---- networking helpers -------------------------------------------------

   private static InputStream openStream(String url) throws IOException {
      String current = url.trim();
      for (int i = 0; i < 6; i++) {
         URL u = new URL(current);
         HttpURLConnection conn = (HttpURLConnection)u.openConnection();
         conn.setInstanceFollowRedirects(false);
         conn.setRequestProperty("User-Agent", "Mozilla/5.0 (FantasticPass)");
         conn.setConnectTimeout(10000);
         conn.setReadTimeout(20000);
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
         return conn.getInputStream();
      }
      throw new IOException("too many redirects");
   }

   private static byte[] readAll(InputStream in) throws IOException {
      try (in) {
         ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
         byte[] buf = new byte[8192];
         int n;
         while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
         }
         return out.toByteArray();
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
