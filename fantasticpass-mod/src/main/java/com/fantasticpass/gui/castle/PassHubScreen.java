package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.player.PassViewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * The Battle Pass hub, rendered on the castle "main" texture. Four hotspots sit
 * over the baked buttons (Rewards / Premium / Quests / Info) with a hover glow,
 * and route to the matching sub-screen.
 */
public final class PassHubScreen extends CastleScreen {
   // Hotspots in texture space (256x256), measured from the art.
   private static final int[] REWARDS = {42, 28, 94, 62};
   private static final int[] PREMIUM = {100, 28, 152, 62};
   private static final int[] QUESTS = {158, 28, 208, 62};
   private static final int[] INFO = {96, 64, 162, 84};

   private final PassDefinition pass;
   private final PlayerPassData data;
   private final int minutesPerTier;
   private float pulse;

   public PassHubScreen(PassDefinition pass, PlayerPassData data, int minutesPerTier) {
      super(Component.translatable("fantasticpass.gui.view.title"), null, castle("battlepass_main"), 9, 0, 247, 103);
      this.pass = pass;
      this.data = data;
      this.minutesPerTier = minutesPerTier;
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.pulse += partialTick * 0.1F;
      this.drawCastleBackground(g);
      this.drawHotspot(g, REWARDS, mouseX, mouseY, 0xFFD24B);
      this.drawHotspot(g, PREMIUM, mouseX, mouseY, 0xFF66FF99);
      this.drawHotspot(g, QUESTS, mouseX, mouseY, 0xFF66CCFF);
      this.drawHotspot(g, INFO, mouseX, mouseY, 0xFFFFFFFF);
      // Level badge under the banner so the player always sees their progress from the hub.
      String lvl = Component.translatable("fantasticpass.gui.level", this.data.getCurrentTier()).getString();
      g.drawCenteredString(this.font, "§l" + lvl, this.width / 2, this.sy(92), 0xFFFFD24B);
      super.render(g, mouseX, mouseY, partialTick);
   }

   private void drawHotspot(GuiGraphics g, int[] r, int mouseX, int mouseY, int rgb) {
      int x0 = this.sx(r[0]);
      int y0 = this.sy(r[1]);
      int x1 = this.sx(r[2]);
      int y1 = this.sy(r[3]);
      boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
      if (hover) {
         float p = 0.35F + 0.25F * (float)Math.abs(Math.sin(this.pulse));
         int alpha = (int)(p * 110.0F) << 24;
         g.fill(x0, y0, x1, y1, alpha | rgb & 0xFFFFFF);
         g.renderOutline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, 0xFF000000 | rgb & 0xFFFFFF);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.inside(REWARDS, mouseX, mouseY)) {
            this.open(new PassViewScreen(this, this.pass, this.data, this.minutesPerTier, false));
            return true;
         }
         if (this.inside(PREMIUM, mouseX, mouseY)) {
            this.open(new PassViewScreen(this, this.pass, this.data, this.minutesPerTier, true));
            return true;
         }
         if (this.inside(QUESTS, mouseX, mouseY)) {
            this.open(new PassInfoScreen(this, this.pass, this.data, this.minutesPerTier, true));
            return true;
         }
         if (this.inside(INFO, mouseX, mouseY)) {
            this.open(new PassInfoScreen(this, this.pass, this.data, this.minutesPerTier, false));
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean inside(int[] r, double mx, double my) {
      return mx >= this.sx(r[0]) && mx < this.sx(r[2]) && my >= this.sy(r[1]) && my < this.sy(r[3]);
   }

   private void open(CastleScreen screen) {
      this.playSound((net.minecraft.sounds.SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
      Minecraft.getInstance().setScreen(screen);
   }
}
