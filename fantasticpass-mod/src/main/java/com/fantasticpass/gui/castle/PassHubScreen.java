package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.gui.player.PassViewScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Battle Pass hub on the castle "main" texture. The clickable areas match the
 * baked button blocks exactly (Rewards / Battle Pass / Quests on rows 0-1, Info
 * on row 2). Hovering lights the block subtly and shows a tooltip.
 */
public final class PassHubScreen extends CastleScreen {
   // {colStart, rowStart, colEnd, rowEnd}
   private static final int[] REWARDS = {0, 0, 2, 1};
   private static final int[] PASS = {3, 0, 5, 1};
   private static final int[] QUESTS = {6, 0, 8, 1};
   private static final int[] INFO = {3, 2, 5, 2};

   private final PassDefinition pass;
   private final PlayerPassData data;
   private final int minutesPerTier;
   private float pulse;

   public PassHubScreen(PassDefinition pass, PlayerPassData data, int minutesPerTier) {
      super(Component.translatable("fantasticpass.gui.view.title"), null, castle("battlepass_main"), 22, 9, 0, 247, 103);
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

      List<Component> tip = null;
      tip = this.hoverBlock(g, REWARDS, mouseX, mouseY) ? this.tip("fantasticpass.gui.rewards", "fantasticpass.hub.rewards_desc") : tip;
      tip = this.hoverBlock(g, PASS, mouseX, mouseY) ? this.passTip() : tip;
      tip = this.hoverBlock(g, QUESTS, mouseX, mouseY) ? this.tip("fantasticpass.gui.tiers", "fantasticpass.hub.quests_desc") : tip;
      tip = this.hoverBlock(g, INFO, mouseX, mouseY) ? this.tip("fantasticpass.gui.info", "fantasticpass.hub.info_desc") : tip;

      super.render(g, mouseX, mouseY, partialTick);
      if (tip != null) {
         g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
      }
   }

   private List<Component> tip(String titleKey, String descKey) {
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable(titleKey).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
      l.add(Component.translatable(descKey).withStyle(net.minecraft.ChatFormatting.GRAY));
      return l;
   }

   private List<Component> passTip() {
      List<Component> l = new ArrayList<>();
      String name = this.pass.getName() != null && !this.pass.getName().isEmpty() ? this.pass.getName() : "Battle Pass";
      l.add(Component.literal(name).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
      l.add(Component.translatable("fantasticpass.gui.level", this.data.getCurrentTier()).withStyle(net.minecraft.ChatFormatting.YELLOW));
      l.add(Component.translatable("fantasticpass.gui.premium")
         .append(": ")
         .append(this.data.isPremium()
            ? Component.literal("\u2714").withStyle(net.minecraft.ChatFormatting.GREEN)
            : Component.literal("\u2715").withStyle(net.minecraft.ChatFormatting.RED)));
      return l;
   }

   private boolean hoverBlock(GuiGraphics g, int[] b, int mouseX, int mouseY) {
      int x0 = this.slotX(b[0]);
      int y0 = this.slotY(b[1]);
      int x1 = this.slotX(b[2]) + this.slotPx();
      int y1 = this.slotY(b[3]) + this.slotPx();
      boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
      if (hover) {
         float p = 0.12F + 0.06F * (float)Math.abs(Math.sin(this.pulse));
         g.fill(x0, y0, x1, y1, (int)(p * 255.0F) << 24 | 0xFFFFFF);
         g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFFFFE08A);
      }

      return hover;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.in(REWARDS, mouseX, mouseY) || this.in(PASS, mouseX, mouseY)) {
            this.open(new PassViewScreen(this, this.pass, this.data, this.minutesPerTier));
            return true;
         }

         if (this.in(QUESTS, mouseX, mouseY)) {
            this.open(new PassInfoScreen(this, this.pass, this.data, this.minutesPerTier, true));
            return true;
         }

         if (this.in(INFO, mouseX, mouseY)) {
            this.open(new PassInfoScreen(this, this.pass, this.data, this.minutesPerTier, false));
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean in(int[] b, double mx, double my) {
      return mx >= this.slotX(b[0]) && mx < this.slotX(b[2]) + this.slotPx() && my >= this.slotY(b[1]) && my < this.slotY(b[3]) + this.slotPx();
   }

   private void open(CastleScreen screen) {
      this.playClick(1.0F);
      Minecraft.getInstance().setScreen(screen);
   }
}
