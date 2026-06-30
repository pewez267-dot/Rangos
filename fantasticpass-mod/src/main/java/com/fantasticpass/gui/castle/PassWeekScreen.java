package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.Quest;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Weekly quest list on the castle "quest" texture (5 rows). Quests sit on rows
 * 1-2 (cols 2-6); prev returns to the overview.
 */
public final class PassWeekScreen extends CastleScreen {
   private static final int NAV_ROW = 4;
   private static final int NAV_PREV = 3;
   private static final int NAV_INFO = 4;

   private final PlayerPassData data;
   private final int week;

   public PassWeekScreen(Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier, int week) {
      super(Component.translatable("fantasticpass.gui.week", week), parent, castle("battlepass_quest_overview"), 43, 9, 20, 247, 160);
      this.data = data;
      this.week = week;
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.drawCastleBackground(g);
      List<Quest> quests = DefaultQuests.allWeekQuests(this.week, this.data.isPremium());
      List<Component> tooltip = null;

      for (int i = 0; i < quests.size() && i < 10; i++) {
         int col = 2 + i % 5;
         int row = i < 5 ? 1 : 2;
         Quest q = quests.get(i);
         int progress = this.data.getQuestProgress(q.getId());
         boolean claimed = this.data.isQuestClaimed(q.getId());
         this.drawQuestSlot(g, q, col, row, progress, claimed);
         if (this.overSlot(mouseX, mouseY, col, row)) {
            tooltip = this.questTooltip(q, progress, claimed);
         }
      }

      this.drawIcon(g, icon(6), NAV_PREV, NAV_ROW);
      this.drawIcon(g, icon(1), NAV_INFO, NAV_ROW);
      if (this.overSlot(mouseX, mouseY, NAV_PREV, NAV_ROW)) {
         this.hover(g, NAV_PREV);
         tooltip = List.of(Component.translatable("fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_INFO, NAV_ROW)) {
         this.hover(g, NAV_INFO);
         tooltip = List.of(
            Component.translatable("fantasticpass.gui.week", this.week).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            Component.translatable("fantasticpass.hub.quests_desc").withStyle(ChatFormatting.GRAY)
         );
      }

      super.render(g, mouseX, mouseY, partialTick);
      if (tooltip != null) {
         g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
      }
   }

   private void hover(GuiGraphics g, int col) {
      int x = this.slotX(col);
      int y = this.slotY(NAV_ROW);
      g.fill(x, y, x + this.slotPx(), y + this.slotPx(), 0x33FFFFFF);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.overSlot(mouseX, mouseY, NAV_PREV, NAV_ROW)) {
         this.playClick(0.9F);
         this.onClose();
         return true;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}
