package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Secondary castle screen:
 *  - Info (battlepass_quest): what the pass is + the player's live stats.
 *  - Milestones (battlepass_quest_overview): scrollable list of tiers that grant
 *    a rank reward.
 * Press the prev icon (or Escape) to return to the hub.
 */
public final class PassInfoScreen extends CastleScreen {
   private static final int ROW_NAV = 3;
   private static final int NAV_PREV = 3;

   private final PassDefinition pass;
   private final PlayerPassData data;
   private final int minutesPerTier;
   private final boolean milestones;
   private final List<Integer> milestoneTiers = new ArrayList<>();
   private int scroll;

   public PassInfoScreen(@Nullable Screen parent, PassDefinition pass, PlayerPassData data, int minutesPerTier, boolean milestones) {
      super(
         Component.translatable(milestones ? "fantasticpass.gui.tiers" : "fantasticpass.gui.info"),
         parent,
         castle(milestones ? "battlepass_quest_overview" : "battlepass_quest"),
         milestones ? 43 : 61,
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
               this.milestoneTiers.add(t);
            }
         }
      }
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.drawCastleBackground(g);
      this.drawIcon(g, icon(6), NAV_PREV, ROW_NAV);
      if (this.overSlot(mouseX, mouseY, NAV_PREV, ROW_NAV)) {
         int x = this.slotX(NAV_PREV);
         int y = this.slotY(ROW_NAV);
         g.fill(x, y, x + this.slotPx(), y + this.slotPx(), 0x33FFFFFF);
      }

      if (this.milestones) {
         this.renderMilestones(g);
      } else {
         this.renderInfo(g);
      }

      super.render(g, mouseX, mouseY, partialTick);
   }

   private void renderInfo(GuiGraphics g) {
      int cx = this.width / 2;
      int y = this.slotY(0) + 2;
      int step = 12;
      String name = this.pass.getName() != null && !this.pass.getName().isEmpty() ? this.pass.getName() : "Battle Pass";
      g.drawCenteredString(this.font, Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), cx, y, 0xFFFFE9A8);
      y += step + 3;
      y = this.center(g, cx, y, step, Component.translatable("fantasticpass.gui.level", this.data.getCurrentTier()).withStyle(ChatFormatting.YELLOW));
      int target = this.data.getCurrentTier() * this.minutesPerTier + this.minutesPerTier;
      y = this.center(g, cx, y, step, Component.translatable("fantasticpass.gui.minutes", this.data.getMinutesActive(), target).withStyle(ChatFormatting.GRAY));
      y = this.center(
         g, cx, y, step,
         Component.translatable("fantasticpass.gui.premium").append(": ").withStyle(ChatFormatting.GOLD)
            .append(this.data.isPremium() ? Component.literal("\u2714").withStyle(ChatFormatting.GREEN) : Component.literal("\u2715").withStyle(ChatFormatting.RED))
      );
      y = this.center(g, cx, y, step,
         Component.translatable("fantasticpass.gui.rank_reward").append(": ").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal(String.valueOf(this.data.getEarnedRankIds().size())).withStyle(ChatFormatting.WHITE)));
      y += 4;
      g.drawCenteredString(this.font, Component.translatable("fantasticpass.hub.info_desc").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), cx, y, 0xFFAAAAAA);
   }

   private int center(GuiGraphics g, int cx, int y, int step, Component c) {
      g.drawCenteredString(this.font, c, cx, y, 0xFFFFFFFF);
      return y + step;
   }

   private void renderMilestones(GuiGraphics g) {
      int cx = this.width / 2;
      g.drawCenteredString(this.font, Component.translatable("fantasticpass.gui.tiers").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), cx, this.slotY(0) - 6, 0xFF00E5FF);
      int listX = this.slotX(1);
      int listTop = this.slotY(0) + 8;
      int listBottom = this.slotY(3) - 2;
      int rowH = 12;
      int visible = Math.max(1, (listBottom - listTop) / rowH);
      int max = Math.max(0, this.milestoneTiers.size() - visible);
      this.scroll = Mth.clamp(this.scroll, 0, max);

      if (this.milestoneTiers.isEmpty()) {
         g.drawCenteredString(this.font, Component.literal("\u2014").withStyle(ChatFormatting.GRAY), cx, (listTop + listBottom) / 2, 0xFFAAAAAA);
         return;
      }

      for (int i = 0; i < visible; i++) {
         int idx = this.scroll + i;
         if (idx >= this.milestoneTiers.size()) {
            break;
         }

         int tier = this.milestoneTiers.get(idx);
         TierDefinition def = this.pass.getTier(tier);
         boolean unlocked = tier <= this.data.getCurrentTier();
         boolean claimed = this.data.isTierClaimed(tier);
         int color = !unlocked ? 0xFFB9A98C : (claimed ? 0xFF6FE06F : 0xFFFFE24B);
         String status = !unlocked ? "\u2715" : (claimed ? "\u2714" : "\u25CF");
         String rank = def != null && def.hasRankReward() ? def.getRankReward().getRankDisplayText() : "-";
         g.drawString(this.font, status + " §7#" + tier + " §r" + rank, listX, listTop + i * rowH, color, false);
      }

      if (max > 0) {
         int trackH = listBottom - listTop;
         int knobH = Math.max(8, trackH * visible / this.milestoneTiers.size());
         int knobY = listTop + (trackH - knobH) * this.scroll / max;
         int barX = this.slotX(7) + this.slotPx();
         g.fill(barX, listTop, barX + 2, listBottom, 0xFF1A2A30);
         g.fill(barX, knobY, barX + 2, knobY + knobH, 0xFF00E5FF);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.overSlot(mouseX, mouseY, NAV_PREV, ROW_NAV)) {
         this.playClick(0.9F);
         this.onClose();
         return true;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.milestones) {
         this.scroll -= (int)Math.signum(delta);
      }

      return true;
   }
}
