package com.fantasticpass.gui.castle;

import com.fantasticpass.client.PassBackgroundManager;
import com.fantasticpass.client.PassPlaylistManager;
import com.fantasticpass.gui.widgets.MusicButton;
import com.fantasticpass.gui.widgets.PeekButton;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Shared base for all "castle" Battle Pass screens.
 *
 * <p>The castle textures are chest-GUI backgrounds. Their slot grid is the
 * vanilla 9-column container layout, offset vertically by the font glyph
 * {@code ascent} declared in the resource pack. Reproducing that math here lets
 * the items land exactly on the slot graphics baked into the artwork:
 * <pre>
 *   texture-x(col) = 48 + col * 18
 *   texture-y(row) = 5 + ascent + row * 18      (slots are 16px inside an 18px cell)
 * </pre>
 *
 * The whole 256x256 texture is blitted 1:1 at an integer scale and centred, and
 * {@link #sx}/{@link #sy} map texture pixels to the screen.
 */
public abstract class CastleScreen extends Screen {
   public static final int SLOT = 16;
   protected static final int CELL = 18;
   protected static final int COLS = 9;

   /** Full-art parallax background drawn behind every castle pass screen. */
   private static final ResourceLocation PASS_BG = new ResourceLocation("fantasticpass", "textures/gui/castle/pass_bg.png");
   private static final int BG_W = 1536;
   private static final int BG_H = 1024;

   protected static ResourceLocation castle(String name) {
      return new ResourceLocation("fantasticpass", "textures/gui/castle/" + name + ".png");
   }

   protected static ResourceLocation icon(int index) {
      String n = index == 0 ? "bp_icons_empty" : String.format("bp_icons_%02d", index);
      return new ResourceLocation("fantasticpass", "textures/gui/castle/icons/" + n + ".png");
   }

   @Nullable
   protected final Screen parent;
   private final ResourceLocation background;
   private final int ascent;
   private final int cx0;
   private final int cy0;
   private final int cx1;
   private final int cy1;

   protected int scale = 3;
   protected int left;
   protected int top;
   private long openTime;
   protected float anim;

   /** When true the pass artwork/foreground is hidden so the wallpaper shows fully. */
   private static boolean peekBackground;

   public static boolean isPeek() {
      return peekBackground;
   }

   public static void togglePeek() {
      peekBackground = !peekBackground;
   }

   protected CastleScreen(Component title, @Nullable Screen parent, ResourceLocation background, int ascent, int cx0, int cy0, int cx1, int cy1) {
      super(title);
      this.parent = parent;
      this.background = background;
      this.ascent = ascent;
      this.cx0 = cx0;
      this.cy0 = cy0;
      this.cx1 = cx1;
      this.cy1 = cy1;
   }

   @Override
   protected void init() {
      peekBackground = false; // always start showing the pass; the eye button reveals the wallpaper
      int contentW = this.cx1 - this.cx0;
      int contentH = this.cy1 - this.cy0;
      // Leave a generous margin so the parallax background stays visible around the book.
      int availW = (int)(this.width * 0.70F);
      int availH = (int)(this.height * 0.70F);
      int fit = Math.min(availW / Math.max(1, contentW), availH / Math.max(1, contentH));
      this.scale = Mth.clamp(fit, 2, 4);
      int drawnW = contentW * this.scale;
      int drawnH = contentH * this.scale;
      int screenContentLeft = (this.width - drawnW) / 2;
      int screenContentTop = (this.height - drawnH) / 2;
      this.left = screenContentLeft - this.cx0 * this.scale;
      this.top = screenContentTop - this.cy0 * this.scale;
      this.openTime = System.currentTimeMillis();
      this.anim = 0.0F;
      PassPlaylistManager.ensurePlaying();
      this.playChime(1.0F);

      // Small, unobtrusive controls in the top-right corner of the pass artwork
      // (present on every castle screen): music volume, and a "peek" eye that
      // hides the pass to reveal the wallpaper behind it.
      int btnSize = Mth.clamp(this.scale * 5, 14, 18);
      int bx = this.sx(this.cx1) - btnSize - 2;
      int by = this.sy(this.cy0) + 2;
      this.addRenderableWidget(new MusicButton(bx, by, btnSize));
      this.addRenderableWidget(new PeekButton(bx - btnSize - 3, by, btnSize));

      this.initControls();
   }

   protected abstract void initControls();

   /** Texture-space X to screen. */
   protected int sx(int tx) {
      return this.left + tx * this.scale;
   }

   /** Texture-space Y to screen. */
   protected int sy(int ty) {
      return this.top + ty * this.scale;
   }

   /** Screen X of the (16px) slot at the given column. */
   protected int slotX(int col) {
      return this.sx(48 + col * CELL);
   }

   /** Screen Y of the (16px) slot at the given row. */
   protected int slotY(int row) {
      return this.sy(5 + this.ascent + row * CELL);
   }

   protected int slotPx() {
      return SLOT * this.scale;
   }

   /** Number of pages needed to show {@code total} items {@code perPage} at a time. */
   protected static int pageCount(int perPage, int total) {
      return Math.max(1, (Math.max(0, total) + perPage - 1) / perPage);
   }

   /**
    * Starting index for a page. The final page snaps so it shows a full window
    * ending at {@code total}, which avoids trailing empty slots ("excedente").
    */
   protected static int pageBase(int page, int perPage, int total) {
      int base = page * perPage;
      if (base + perPage > total) {
         base = Math.max(0, total - perPage);
      }

      return Math.max(0, base);
   }

   protected boolean overSlot(double mx, double my, int col, int row) {
      int x = this.slotX(col);
      int y = this.slotY(row);
      int s = this.slotPx();
      return mx >= x && mx < x + s && my >= y && my < y + s;
   }

   protected float updateAnim() {
      float t = Mth.clamp((System.currentTimeMillis() - this.openTime) / 220.0F, 0.0F, 1.0F);
      this.anim = 1.0F - (1.0F - t) * (1.0F - t);
      return this.anim;
   }

   protected void drawCastleBackground(GuiGraphics g) {
      this.drawParallaxBackground(g);
      if (peekBackground) {
         return; // peek mode: only the wallpaper (and the corner buttons) are shown
      }
      float a = this.updateAnim();
      g.setColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, a));
      g.blit(this.background, this.left, this.top, 256 * this.scale, 256 * this.scale, 0.0F, 0.0F, 256, 256, 256, 256);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /**
    * Draw the background. If the pass defines wallpaper links they are cycled
    * (cover-fill, cross-faded) by {@link PassBackgroundManager}; otherwise the
    * baked full-art landscape is used. A gentle scrim keeps the book legible,
    * but it is dropped in peek mode so the wallpaper reads at full clarity.
    */
   private void drawParallaxBackground(GuiGraphics g) {
      if (!PassBackgroundManager.render(g, this.width, this.height)) {
         float cover = Math.max((float)this.width / BG_W, (float)this.height / BG_H);
         int drawW = Math.round(BG_W * cover);
         int drawH = Math.round(BG_H * cover);
         int x = (this.width - drawW) / 2;
         int y = (this.height - drawH) / 2;
         g.blit(PASS_BG, x, y, drawW, drawH, 0.0F, 0.0F, BG_W, BG_H, BG_W, BG_H);
      }
      if (!peekBackground) {
         // Gentle scrim for legibility (keeps the art vivid).
         g.fill(0, 0, this.width, this.height, 0x44000000);
      }
   }

   /** Draw a 16x16 icon texture scaled into the slot at (col,row). */
   protected void drawIcon(GuiGraphics g, ResourceLocation tex, int col, int row) {
      int s = this.slotPx();
      g.blit(tex, this.slotX(col), this.slotY(row), s, s, 0.0F, 0.0F, 16, 16, 16, 16);
   }

   protected void drawIconAt(GuiGraphics g, ResourceLocation tex, int x, int y) {
      int s = this.slotPx();
      g.blit(tex, x, y, s, s, 0.0F, 0.0F, 16, 16, 16, 16);
   }

   /**
    * Draw a slim, framed progress bar in texture space (x..x+w, height h at ty).
    * Designed to sit on the brick ledge under the banner so it never covers the
    * reward grid. {@code frac} in [0,1] fills the bar with a warm gold gradient.
    */
   protected void drawProgressBar(GuiGraphics g, int txStart, int txEnd, int ty, int th, float frac) {
      int x0 = this.sx(txStart);
      int x1 = this.sx(txEnd);
      int y0 = this.sy(ty);
      int y1 = this.sy(ty + th);
      // Dark recessed track + 1px frame.
      g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF1A0F08);
      g.fill(x0, y0, x1, y1, 0xFF0C0905);
      int span = x1 - x0;
      int fill = Math.round(span * Mth.clamp(frac, 0.0F, 1.0F));
      if (fill > 0) {
         // Gold gradient body with a brighter top highlight line.
         g.fillGradient(x0, y0, x0 + fill, y1, 0xFFFFC34B, 0xFFD9881F);
         g.fill(x0, y0, x0 + fill, y0 + Math.max(1, this.scale / 2), 0x66FFFFFF);
      }
      // Outer highlight frame.
      g.renderOutline(x0 - 1, y0 - 1, (x1 - x0) + 2, (y1 - y0) + 2, 0xFF5A3A18);
   }

   /** Render an ItemStack scaled into the slot at (col,row). */
   protected void drawItem(GuiGraphics g, net.minecraft.world.item.ItemStack stack, int col, int row, boolean decorations) {
      if (stack.isEmpty()) {
         return;
      }

      int x = this.slotX(col);
      int y = this.slotY(row);
      g.pose().pushPose();
      g.pose().translate((double)x, (double)y, 0.0);
      g.pose().scale((float)this.scale, (float)this.scale, 1.0F);
      g.renderItem(stack, 0, 0);
      if (decorations) {
         g.renderItemDecorations(this.font, stack, 0, 0);
      }

      g.pose().popPose();
   }

   /** Single quest icon used for every quest (the castle quest scroll). */
   protected static final int QUEST_ICON = 1;

   /** Draw a quest in its slot: single quest icon + progress bar (or green check when complete). */
   protected void drawQuestSlot(GuiGraphics g, com.fantasticpass.quest.Quest q, int col, int row, int progress, boolean claimed) {
      this.drawIcon(g, icon(QUEST_ICON), col, row);
      int x = this.slotX(col);
      int y = this.slotY(row);
      int s = this.slotPx();
      boolean complete = claimed || progress >= q.getTarget();
      if (complete) {
         g.fill(x, y, x + s, y + s, 0x3355FF55);
         g.drawString(this.font, "\u2714", x + s - 9, y + s - 9, 0xFF6FE06F, true);
      } else {
         float frac = Mth.clamp((float)progress / (float)q.getTarget(), 0.0F, 1.0F);
         int bx = x + 2;
         int by = y + s - 5;
         int bw = s - 4;
         g.fill(bx, by, bx + bw, by + 3, 0xFF0E1A20);
         g.fill(bx, by, bx + Math.round(bw * frac), by + 3, 0xFF00E5FF);
      }
   }

   /** Draw a PREMIUM quest that the (free) player cannot progress yet: dimmed scroll + padlock. */
   protected void drawQuestSlotLocked(GuiGraphics g, int col, int row) {
      int x = this.slotX(col);
      int y = this.slotY(row);
      int s = this.slotPx();
      g.setColor(1.0F, 1.0F, 1.0F, 0.4F);
      this.drawIcon(g, icon(QUEST_ICON), col, row);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      g.fill(x, y, x + s, y + s, 0x55101018);
      int q = Math.max(8, s / 2);
      g.blit(icon(5), x + (s - q) / 2, y + (s - q) / 2, q, q, 0.0F, 0.0F, 16, 16, 16, 16);
   }

   protected java.util.List<Component> questLockedTooltip(com.fantasticpass.quest.Quest q) {
      java.util.List<Component> l = new java.util.ArrayList<>();
      l.add(q.getDescription().copy().withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.BOLD));
      l.add(Component.translatable("fantasticpass.quest.premium_only").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
      l.add(Component.translatable("fantasticpass.quest.points", q.getPoints()).withStyle(net.minecraft.ChatFormatting.AQUA));
      l.add(Component.translatable("fantasticpass.gui.premium_hint").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
      return l;
   }

   protected java.util.List<Component> questTooltip(com.fantasticpass.quest.Quest q, int progress, boolean claimed) {
      java.util.List<Component> l = new java.util.ArrayList<>();
      l.add(q.getDescription().copy().withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
      boolean complete = claimed || progress >= q.getTarget();
      l.add(Component.translatable("fantasticpass.quest.progress", Math.min(progress, q.getTarget()), q.getTarget())
         .withStyle(complete ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY));
      l.add(Component.translatable("fantasticpass.quest.points", q.getPoints()).withStyle(net.minecraft.ChatFormatting.AQUA));
      l.add(complete
         ? Component.translatable("fantasticpass.quest.completed").withStyle(net.minecraft.ChatFormatting.GREEN)
         : Component.translatable("fantasticpass.quest.in_progress").withStyle(net.minecraft.ChatFormatting.YELLOW));
      return l;
   }

   // --- Pleasant UI sound palette (amethyst chimes + bell-like notes). ---

   /** Soft, glassy click used for buttons and tile selection. */
   protected void playClick(float pitch) {
      this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, Mth.clamp(pitch + 0.2F, 0.5F, 2.0F));
   }

   /** Bright chime, e.g. when opening a screen or turning a page. */
   protected void playChime(float pitch) {
      this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, Mth.clamp(pitch, 0.5F, 2.0F));
   }

   /** Rewarding two-layer sound for a successful FREE claim. */
   protected void playClaimFx() {
      this.playSound(SoundEvents.PLAYER_LEVELUP, 0.9F);
      this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F);
   }

   /**
    * Claim feedback. A premium claim is noticeably more epic (challenge-complete
    * fanfare + beacon shimmer) than a free claim, while staying pleasant.
    */
   protected void playClaimFx(boolean premium) {
      if (premium) {
         this.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
         this.playSound(SoundEvents.PLAYER_LEVELUP, 1.1F);
         this.playSound(SoundEvents.BEACON_ACTIVATE, 1.6F);
         this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F);
      } else {
         this.playClaimFx();
      }
   }

   /** Low, soft note for a denied / unavailable action (never harsh). */
   protected void playDenied() {
      this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 0.55F);
   }

   protected void playSound(SoundEvent event, float pitch) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
   }

   @Override
   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}