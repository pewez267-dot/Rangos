package com.fantasticpass.gui.player;

import com.fantasticpass.client.PassMusicInstance;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.castle.CastleScreen;
import com.fantasticpass.gui.widgets.ThemedButton;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.progression.RewardDispatcher;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Battle Pass rewards screen, rendered over the castle "reward" texture.
 * Top gold strip = FREE rewards, bottom strip = PREMIUM rewards, with the tier
 * number shown between them. Progress bar, claim and navigation controls sit in
 * a clean strip below the castle panel.
 */
public final class PassViewScreen extends CastleScreen {
   private static final int COLS = 7;
   private static final int PAGE_COUNT = 15; // ceil(100/7)
   private static final int SLOT_X0 = 88; // texture-space left of the gold strip interior
   private static final int SLOT_STEP = 17;
   private static final int FREE_ROW_TY = 46;
   private static final int PREM_ROW_TY = 80;
   private static final int NUMBER_TY = 67;
   private static final int XP_PER_MINUTE = 10;

   private final PassDefinition pass;
   private PlayerPassData data;
   private final int minutesPerTier;
   private final boolean focusPremium;

   private int page;
   private int selectedTier;
   private float pulse;
   private float pageSlide;
   private int slideDir;
   private long flashUntil;
   private int flashTier;
   private boolean flashSuccess;

   private ThemedButton prevButton;
   private ThemedButton nextButton;
   private ThemedButton claimButton;
   private ThemedButton backButton;
   private ThemedButton muteButton;

   public PassViewScreen(@Nullable Screen parent, PassDefinition pass, PlayerPassData data, int minutesPerTier, boolean focusPremium) {
      super(Component.translatable("fantasticpass.gui.view.title"), parent, castle("battlepass_reward"), 9, 20, 247, 160);
      this.pass = pass;
      this.data = data;
      this.minutesPerTier = Math.max(1, minutesPerTier);
      this.focusPremium = focusPremium;
      int cur = data.getCurrentTier();
      this.selectedTier = Mth.clamp(cur == 0 ? 1 : cur, 1, 100);
      this.page = (this.selectedTier - 1) / COLS;
   }

   @Override
   protected void initControls() {
      int panelBottom = this.sy(160);
      int barLeft = this.sx(20);
      int barRight = this.sx(236);
      int rowY = Math.min(this.height - 22, panelBottom + 14);
      int w = barRight - barLeft;
      int btnH = 18;
      int navW = 40;
      this.backButton = this.addRenderableWidget(
         new ThemedButton(barLeft, rowY, 44, btnH, Component.translatable("fantasticpass.gui.prev").copy(), 0xE0C45A, b -> this.onClose())
      );
      this.backButton.setMessage(Component.literal("§l<"));
      this.prevButton = this.addRenderableWidget(
         new ThemedButton(barLeft + 48, rowY, navW, btnH, Component.literal("\u25C0"), 0x00E5FF, b -> this.changePage(-1))
      );
      this.nextButton = this.addRenderableWidget(
         new ThemedButton(barRight - navW, rowY, navW, btnH, Component.literal("\u25B6"), 0x00E5FF, b -> this.changePage(1))
      );
      int claimX = barLeft + 48 + navW + 4;
      int claimW = barRight - navW - 4 - claimX;
      this.claimButton = this.addRenderableWidget(
         new ThemedButton(claimX, rowY, claimW, btnH, Component.translatable("fantasticpass.gui.claim"), 0xFFD24B, b -> this.claimSelected())
      );
      this.muteButton = this.addRenderableWidget(new ThemedButton(this.width - 30, 8, 24, 18, this.muteLabel(), 0x00E5FF, b -> this.toggleMute()));
   }

   private Component muteLabel() {
      return Component.literal(PassMusicInstance.muted ? "§7\u266a\u2715" : "§b\u266a");
   }

   private void toggleMute() {
      PassMusicInstance.muted = !PassMusicInstance.muted;
      this.muteButton.setMessage(this.muteLabel());
      this.playClick(1.0F);
   }

