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
 * Shared base for all "castle" Battle Pass screens. Handles:
 *  - blitting a 256x256 castle texture centred and integer-scaled,
 *  - texture-space -> screen-space coordinate mapping (sx / sy),
 *  - a smooth fade-in animation when the screen opens,
 *  - looping background music that survives navigation between castle screens,
 *  - convenience helpers for UI sounds.
 */
public abstract class CastleScreen extends Screen {
   protected static ResourceLocation castle(String name) {
      return new ResourceLocation("fantasticpass", "textures/gui/castle/" + name + ".png");
   }

   @Nullable
   protected final Screen parent;
   private final ResourceLocation background;
   private final int cx0;
   private final int cy0;
   private final int cx1;
   private final int cy1;

   protected int scale = 3;
   protected int left;
   protected int top;
   private long openTime;
   protected float anim;

   protected CastleScreen(Component title, @Nullable Screen parent, ResourceLocation background, int cx0, int cy0, int cx1, int cy1) {
      super(title);
      this.parent = parent;
      this.background = background;
      this.cx0 = cx0;
      this.cy0 = cy0;
      this.cx1 = cx1;
      this.cy1 = cy1;
   }

   @Override
   protected void init() {
      int contentW = this.cx1 - this.cx0;
      int contentH = this.cy1 - this.cy0;
      int availW = this.width - 32;
      int availH = this.height - 64;
      int fit = Math.min(availW / Math.max(1, contentW), availH / Math.max(1, contentH));
      this.scale = Mth.clamp(fit, 2, 5);
      int drawnW = contentW * this.scale;
      int drawnH = contentH * this.scale;
      int screenContentLeft = (this.width - drawnW) / 2;
      int screenContentTop = (this.height - drawnH) / 2 - 10;
      this.left = screenContentLeft - this.cx0 * this.scale;
      this.top = screenContentTop - this.cy0 * this.scale;
      this.openTime = System.currentTimeMillis();
      this.anim = 0.0F;
      PassMusicManager.ensurePlaying();
      this.initControls();
   }

   /** Subclasses add their widgets here (called after layout is computed). */
   protected abstract void initControls();

   /** Map a texture-space X (0..256) to screen space. */
   protected int sx(int tx) {
      return this.left + tx * this.scale;
   }

   /** Map a texture-space Y (0..256) to screen space. */
   protected int sy(int ty) {
      return this.top + ty * this.scale;
   }

   protected int contentScreenWidth() {
      return (this.cx1 - this.cx0) * this.scale;
   }

   protected float updateAnim() {
      float t = Mth.clamp((System.currentTimeMillis() - this.openTime) / 260.0F, 0.0F, 1.0F);
      this.anim = 1.0F - (1.0F - t) * (1.0F - t);
      return this.anim;
   }

   protected void drawCastleBackground(GuiGraphics g) {
      this.renderBackground(g);
      float a = this.updateAnim();
      int slide = Math.round((1.0F - a) * 14.0F);
      g.setColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, a));
      g.blit(this.background, this.left, this.top - slide, 256 * this.scale, 256 * this.scale, 0.0F, 0.0F, 256, 256, 256, 256);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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
