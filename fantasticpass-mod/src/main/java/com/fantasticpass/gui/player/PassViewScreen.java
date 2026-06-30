package com.fantasticpass.gui.player;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.castle.CastleScreen;
import com.fantasticpass.network.ClaimTierPacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.progression.RewardDispatcher;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Rewards screen on the castle "reward" texture. Faithful to the source pack's
 * 5-row chest layout:
 *   row 0  -> FREE rewards     (9 tiers per page)
 *   row 1  -> tier number track
 *   row 2  -> PREMIUM rewards
 *   row 4  -> prev / info / next navigation (centre slots)
 * Items render directly in the slots; clicking a claimable tier claims it
 * (server-authoritative); hovering shows the reward tooltip.
 */
public final class PassViewScreen extends CastleScreen {
   private static final int TIERS_PER_PAGE = 9;
   private static final int PAGE_COUNT = 12; // ceil(100/9)
   private static final int ROW_FREE = 0;
   private static final int ROW_TRACK = 1;
   private static final int ROW_PREM = 2;
   private static final int ROW_NAV = 4;
   private static final int NAV_PREV = 3;
   private static final int NAV_INFO = 4;
   private static final int NAV_NEXT = 5;
   private static final int XP_PER_MINUTE = 10;

   private final PassDefinition pass;
   private PlayerPassData data;
   private final int pointsPerTier;
   private int page;
   private float pulse;
   private long flashUntil;
   private int flashTier;
   private boolean flashSuccess;

