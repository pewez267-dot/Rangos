package com.fantasticpass.gui.castle;

import com.fantasticpass.client.PassMusicManager;
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
      int contentW = this.cx1 - this.cx0;
      int contentH = this.cy1 - this.cy0;
      int fit = Math.min((this.width - 24) / Math.max(1, contentW), (this.height - 40) / Math.max(1, contentH));
      this.scale = Mth.clamp(fit, 2, 5);
      int drawnW = contentW * this.scale;
      int drawnH = contentH * this.scale;
      int screenContentLeft = (this.width - drawnW) / 2;
      int screenContentTop = (this.height - drawnH) / 2;
      this.left = screenContentLeft - this.cx0 * this.scale;
      this.top = screenContentTop - this.cy0 * this.scale;
      this.openTime = System.currentTimeMillis();
      this.anim = 0.0F;
      PassMusicManager.ensurePlaying();
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
      this.renderBackground(g);
      float a = this.updateAnim();
      g.setColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, a));
      g.blit(this.background, this.left, this.top, 256 * this.scale, 256 * this.scale, 0.0F, 0.0F, 256, 256, 256, 256);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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

   protected void playClick(float pitch) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), pitch));
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
