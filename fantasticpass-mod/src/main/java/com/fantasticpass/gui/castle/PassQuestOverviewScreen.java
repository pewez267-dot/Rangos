package com.fantasticpass.gui.castle;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.Quest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Quest hub on the castle "quest_overview" texture (4 rows). Lists the weekly
 * quest sets (5 per page on row 1) plus prev / daily-quests / next on row 3.
 */
public final class PassQuestOverviewScreen extends CastleScreen {
   private static final int WEEK_ROW = 1;
   private static final int FIRST_COL = 2;
   private static final int PER_PAGE = 5;
   private static final int NAV_ROW = 3;
   private static final int NAV_PREV = 3;
   private static final int NAV_DAILY = 4;
   private static final int NAV_NEXT = 5;

   private final PassDefinition pass;
   private final PlayerPassData data;
   private final int pointsPerTier;
   private int page;
   private float pulse;

   public PassQuestOverviewScreen(net.minecraft.client.gui.screens.Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier) {
      super(Component.translatable("fantasticpass.gui.quests"), parent, castle("battlepass_quest"), 61, 9, 39, 247, 160);
      this.pass = pass;
      this.data = data;
      this.pointsPerTier = pointsPerTier;
   }

   private int pageCount() {
      return Math.max(1, (DefaultQuests.weekCount() + PER_PAGE - 1) / PER_PAGE);
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.pulse += partialTick * 0.12F;
      this.drawCastleBackground(g);
      List<Component> tooltip = null;

      for (int i = 0; i < PER_PAGE; i++) {
         int week = this.page * PER_PAGE + i + 1;
         if (week > DefaultQuests.weekCount()) {
            break;
         }

         int col = FIRST_COL + i;
         this.drawWeek(g, week, col);
         if (this.overSlot(mouseX, mouseY, col, WEEK_ROW)) {
            tooltip = this.weekTooltip(week);
         }
      }

      this.drawIcon(g, icon(6), NAV_PREV, NAV_ROW);
      this.drawIcon(g, icon(10), NAV_DAILY, NAV_ROW);
      this.drawIcon(g, icon(7), NAV_NEXT, NAV_ROW);
      this.hover(g, NAV_PREV, mouseX, mouseY);
      this.hover(g, NAV_DAILY, mouseX, mouseY);
      this.hover(g, NAV_NEXT, mouseX, mouseY);
      if (this.overSlot(mouseX, mouseY, NAV_DAILY, NAV_ROW)) {
         tooltip = List.of(Component.translatable("fantasticpass.gui.daily_quests").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      } else if (this.overSlot(mouseX, mouseY, NAV_PREV, NAV_ROW)) {
         tooltip = List.of(Component.translatable("fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_NEXT, NAV_ROW)) {
         tooltip = List.of(Component.translatable("fantasticpass.gui.next").withStyle(ChatFormatting.YELLOW));
      }

      super.render(g, mouseX, mouseY, partialTick);
      if (tooltip != null) {
         g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
      }
   }

   private void drawWeek(GuiGraphics g, int week, int col) {
      this.drawIcon(g, icon(1), col, WEEK_ROW);
      boolean done = this.weekDone(week);
      boolean current = week == this.data.getCurrentWeek();
      int x = this.slotX(col);
      int y = this.slotY(WEEK_ROW);
      int s = this.slotPx();
      if (done) {
         g.fill(x, y, x + s, y + s, 0x3355FF55);
         g.drawString(this.font, "\u2714", x + s - 9, y + s - 9, 0xFF6FE06F, true);
      }

      g.drawCenteredString(this.font, String.valueOf(week), x + s / 2, y + s / 2 - 4, current ? 0xFFFFE24B : 0xFFFFFFFF);
      if (current && !done) {
         float a = 0.45F + 0.45F * (float)Math.abs(Math.sin(this.pulse));
         g.renderOutline(x - 1, y - 1, s + 2, s + 2, (int)(a * 220.0F) << 24 | 0xFFE24B);
      }
   }

   private boolean weekDone(int week) {
      for (Quest q : DefaultQuests.weekQuests(week)) {
         if (!this.data.isQuestClaimed(q.getId())) {
            return false;
         }
      }

      return true;
   }

   private List<Component> weekTooltip(int week) {
      List<Quest> qs = DefaultQuests.weekQuests(week);
      int done = 0;
      for (Quest q : qs) {
         if (this.data.isQuestClaimed(q.getId())) {
            done++;
         }
      }

      List<Component> l = new ArrayList<>();
      l.add(Component.translatable("fantasticpass.gui.week", week).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      l.add(Component.translatable("fantasticpass.quest.progress", done, qs.size()).withStyle(done == qs.size() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
      l.add(Component.translatable("fantasticpass.gui.click_view").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
      return l;
   }

   private void hover(GuiGraphics g, int col, int mouseX, int mouseY) {
      if (this.overSlot(mouseX, mouseY, col, NAV_ROW)) {
         int x = this.slotX(col);
         int y = this.slotY(NAV_ROW);
         g.fill(x, y, x + this.slotPx(), y + this.slotPx(), 0x33FFFFFF);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.overSlot(mouseX, mouseY, NAV_PREV, NAV_ROW)) {
            this.playClick(0.9F);
            if (this.page == 0) {
               this.onClose();
            } else {
               this.page--;
            }

            return true;
         }

         if (this.overSlot(mouseX, mouseY, NAV_NEXT, NAV_ROW)) {
            this.page = Mth.clamp(this.page + 1, 0, this.pageCount() - 1);
            this.playClick(1.1F);
            return true;
         }

         if (this.overSlot(mouseX, mouseY, NAV_DAILY, NAV_ROW)) {
            this.playClick(1.0F);
            Minecraft.getInstance().setScreen(new PassDailyQuestsScreen(this, this.pass, this.data, this.pointsPerTier));
            return true;
         }

         for (int i = 0; i < PER_PAGE; i++) {
            int week = this.page * PER_PAGE + i + 1;
            if (week <= DefaultQuests.weekCount() && this.overSlot(mouseX, mouseY, FIRST_COL + i, WEEK_ROW)) {
               this.playClick(1.0F);
               Minecraft.getInstance().setScreen(new PassWeekScreen(this, this.pass, this.data, this.pointsPerTier, week));
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}
