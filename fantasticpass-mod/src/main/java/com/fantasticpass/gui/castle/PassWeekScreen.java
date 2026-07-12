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

   private final PassDefinition pass;
   private final PlayerPassData data;
   private final int week;

   public PassWeekScreen(Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier, int week) {
      super(Component.translatable("fantasticpass.gui.week", week), parent, castle("battlepass_quest_overview"), 43, 9, 20, 247, 160);
      this.pass = pass;
      this.data = data;
      this.week = week;
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.drawCastleBackground(g);
      if (isPeek()) {
         super.render(g, mouseX, mouseY, partialTick);
         return;
      }
      List<Quest> freeQuests = this.pass.weekFreeQuests(this.week);
      List<Quest> premiumQuests = this.pass.weekPremiumQuests(this.week);
      boolean premium = this.data.isPremium();
      List<Component> tooltip = null;

      // Row 1: free weekly quests. Row 2: premium weekly quests (locked for free players).
      for (int i = 0; i < freeQuests.size() && i < 5; i++) {
         int col = 2 + i;
         Quest q = freeQuests.get(i);
         int progress = this.data.getQuestProgress(q.getId());
         boolean claimed = this.data.isQuestClaimed(q.getId());
         this.drawQuestSlot(g, q, col, 1, progress, claimed);
         if (this.overSlot(mouseX, mouseY, col, 1)) {
            tooltip = this.questTooltip(q, progress, claimed);
         }
      }

      for (int i = 0; i < premiumQuests.size() && i < 5; i++) {
         int col = 2 + i;
         Quest q = premiumQuests.get(i);
         if (premium) {
            int progress = this.data.getQuestProgress(q.getId());
            boolean claimed = this.data.isQuestClaimed(q.getId());
            this.drawQuestSlot(g, q, col, 2, progress, claimed);
            if (this.overSlot(mouseX, mouseY, col, 2)) {
               tooltip = this.questTooltip(q, progress, claimed);
            }
         } else {
            this.drawQuestSlotLocked(g, col, 2);
            if (this.overSlot(mouseX, mouseY, col, 2)) {
               tooltip = this.questLockedTooltip(q);
            }
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
      if (isPeek()) {
         return super.mouseClicked(mouseX, mouseY, button);
      }
      if (button == 0 && this.overSlot(mouseX, mouseY, NAV_PREV, NAV_ROW)) {
         this.playClick(0.9F);
         this.onClose();
         return true;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}