   private void changePage(int delta) {
      int np = Mth.clamp(this.page + delta, 0, PAGE_COUNT - 1);
      if (np != this.page) {
         this.slideDir = np > this.page ? 1 : -1;
         this.pageSlide = 1.0F;
         this.page = np;
         int t = this.page * COLS + 1;
         if (this.selectedTier < t || this.selectedTier > t + COLS - 1) {
            this.selectedTier = Math.min(100, t);
         }

         this.playClick(0.9F + 0.1F * this.slideDir);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.pulse += partialTick * 0.14F;
      if (this.pageSlide > 0.0F) {
         this.pageSlide = Math.max(0.0F, this.pageSlide - partialTick * 0.16F);
      }

      this.drawCastleBackground(g);
      this.drawPassName(g);
      this.drawRowLabels(g);
      this.drawGrid(g, mouseX, mouseY);
      this.drawProgress(g);
      this.updateButtons();
      super.render(g, mouseX, mouseY, partialTick);
      this.drawSelectedTooltip(g);
   }

   private void drawPassName(GuiGraphics g) {
      String name = this.pass.getName();
      if (name != null && !name.isEmpty()) {
         g.drawCenteredString(this.font, "§l" + name.toUpperCase(), this.width / 2, this.sy(10), 0xFFFFE9A8);
      }
   }

   private void drawRowLabels(GuiGraphics g) {
      Component free = Component.translatable("fantasticpass.gui.free");
      Component prem = Component.translatable("fantasticpass.gui.prem");
      int freeY = this.sy(FREE_ROW_TY) + (16 * this.scale - 8) / 2;
      int premY = this.sy(PREM_ROW_TY) + (16 * this.scale - 8) / 2;
      int rightX = this.sx(SLOT_X0) - 4;
      g.drawString(this.font, "§l" + free.getString(), rightX - this.font.width(free.getString()), freeY, 0xFFBFD0E0, true);
      int premColor = this.focusPremium ? 0xFFFFD24B : 0xFFE0B341;
      g.drawString(this.font, "§l" + prem.getString(), rightX - this.font.width(prem.getString()), premY, premColor, true);
   }

   private void drawGrid(GuiGraphics g, int mouseX, int mouseY) {
      int hovered = this.tierAt(mouseX, mouseY);
      int base = this.page * COLS;
      int slideOffset = Math.round(this.pageSlide * this.slideDir * -10.0F * this.scale);

      for (int c = 0; c < COLS; c++) {
         int tier = base + c + 1;
         if (tier > 100) {
            break;
         }

         int x = this.sx(SLOT_X0 + c * SLOT_STEP) + slideOffset;
         this.drawColumn(g, tier, x, tier == hovered);
      }
   }

   private void drawColumn(GuiGraphics g, int tier, int x, boolean hovered) {
      TierDefinition def = this.pass.getTier(tier);
      boolean claimed = this.data.isTierClaimed(tier);
      boolean unlocked = tier <= this.data.getCurrentTier();
      boolean selected = tier == this.selectedTier;
      boolean claimable = unlocked && !claimed;
      int sz = 16 * this.scale;
      int freeY = this.sy(FREE_ROW_TY);
      int premY = this.sy(PREM_ROW_TY);

      // tier number between the two rows
      int numY = this.sy(NUMBER_TY);
      int numCol = selected ? 0xFFFFD24B : (unlocked ? (claimed ? 0xFFAAAAAA : 0xFFFFFFFF) : 0xFF7A6A55);
      g.drawCenteredString(this.font, String.valueOf(tier), x + sz / 2, numY, numCol);

      ItemStack free = def != null && !def.getFreeRewards().isEmpty() ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
      ItemStack prem = def != null && !def.getPremiumRewards().isEmpty() ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;
      boolean premLocked = !prem.isEmpty() && !this.data.isPremium();
      this.drawSlot(g, x, freeY, sz, free, claimed, unlocked, false, claimable, selected, hovered);
      this.drawSlot(g, x, premY, sz, prem, claimed, unlocked, premLocked, claimable && !premLocked, selected, hovered);
   }

   private void drawSlot(
      GuiGraphics g, int x, int y, int sz, ItemStack stack, boolean claimed, boolean unlocked, boolean premLocked, boolean claimable, boolean selected, boolean hovered
   ) {
      int border;
      int fill;
      if (!unlocked || premLocked) {
         fill = 0xB0140D0A;
         border = 0xFF3A2A24;
      } else if (claimed) {
         fill = 0x804B7A2E;
         border = 0xFF6FBF3F;
      } else {
         fill = 0x80201A12;
         border = 0xFFE0B341;
      }

      g.fill(x, y, x + sz, y + sz, fill);
      g.renderOutline(x, y, sz, sz, border);

      if (!stack.isEmpty()) {
         g.pose().pushPose();
         g.pose().translate((float)x, (float)y, 0.0F);
         g.pose().scale((float)this.scale, (float)this.scale, 1.0F);
         g.renderItem(stack, 0, 0);
         if (unlocked && !premLocked) {
            g.renderItemDecorations(this.font, stack, 0, 0);
         }

         g.pose().popPose();
         if (!unlocked || premLocked) {
            g.fill(x + 1, y + 1, x + sz - 1, y + sz - 1, 0x9C000000);
         }
      }

      if (claimed) {
         g.drawString(this.font, "§a\u2714", x + sz - 9, y + sz - 9, 0xFF6FBF3F, true);
      }

      if (claimable) {
         float a = 0.5F + 0.5F * (float)Math.abs(Math.sin(this.pulse));
         int alpha = (int)(a * 200.0F) << 24;
         g.renderOutline(x - 1, y - 1, sz + 2, sz + 2, alpha | 0xFFD24B);
      }

      if (selected) {
         g.renderOutline(x - 2, y - 2, sz + 4, sz + 4, 0xFF00E5FF);
      } else if (hovered) {
         g.fill(x, y, x + sz, y + sz, 0x2600E5FF);
      }

      // claim flash feedback
      if (System.currentTimeMillis() < this.flashUntil && this.flashTier == this.tierOfSlotX(x)) {
         float ft = (this.flashUntil - System.currentTimeMillis()) / 600.0F;
         int fa = (int)(Mth.clamp(ft, 0.0F, 1.0F) * 180.0F) << 24;
         g.fill(x, y, x + sz, y + sz, fa | (this.flashSuccess ? 0x6FFF6F : 0xFF6F6F));
      }
   }

   private int tierOfSlotX(int x) {
      int base = this.page * COLS;
      int col = Math.round((float)(x - this.sx(SLOT_X0)) / (SLOT_STEP * this.scale));
      return base + col + 1;
   }

   private void drawProgress(GuiGraphics g) {
      int tier = this.data.getCurrentTier();
      int minutesInto = Math.max(0, this.data.getMinutesActive() - tier * this.minutesPerTier);
      int curXp = minutesInto * XP_PER_MINUTE;
      int tierXp = this.minutesPerTier * XP_PER_MINUTE;
      float frac = tier >= 100 ? 1.0F : Math.min(1.0F, (float)curXp / (float)tierXp);
      int bx = this.sx(20);
      int bw = this.sx(236) - bx;
      int by = this.sy(160) + 2;
      int bh = 7;

      String lvl = Component.translatable("fantasticpass.gui.level", tier).getString();
      g.drawString(this.font, "§l" + lvl, bx, by - 10, 0xFFFFD24B, true);
      String xp = tier >= 100
         ? Component.translatable("fantasticpass.gui.max").getString()
         : Component.translatable("fantasticpass.gui.xp", curXp, tierXp).getString();
      g.drawString(this.font, xp, bx + bw - this.font.width(xp), by - 10, 0xFFBFD0E0, true);
      String pg = Component.translatable("fantasticpass.gui.page", this.page + 1, PAGE_COUNT).getString();
      g.drawCenteredString(this.font, pg, bx + bw / 2, by - 10, 0xFF00E5FF);

      g.fill(bx, by, bx + bw, by + bh, 0xFF0E1A20);
      g.renderOutline(bx, by, bw, bh, 0xFF2E5560);
      int fw = Math.round((bw - 2) * frac);
      if (fw > 0) {
         g.fillGradient(bx + 1, by + 1, bx + 1 + fw, by + bh - 1, 0xFF00E5FF, 0xFF00A4D6);
         g.fill(bx + 1, by + 1, bx + 1 + fw, by + 2, 0x66FFFFFF);
      }
   }

   private void drawSelectedTooltip(GuiGraphics g) {
      TierDefinition def = this.pass.getTier(this.selectedTier);
      StringBuilder info = new StringBuilder("§f" + Component.translatable("fantasticpass.gui.tier_info", this.selectedTier).getString());
      if (def != null) {
         int items = def.getFreeRewards().size() + def.getPremiumRewards().size();
         int cmds = def.getFreeCommands().size() + def.getPremiumCommands().size();
         if (items > 0) {
            info.append("  §7").append(Component.translatable("fantasticpass.gui.items_count", items).getString());
         }

         if (cmds > 0) {
            info.append("  §b").append(Component.translatable("fantasticpass.gui.cmd_count", cmds).getString());
         }

         if (def.hasRankReward()) {
            info.append("  §d\u2756 ").append(def.getRankReward().getRankDisplayText());
         }
      }

      int by = this.sy(160) + 2;
      g.drawCenteredString(this.font, info.toString(), this.width / 2, by + 12, 0xFFFFFFFF);
   }

   private void updateButtons() {
      boolean unlocked = this.selectedTier <= this.data.getCurrentTier();
      boolean claimed = this.data.isTierClaimed(this.selectedTier);
      if (!unlocked) {
         this.claimButton.setMessage(Component.literal("§7" + Component.translatable("fantasticpass.gui.tier_locked").getString()));
         this.claimButton.setAccent(0x33333F);
         this.claimButton.active = false;
      } else if (claimed) {
         this.claimButton.setMessage(Component.literal("§a" + Component.translatable("fantasticpass.gui.claimed_check").getString() + " \u2714"));
         this.claimButton.setAccent(0x55FF55);
         this.claimButton.active = false;
      } else {
         this.claimButton.setMessage(Component.literal("§l" + Component.translatable("fantasticpass.gui.claim_tier", this.selectedTier).getString()));
         this.claimButton.setAccent(0xFFD24B);
         this.claimButton.active = true;
      }

      this.prevButton.active = this.page > 0;
      this.nextButton.active = this.page < PAGE_COUNT - 1;
   }

   private int tierAt(double mx, double my) {
      int sz = 16 * this.scale;
      int freeY = this.sy(FREE_ROW_TY);
      int premY = this.sy(PREM_ROW_TY);
      boolean inFree = my >= freeY && my < freeY + sz;
      boolean inPrem = my >= premY && my < premY + sz;
      if (!inFree && !inPrem) {
         return -1;
      }

      int relX = (int)(mx - this.sx(SLOT_X0));
      if (relX < 0) {
         return -1;
      }

      int step = SLOT_STEP * this.scale;
      int col = relX / step;
      if (relX % step > sz) {
         return -1;
      }

      if (col >= 0 && col < COLS) {
         int tier = this.page * COLS + col + 1;
         return tier <= 100 ? tier : -1;
      }

      return -1;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      }

      if (button == 0) {
         int tier = this.tierAt(mouseX, mouseY);
         if (tier > 0) {
            this.selectedTier = tier;
            this.playClick(1.2F);
            return true;
         }
      }

      return false;
   }

   private void claimSelected() {
      if (this.selectedTier <= this.data.getCurrentTier() && !this.data.isTierClaimed(this.selectedTier)) {
         // Server-authoritative: do NOT optimistically mark claimed. Wait for ClaimResultPacket.
         PacketHandler.sendToServer(new ClaimTierPacket(this.selectedTier));
         this.claimButton.active = false;
         this.playClick(0.8F);
      }
   }

   /** Called by the client packet handler when the server reports a claim result. */
   public void applyServerData(PlayerPassData serverData, RewardDispatcher.ClaimResult result, int tier) {
      this.data.copyFrom(serverData);
      this.flashTier = tier;
      this.flashUntil = System.currentTimeMillis() + 600L;
      this.flashSuccess = result == RewardDispatcher.ClaimResult.SUCCESS;
      if (this.flashSuccess) {
         this.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F);
      } else {
         this.playClick(0.6F);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      this.changePage(delta > 0.0 ? -1 : 1);
      return true;
   }
}