   public PassViewScreen(@Nullable Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier) {
      super(Component.translatable("fantasticpass.gui.view.title"), parent, castle("battlepass_reward"), 43, 9, 20, 247, 160);
      this.pass = pass;
      this.data = data;
      this.pointsPerTier = Math.max(1, pointsPerTier);
      int cur = Math.max(1, data.getCurrentTier());
      this.page = Mth.clamp((cur - 1) / TIERS_PER_PAGE, 0, PAGE_COUNT - 1);
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.pulse += partialTick * 0.12F;
      this.drawCastleBackground(g);

      int base = this.page * TIERS_PER_PAGE;
      List<Component> tooltip = null;

      for (int c = 0; c < TIERS_PER_PAGE; c++) {
         int tier = base + c + 1;
         if (tier > 100) {
            continue;
         }

         this.drawTierColumn(g, c, tier);
         if (this.overSlot(mouseX, mouseY, c, ROW_FREE)) {
            tooltip = this.rewardTooltip(tier, false);
         } else if (this.overSlot(mouseX, mouseY, c, ROW_PREM)) {
            tooltip = this.rewardTooltip(tier, true);
         } else if (this.overSlot(mouseX, mouseY, c, ROW_TRACK)) {
            tooltip = this.trackTooltip(tier);
         }
      }

      this.drawNav(g, mouseX, mouseY);
      if (this.overSlot(mouseX, mouseY, NAV_PREV, ROW_NAV)) {
         tooltip = List.of(Component.translatable(this.page == 0 ? "fantasticpass.gui.prev" : "fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_NEXT, ROW_NAV)) {
         tooltip = List.of(Component.translatable("fantasticpass.gui.next").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_INFO, ROW_NAV)) {
         tooltip = this.infoTooltip();
      }

      super.render(g, mouseX, mouseY, partialTick);
      if (tooltip != null) {
         g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
      }
   }

   private void drawTierColumn(GuiGraphics g, int col, int tier) {
      TierDefinition def = this.pass.getTier(tier);
      boolean unlocked = tier <= this.data.getCurrentTier();
      boolean claimed = this.data.isTierClaimed(tier);
      boolean premium = this.data.isPremium();
      boolean claimable = unlocked && !claimed;

      ItemStack free = def != null && !def.getFreeRewards().isEmpty() ? def.getFreeRewards().get(0) : ItemStack.EMPTY;
      ItemStack prem = def != null && !def.getPremiumRewards().isEmpty() ? def.getPremiumRewards().get(0) : ItemStack.EMPTY;
      int freeCount = def == null ? 0 : def.getFreeRewards().size();
      int premCount = def == null ? 0 : def.getPremiumRewards().size();

      // FREE row
      this.drawRewardSlot(g, col, ROW_FREE, free, freeCount, unlocked, claimed, claimable, false);
      // PREMIUM row (locked extra if not premium)
      this.drawRewardSlot(g, col, ROW_PREM, prem, premCount, unlocked, claimed, claimable && premium, !premium && !prem.isEmpty());

      // TRACK row: status icon + tier number
      int trackIcon = unlocked ? 4 : 5; // bp_icons_04 unlocked / 05 locked
      this.drawIcon(g, icon(claimed ? 3 : trackIcon), col, ROW_TRACK);
      int s = this.slotPx();
      int cx = this.slotX(col) + s / 2;
      int cy = this.slotY(ROW_TRACK) + s / 2 - 4;
      int numColor = tier == this.data.getCurrentTier() ? 0xFFFFE24B : (unlocked ? 0xFFFFFFFF : 0xFFB9A98C);
      g.drawCenteredString(this.font, String.valueOf(tier), cx, cy, numColor);
   }

   private void drawRewardSlot(
      GuiGraphics g, int col, int row, ItemStack stack, int count, boolean unlocked, boolean claimed, boolean claimable, boolean notEligible
   ) {
      int x = this.slotX(col);
      int y = this.slotY(row);
      int s = this.slotPx();

      if (!stack.isEmpty()) {
         this.drawItem(g, stack, col, row, unlocked);
         if (count > 1) {
            g.drawString(this.font, "+" + (count - 1), x + s - 12, y + s - 9, 0xFFFFE24B, true);
         }
      }

      if (notEligible) {
         g.fill(x, y, x + s, y + s, 0x99201018);
         this.drawIconAt(g, icon(9), x, y); // bp_icons_09 not eligible
      } else if (!unlocked) {
         g.fill(x, y, x + s, y + s, 0xAA0A0A0F);
      } else if (claimed) {
         g.fill(x, y, x + s, y + s, 0x3355FF55);
         g.drawString(this.font, "\u2714", x + s - 9, y + s - 9, 0xFF6FE06F, true);
      }

      if (claimable && !stack.isEmpty()) {
         float a = 0.45F + 0.45F * (float)Math.abs(Math.sin(this.pulse));
         g.renderOutline(x - 1, y - 1, s + 2, s + 2, (int)(a * 220.0F) << 24 | 0xFFE24B);
      }

      if (System.currentTimeMillis() < this.flashUntil && this.flashTier == this.tierOfColumn(col)) {
         float ft = Mth.clamp((this.flashUntil - System.currentTimeMillis()) / 600.0F, 0.0F, 1.0F);
         g.fill(x, y, x + s, y + s, (int)(ft * 170.0F) << 24 | (this.flashSuccess ? 0x6FFF6F : 0xFF6F6F));
      }
   }

   private void drawNav(GuiGraphics g, int mouseX, int mouseY) {
      this.drawIcon(g, icon(6), NAV_PREV, ROW_NAV); // prev
      this.drawIcon(g, icon(8), NAV_INFO, ROW_NAV); // info
      this.drawIcon(g, icon(7), NAV_NEXT, ROW_NAV); // next
      this.hoverSlot(g, NAV_PREV, ROW_NAV, mouseX, mouseY);
      this.hoverSlot(g, NAV_INFO, ROW_NAV, mouseX, mouseY);
      this.hoverSlot(g, NAV_NEXT, ROW_NAV, mouseX, mouseY);
   }

   private void hoverSlot(GuiGraphics g, int col, int row, int mouseX, int mouseY) {
      if (this.overSlot(mouseX, mouseY, col, row)) {
         int x = this.slotX(col);
         int y = this.slotY(row);
         int s = this.slotPx();
         g.fill(x, y, x + s, y + s, 0x33FFFFFF);
      }
   }

   private int tierOfColumn(int col) {
      return this.page * TIERS_PER_PAGE + col + 1;
   }

   private List<Component> rewardTooltip(int tier, boolean premium) {
      TierDefinition def = this.pass.getTier(tier);
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable("fantasticpass.gui.tier_info", tier)
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
         .append(Component.literal(premium ? "  " : "  "))
         .append(Component.translatable(premium ? "fantasticpass.gui.premium" : "fantasticpass.gui.free")
            .withStyle(premium ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)));
      List<ItemStack> rewards = def == null ? List.of() : (premium ? def.getPremiumRewards() : def.getFreeRewards());
      if (rewards.isEmpty()) {
         l.add(Component.literal("\u2014").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         for (ItemStack st : rewards) {
            l.add(Component.literal(" \u2022 ").withStyle(ChatFormatting.GRAY).append(st.getCount() + "x ").append(st.getHoverName()));
         }
      }

      if (def != null && def.hasRankReward() && !premium) {
         l.add(Component.literal(" \u2756 ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(def.getRankReward().getRankDisplayText())));
      }

      l.add(Component.empty());
      l.add(this.statusLine(tier, premium));
      return l;
   }

   private Component statusLine(int tier, boolean premium) {
      boolean unlocked = tier <= this.data.getCurrentTier();
      boolean claimed = this.data.isTierClaimed(tier);
      if (premium && !this.data.isPremium()) {
         return Component.translatable("fantasticpass.gui.not_eligible").withStyle(ChatFormatting.RED);
      } else if (claimed) {
         return Component.translatable("fantasticpass.gui.claimed").withStyle(ChatFormatting.GREEN);
      } else if (unlocked) {
         return Component.translatable("fantasticpass.gui.click_to_claim").withStyle(ChatFormatting.YELLOW);
      } else {
         return Component.translatable("fantasticpass.gui.locked").withStyle(ChatFormatting.RED);
      }
   }

   private List<Component> trackTooltip(int tier) {
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable("fantasticpass.gui.tier_info", tier).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      if (tier == this.data.getCurrentTier()) {
         int into = Math.max(0, this.data.getPoints() - tier * this.pointsPerTier);
         l.add(Component.translatable("fantasticpass.gui.xp", Math.min(into, this.pointsPerTier), this.pointsPerTier).withStyle(ChatFormatting.AQUA));
      }

      l.add(this.statusLine(tier, false));
      return l;
   }

   private List<Component> infoTooltip() {
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable("fantasticpass.gui.page", this.page + 1, PAGE_COUNT).withStyle(ChatFormatting.AQUA));
      l.add(Component.translatable("fantasticpass.gui.level", this.data.getCurrentTier()).withStyle(ChatFormatting.YELLOW));
      l.add(Component.translatable("fantasticpass.gui.premium")
         .append(": ")
         .append(this.data.isPremium()
            ? Component.literal("\u2714").withStyle(ChatFormatting.GREEN)
            : Component.literal("\u2715").withStyle(ChatFormatting.RED)));
      return l;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.overSlot(mouseX, mouseY, NAV_PREV, ROW_NAV)) {
            this.changePage(-1);
            return true;
         }

         if (this.overSlot(mouseX, mouseY, NAV_NEXT, ROW_NAV)) {
            this.changePage(1);
            return true;
         }

         int base = this.page * TIERS_PER_PAGE;
         for (int c = 0; c < TIERS_PER_PAGE; c++) {
            int tier = base + c + 1;
            if (tier <= 100 && (this.overSlot(mouseX, mouseY, c, ROW_FREE) || this.overSlot(mouseX, mouseY, c, ROW_PREM) || this.overSlot(mouseX, mouseY, c, ROW_TRACK))) {
               this.tryClaim(tier);
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void changePage(int delta) {
      if (delta < 0 && this.page == 0) {
         this.playClick(0.9F);
         Minecraft.getInstance().setScreen(this.parent);
         return;
      }

      int np = Mth.clamp(this.page + delta, 0, PAGE_COUNT - 1);
      if (np != this.page) {
         this.page = np;
         this.playClick(1.0F + 0.1F * delta);
      }
   }

   private void tryClaim(int tier) {
      if (tier <= this.data.getCurrentTier() && !this.data.isTierClaimed(tier)) {
         PacketHandler.sendToServer(new ClaimTierPacket(tier));
         this.playClick(0.8F);
      } else {
         this.playClick(0.5F);
      }
   }

   public void applyServerData(PlayerPassData serverData, RewardDispatcher.ClaimResult result, int tier) {
      this.data.copyFrom(serverData);
      this.flashTier = tier;
      this.flashUntil = System.currentTimeMillis() + 600L;
      this.flashSuccess = result == RewardDispatcher.ClaimResult.SUCCESS;
      if (this.flashSuccess) {
         this.playSound(SoundEvents.PLAYER_LEVELUP, 0.7F);
      } else {
         this.playClick(0.4F);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      this.changePage(delta > 0.0 ? -1 : 1);
      return true;
   }
}
