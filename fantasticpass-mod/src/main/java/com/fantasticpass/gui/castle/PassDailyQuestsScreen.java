package com.fantasticpass.gui.castle;

import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Daily quest list on the castle "daily_quest" texture (5 rows). Free dailies
 * sit on row 1 (cols 2-6); premium dailies on row 2 (locked previews for free
 * players). Prev returns to the hub.
 */
public final class PassDailyQuestsScreen extends CastleScreen {
   private static final int NAV_ROW = 4;
   private static final int NAV_PREV = 3;
   private static final int NAV_INFO = 4;

   private final PassDefinition pass;
   private final PlayerPassData data;

   public PassDailyQuestsScreen(Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier) {
      super(Component.translatable("fantasticpass.gui.daily_quests"), parent, castle("battlepass_daily_quest"), 43, 9, 20, 247, 160);
      this.pass = pass;
      this.data = data;
   }

   @Override
   protected void initControls() {
   }

   private int premiumDailyCount() {
      int override = this.pass == null ? 0 : this.pass.getDailyPremiumCount();
      return override > 0 ? override : PassConfig.DAILY_PREMIUM_COUNT.get();
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.drawCastleBackground(g);
      boolean premium = this.data.isPremium();

      // Split the rolled dailies into free (df_) and premium (dp_) by id prefix.
      List<Quest> freeQuests = new ArrayList<>();
      List<Quest> premiumQuests = new ArrayList<>();
      for (Quest q : QuestManager.activeDaily(this.pass, this.data)) {
         (q.getId().startsWith("dp_") ? premiumQuests : freeQuests).add(q);
      }
      // Free players still SEE the premium dailies (locked previews).
      if (!premium) {
         premiumQuests = QuestManager.previewPremiumDaily(
            this.pass, Minecraft.getInstance().player.getUUID(), this.premiumDailyCount());
      }

      List<Component> tooltip = null;

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
      this.drawIcon(g, icon(10), NAV_INFO, NAV_ROW);
      if (this.overSlot(mouseX, mouseY, NAV_PREV, NAV_ROW)) {
         this.hover(g, NAV_PREV);
         tooltip = List.of(Component.translatable("fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_INFO, NAV_ROW)) {
         this.hover(g, NAV_INFO);
         tooltip = List.of(
            Component.translatable("fantasticpass.gui.daily_quests").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            Component.translatable("fantasticpass.gui.daily_reset").withStyle(ChatFormatting.GRAY),
            Component.translatable("fantasticpass.gui.daily_timer", formatRemaining()).withStyle(ChatFormatting.AQUA)
         );
      }

      // Live countdown to the daily reset (UTC midnight), centred under the banner.
      Component timer = Component.translatable("fantasticpass.gui.daily_timer", formatRemaining()).withStyle(ChatFormatting.AQUA);
      g.drawCenteredString(this.font, timer, this.width / 2, this.slotY(0) + 4, 0xFF7FE7FF);

      super.render(g, mouseX, mouseY, partialTick);
      if (tooltip != null) {
         g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
      }
   }

   /** Time left until the daily quests reset (UTC midnight), as HH:MM:SS. */
   private static String formatRemaining() {
      long dayMs = 86400000L;
      long rem = dayMs - (System.currentTimeMillis() % dayMs);
      long s = rem / 1000L;
      return String.format("%02d:%02d:%02d", s / 3600L, s % 3600L / 60L, s % 60L);
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
