package com.fantasticpass.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Compact, book-styled "quest complete" notification. Replaces the old green
 * chat line with a single clean toast (vanilla toast frame + the pass quest
 * scroll icon), so completing a quest never spams or clutters the screen.
 */
public final class QuestCompleteToast implements Toast {
   private static final ResourceLocation FRAME = new ResourceLocation("textures/gui/toasts.png");
   private static final ResourceLocation SCROLL = new ResourceLocation("fantasticpass", "textures/gui/castle/icons/bp_icons_01.png");
   private static final long DURATION_MS = 4000L;

   private final Component title;
   private final Component line;

   public QuestCompleteToast(Component questDesc, int points, boolean premium) {
      this.title = Component.translatable(premium ? "fantasticpass.toast.quest_complete_premium" : "fantasticpass.toast.quest_complete")
         .withStyle(premium ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN, ChatFormatting.BOLD);
      this.line = questDesc.copy().withStyle(ChatFormatting.WHITE)
         .append(Component.literal("  +" + points).withStyle(ChatFormatting.AQUA));
   }

   @Override
   public int width() {
      return 176;
   }

   @Override
   public Toast.Visibility render(GuiGraphics g, ToastComponent toasts, long timeSinceLastVisible) {
      // Vanilla toast frame, widened to fit the quest description cleanly.
      g.blit(FRAME, 0, 0, 0, 0, this.width() / 2, this.height());
      g.blit(FRAME, this.width() / 2, 0, 160 - this.width() / 2, 0, this.width() / 2, this.height());

      Font font = toasts.getMinecraft().font;
      g.drawString(font, this.title, 30, 7, 0xFFFFFFFF, false);
      g.drawString(font, this.line, 30, 18, 0xFFFFFFFF, false);
      g.blit(SCROLL, 8, 8, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16);

      return timeSinceLastVisible >= DURATION_MS ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
   }
}
