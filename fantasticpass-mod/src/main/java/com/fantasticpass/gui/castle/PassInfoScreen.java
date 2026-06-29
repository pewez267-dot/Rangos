package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.widgets.ThemedButton;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Secondary castle screen used for two of the hub buttons:
 *  - Info  (battlepass_quest texture): pass stats and how progression works.
 *  - Quests/Milestones (battlepass_quest_overview texture): the list of tiers
 *    that grant a rank reward, shown as scrollable "milestones".
 */
public final class PassInfoScreen extends CastleScreen {
   private final PassDefinition pass;
   private final PlayerPassData data;
   private final int minutesPerTier;
   private final boolean milestones;
   private final List<int[]> milestoneTiers = new ArrayList<>(); // {tier}
   private int scroll;

   public PassInfoScreen(@Nullable Screen parent, PassDefinition pass, PlayerPassData data, int minutesPerTier, boolean milestones) {
      super(
         Component.translatable(milestones ? "fantasticpass.gui.tiers" : "fantasticpass.gui.info"),
         parent,
         castle(milestones ? "battlepass_quest_overview" : "battlepass_quest"),
         9,
         milestones ? 20 : 39,
         247,
         160
      );
      this.pass = pass;
      this.data = data;
      this.minutesPerTier = minutesPerTier;
      this.milestones = milestones;
      if (milestones) {
         for (int t = 1; t <= 100; t++) {
            TierDefinition def = pass.getTier(t);
            if (def != null && def.hasRankReward()) {
               this.milestoneTiers.add(new int[]{t});
            }
         }
      }
   }

   @Override
   protected void initControls() {
      this.addRenderableWidget(
         new ThemedButton(this.width / 2 - 40, Math.min(this.height - 22, this.sy(160) + 6), 80, 18, Component.literal("§l<"), 0xE0C45A, b -> this.onClose())
      );
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.drawCastleBackground(g);
      if (this.milestones) {
         this.renderMilestones(g, mouseX, mouseY);
      } else {
         this.renderInfo(g);
      }

      super.render(g, mouseX, mouseY, partialTick);
   }

   private void renderInfo(GuiGraphics g) {
      int cx = this.width / 2;
      int y = this.sy(58);
      int step = 13;
      String name = this.pass.getName() != null && !this.pass.getName().isEmpty() ? this.pass.getName() : "Battle Pass";
      g.drawCenteredString(this.font, "§l§e" + name.toUpperCase(), cx, y, 0xFFFFE9A8);
      y += step + 4;
      y = this.line(g, cx, y, step, "fantasticpass.gui.level", this.data.getCurrentTier());
      y = this.lineRaw(g, cx, y, step, "§7" + Component.translatable("fantasticpass.gui.minutes", this.data.getMinutesActive(), this.data.getCurrentTier() * this.minutesPerTier + this.minutesPerTier).getString());
      y = this.lineRaw(g, cx, y, step, "§7" + Component.translatable("fantasticpass.gui.minutes_per_tier").getString() + ": §f" + this.minutesPerTier);
      String prem = this.data.isPremium() ? "§a\u2714" : "§c\u2715";
      y = this.lineRaw(g, cx, y, step, "§6" + Component.translatable("fantasticpass.gui.premium").getString() + ": " + prem);
      int ranks = this.data.getEarnedRankIds().size();
      y = this.lineRaw(g, cx, y, step, "§d" + Component.translatable("fantasticpass.gui.rank_reward").getString() + ": §f" + ranks);
      y += 4;
      g.drawCenteredString(this.font, "§o§7" + Component.translatable("fantasticpass.gui.click_claim").getString(), cx, y, 0xFFAAAAAA);
   }

   private int line(GuiGraphics g, int cx, int y, int step, String key, Object arg) {
      g.drawCenteredString(this.font, "§f" + Component.translatable(key, arg).getString(), cx, y, 0xFFFFFFFF);
      return y + step;
   }

   private int lineRaw(GuiGraphics g, int cx, int y, int step, String text) {
      g.drawCenteredString(this.font, text, cx, y, 0xFFFFFFFF);
      return y + step;
   }

   private void renderMilestones(GuiGraphics g, int mouseX, int mouseY) {
      int listX = this.sx(40);
      int listW = this.sx(216) - listX;
      int listTop = this.sy(46);
      int listBottom = this.sy(150);
      int rowH = 12;
      int visible = (listBottom - listTop) / rowH;
      int max = Math.max(0, this.milestoneTiers.size() - visible);
      this.scroll = Mth.clamp(this.scroll, 0, max);

      g.drawCenteredString(this.font, "§l§b" + Component.translatable("fantasticpass.gui.tiers").getString(), this.width / 2, this.sy(34), 0xFF00E5FF);
      if (this.milestoneTiers.isEmpty()) {
         g.drawCenteredString(this.font, "§7—", this.width / 2, (listTop + listBottom) / 2, 0xFFAAAAAA);
         return;
      }

      for (int i = 0; i < visible; i++) {
         int idx = this.scroll + i;
         if (idx >= this.milestoneTiers.size()) {
            break;
         }

         int tier = this.milestoneTiers.get(idx)[0];
         TierDefinition def = this.pass.getTier(tier);
         boolean unlocked = tier <= this.data.getCurrentTier();
         boolean claimed = this.data.isTierClaimed(tier);
         int ry = listTop + i * rowH;
         int color = !unlocked ? 0xFF7A6A55 : (claimed ? 0xFF6FBF3F : 0xFFFFD24B);
         String status = !unlocked ? "\u2715" : (claimed ? "\u2714" : "\u25CF");
         String label = "§7#" + tier + "  §r" + (def != null && def.hasRankReward() ? def.getRankReward().getRankDisplayText() : "-");
         g.drawString(this.font, status + " " + label, listX, ry, color, false);
      }

      if (max > 0) {
         int trackH = listBottom - listTop;
         int knobH = Math.max(8, trackH * visible / this.milestoneTiers.size());
         int knobY = listTop + (trackH - knobH) * this.scroll / max;
         int sxr = this.sx(216) + 2;
         g.fill(sxr, listTop, sxr + 2, listBottom, 0xFF1A2A30);
         g.fill(sxr, knobY, sxr + 2, knobY + knobH, 0xFF00E5FF);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.milestones) {
         this.scroll -= (int)Math.signum(delta);
         return true;
      }

      return super.mouseScrolled(mouseX, mouseY, delta);
   }
}
