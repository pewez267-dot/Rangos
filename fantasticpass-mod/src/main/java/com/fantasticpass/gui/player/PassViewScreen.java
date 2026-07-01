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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Rewards screen on the castle "reward" texture. The free and premium tracks are
 * shown on SEPARATE screens (this same class in two modes) but share one tier
 * track, so progression is connected:
 *   - free mode    -> free rewards on row 0
 *   - premium mode -> premium rewards on row 2
 *   - row 1 (tier number track) is always shown
 *   - row 4 -> prev / info / next navigation (centre slots)
 * Rewards render as the original treasure-cart icons; the actual items are in
 * the tooltip. Clicking a claimable tier claims it (server-authoritative).
 */
public final class PassViewScreen extends CastleScreen {
   private static final int TIERS_PER_PAGE = 9;
   private static final int ROW_FREE = 0;
   private static final int ROW_TRACK = 1;
   private static final int ROW_PREM = 2;
   private static final int ROW_NAV = 4;
   private static final int NAV_PREV = 3;
   private static final int NAV_INFO = 4;
   private static final int NAV_NEXT = 5;
   private static final int BAR_X0 = 49;
   private static final int BAR_X1 = 207;
   private static final int BAR_Y = 40;
   private static final int BAR_H = 4;

   private final PassDefinition pass;
   private PlayerPassData data;
   private final int pointsPerTier;
   private final boolean premiumView;
   private final int tierCount;
   private int page;
   private float pulse;
   private long flashUntil;
   private int flashTier;
   private boolean flashSuccess;
   private boolean flashPremium;

   public PassViewScreen(@Nullable Screen parent, PassDefinition pass, PlayerPassData data, int pointsPerTier, boolean premiumView) {
      super(Component.translatable("fantasticpass.gui.view.title"), parent, castle("battlepass_reward"), 43, 9, 20, 247, 160);
      this.pass = pass;
      this.data = data;
      this.pointsPerTier = Math.max(1, pointsPerTier);
      this.premiumView = premiumView;
      this.tierCount = pass.getTierCount();
      int cur = Math.max(1, data.getCurrentTier());
      this.page = Mth.clamp((cur - 1) / TIERS_PER_PAGE, 0, pageCount(TIERS_PER_PAGE, this.tierCount) - 1);
   }

   private int pages() {
      return pageCount(TIERS_PER_PAGE, this.tierCount);
   }

   @Override
   protected void initControls() {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.pulse += partialTick * 0.12F;
      this.drawCastleBackground(g);
      if (isPeek()) {
         super.render(g, mouseX, mouseY, partialTick);
         return;
      }

      // Slim tier-progress bar on the brick ledge under the banner (unobtrusive).
      this.drawProgressBar(g, BAR_X0, BAR_X1, BAR_Y, BAR_H, this.tierFraction());

      int base = pageBase(this.page, TIERS_PER_PAGE, this.tierCount);
      List<Component> tooltip = null;

      for (int c = 0; c < TIERS_PER_PAGE; c++) {
         int tier = base + c + 1;
         if (tier > this.tierCount) {
            continue;
         }

         this.drawTierColumn(g, c, tier);
         if (this.overSlot(mouseX, mouseY, c, ROW_FREE)) {
            tooltip = this.rewardTooltip(tier, false);
         } else if (this.overSlot(mouseX, mouseY, c, ROW_PREM)) {
            tooltip = this.premiumView ? this.rewardTooltip(tier, true) : this.premiumLockedTooltip(tier);
         } else if (this.overSlot(mouseX, mouseY, c, ROW_TRACK)) {
            tooltip = this.trackTooltip(tier);
         }
      }

      this.drawNav(g, mouseX, mouseY);
      if (this.overSlot(mouseX, mouseY, NAV_PREV, ROW_NAV)) {
         tooltip = List.of(Component.translatable("fantasticpass.gui.prev").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_NEXT, ROW_NAV)) {
         tooltip = List.of(Component.translatable("fantasticpass.gui.next").withStyle(ChatFormatting.YELLOW));
      } else if (this.overSlot(mouseX, mouseY, NAV_INFO, ROW_NAV)) {
         tooltip = this.infoTooltip();
      } else if (this.overProgressBar(mouseX, mouseY)) {
         tooltip = this.progressTooltip();
      }

      super.render(g, mouseX, mouseY, partialTick);
      if (tooltip != null) {
         g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
      }
   }

   /** Fraction [0,1] of points accumulated toward the next tier. */
   private float tierFraction() {
      int cur = this.data.getCurrentTier();
      if (cur >= this.tierCount) {
         return 1.0F;
      }
      int into = this.data.getPoints() - cur * this.pointsPerTier;
      return Mth.clamp((float)into / (float)this.pointsPerTier, 0.0F, 1.0F);
   }

   private boolean overProgressBar(double mx, double my) {
      return mx >= this.sx(BAR_X0) && mx < this.sx(BAR_X1) && my >= this.sy(BAR_Y) - 2 && my < this.sy(BAR_Y + BAR_H) + 2;
   }

   private List<Component> progressTooltip() {
      List<Component> l = new ArrayList<>();
      int cur = this.data.getCurrentTier();
      l.add(Component.translatable("fantasticpass.gui.progress").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      l.add(Component.translatable("fantasticpass.gui.level", cur).withStyle(ChatFormatting.YELLOW));
      if (cur >= this.tierCount) {
         l.add(Component.translatable("fantasticpass.gui.maxed").withStyle(ChatFormatting.GREEN));
      } else {
         int into = Math.max(0, this.data.getPoints() - cur * this.pointsPerTier);
         l.add(Component.translatable("fantasticpass.gui.xp", Math.min(into, this.pointsPerTier), this.pointsPerTier).withStyle(ChatFormatting.AQUA));
         l.add(Component.translatable("fantasticpass.gui.total_points", this.data.getPoints()).withStyle(ChatFormatting.GRAY));
      }
      return l;
   }

   private void drawTierColumn(GuiGraphics g, int col, int tier) {
      TierDefinition def = this.pass.getTier(tier);
      boolean unlocked = tier <= this.data.getCurrentTier();
      boolean premium = this.data.isPremium();
      boolean freeClaimed = this.data.isFreeClaimed(tier);
      boolean premClaimed = this.data.isPremiumClaimed(tier);

      // FREE row is shown in BOTH sections (premium unlocks free too).
      boolean hasFree = def != null && (!def.getFreeRewards().isEmpty() || def.hasRankReward());
      this.drawRewardSlot(g, col, ROW_FREE, hasFree, unlocked, freeClaimed, unlocked && !freeClaimed, false);

      // PREMIUM row.
      boolean hasPrem = def != null && !def.getPremiumRewards().isEmpty();
      if (this.premiumView) {
         this.drawRewardSlot(g, col, ROW_PREM, hasPrem, unlocked, premClaimed, unlocked && premium && !premClaimed, !premium && hasPrem);
      } else {
         // Free "Rewards" section: the premium line is locked/preview.
         this.drawRewardSlot(g, col, ROW_PREM, hasPrem, false, false, false, hasPrem);
      }

      // TRACK row: padlock progress icon (open when reached) + tier number.
      this.drawIcon(g, icon(unlocked ? 4 : 5), col, ROW_TRACK);
      int s = this.slotPx();
      int cx = this.slotX(col) + s / 2;
      int cy = this.slotY(ROW_TRACK) + s / 2 - 4;
      int numColor = tier == this.data.getCurrentTier() ? 0xFFFFE24B : (unlocked ? 0xFFFFFFFF : 0xFFB9A98C);
      g.drawCenteredString(this.font, String.valueOf(tier), cx, cy, numColor);
   }

   private void drawRewardSlot(
      GuiGraphics g, int col, int row, boolean hasReward, boolean unlocked, boolean claimed, boolean claimable, boolean notEligible
   ) {
      int x = this.slotX(col);
      int y = this.slotY(row);
      int s = this.slotPx();
      // Subtle cosmetic float so the reward icons feel alive. Small amplitude
      // keeps them inside the baked slot; this changes NO layout or click logic
      // (hit-testing still uses the static slot position).
      int yb = y + Math.round((float)Math.sin(this.pulse * 1.3 + col * 0.7 + row * 1.1) * (this.scale * 0.5F));

      if (notEligible) {
         this.drawIconAt(g, icon(9), x, yb); // locked / "premium" cart, no dark overlay
         this.drawSparkle(g, x, yb, s);
         return;
      }

      if (!hasReward) {
         return; // empty baked slot for tiers without a reward on this track
      }

      if (claimed) {
         this.drawIconAt(g, icon(3), x, yb); // emptied cart = claimed
      } else {
         this.drawIconAt(g, icon(2), x, yb); // full treasure cart (clean, no overlay)
         if (claimable) {
            float a = 0.45F + 0.45F * (float)Math.abs(Math.sin(this.pulse));
            g.renderOutline(x - 1, yb - 1, s + 2, s + 2, (int)(a * 220.0F) << 24 | 0xFFE24B);
         }
         if (row == ROW_PREM) {
            this.drawSparkle(g, x, yb, s); // premium reward: gentle twinkle
         }
      }

      // Claim flash only on the track that was actually claimed (free OR premium),
      // never both at once. Kept on the STATIC slot so it always covers the cell.
      boolean flashRow = (row == ROW_FREE && !this.flashPremium) || (row == ROW_PREM && this.flashPremium);
      if (flashRow && System.currentTimeMillis() < this.flashUntil && this.flashTier == this.tierOfColumn(col)) {
         float ft = Mth.clamp((this.flashUntil - System.currentTimeMillis()) / 600.0F, 0.0F, 1.0F);
         g.fill(x, y, x + s, y + s, (int)(ft * 170.0F) << 24 | (this.flashSuccess ? 0x6FFF6F : 0xFF6F6F));
      }
   }

   /** A couple of gently twinkling gold stars over premium reward icons. */
   private void drawSparkle(GuiGraphics g, int x, int y, int s) {
      float a1 = 0.30F + 0.50F * (float)Math.abs(Math.sin(this.pulse * 1.6));
      float a2 = 0.30F + 0.50F * (float)Math.abs(Math.sin(this.pulse * 1.6 + 1.9F));
      int pad = Math.max(2, this.scale);
      this.drawStar(g, x + s - pad, y + pad, a1);
      this.drawStar(g, x + pad, y + s - pad, a2);
   }

   private void drawStar(GuiGraphics g, int cx, int cy, float alpha) {
      int c = ((int)(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | 0xFFE9A6;
      g.fill(cx, cy - 1, cx + 1, cy + 2, c); // vertical
      g.fill(cx - 1, cy, cx + 2, cy + 1, c); // horizontal
   }

   private void drawNav(GuiGraphics g, int mouseX, int mouseY) {
      this.drawIcon(g, icon(6), NAV_PREV, ROW_NAV);
      this.drawIcon(g, icon(8), NAV_INFO, ROW_NAV);
      this.drawIcon(g, icon(7), NAV_NEXT, ROW_NAV);
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
      return pageBase(this.page, TIERS_PER_PAGE, this.tierCount) + col + 1;
   }

   private List<Component> rewardTooltip(int tier, boolean premium) {
      TierDefinition def = this.pass.getTier(tier);
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable("fantasticpass.gui.tier_info", tier)
         .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
         .append(Component.literal("  "))
         .append(Component.translatable(premium ? "fantasticpass.gui.premium" : "fantasticpass.gui.free")
            .withStyle(premium ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)));
      List<ItemStack> rewards = def == null ? List.of() : (premium ? def.getPremiumRewards() : def.getFreeRewards());
      if (rewards.isEmpty() && !(def != null && def.hasRankReward() && !premium)) {
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

   private List<Component> premiumLockedTooltip(int tier) {
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable("fantasticpass.gui.premium_rewards").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
      l.add(Component.translatable("fantasticpass.gui.premium_locked").withStyle(ChatFormatting.RED));
      l.add(Component.translatable("fantasticpass.gui.premium_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
      return l;
   }

   private Component statusLine(int tier, boolean premium) {
      boolean unlocked = tier <= this.data.getCurrentTier();
      boolean claimed = this.data.isClaimed(tier, premium);
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

      l.add(this.statusLine(tier, this.premiumView));
      return l;
   }

   private List<Component> infoTooltip() {
      List<Component> l = new ArrayList<>();
      l.add(Component.translatable(this.premiumView ? "fantasticpass.gui.premium" : "fantasticpass.gui.rewards").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
      l.add(Component.translatable("fantasticpass.gui.page", this.page + 1, this.pages()).withStyle(ChatFormatting.AQUA));
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
      if (isPeek()) {
         return super.mouseClicked(mouseX, mouseY, button);
      }
      if (button == 0) {
         if (this.overSlot(mouseX, mouseY, NAV_PREV, ROW_NAV)) {
            this.changePage(-1);
            return true;
         }

         if (this.overSlot(mouseX, mouseY, NAV_NEXT, ROW_NAV)) {
            this.changePage(1);
            return true;
         }

         int base = pageBase(this.page, TIERS_PER_PAGE, this.tierCount);
         for (int c = 0; c < TIERS_PER_PAGE; c++) {
            int tier = base + c + 1;
            if (tier > this.tierCount) {
               continue;
            }

            // FREE row always claims the FREE track independently.
            if (this.overSlot(mouseX, mouseY, c, ROW_FREE)) {
               this.tryClaim(tier, false);
               return true;
            }

            // Centre TRACK row claims the track of the section you are viewing.
            if (this.overSlot(mouseX, mouseY, c, ROW_TRACK)) {
               this.tryClaim(tier, this.premiumView);
               return true;
            }

            // PREMIUM row claims the PREMIUM track (only in the premium section).
            if (this.overSlot(mouseX, mouseY, c, ROW_PREM)) {
               if (this.premiumView) {
                  this.tryClaim(tier, true);
               } else {
                  this.playDenied();
               }

               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void changePage(int delta) {
      this.changePage(delta, true);
   }

   /**
    * @param allowExit when true (the prev arrow button) going back from page 0
    *                  returns to the hub; when false (scroll wheel) it just
    *                  clamps so scrolling never kicks the player out.
    */
   private void changePage(int delta, boolean allowExit) {
      if (delta < 0 && this.page == 0) {
         if (allowExit) {
            this.playClick(0.9F);
            Minecraft.getInstance().setScreen(this.parent);
         }
         return;
      }

      int np = Mth.clamp(this.page + delta, 0, this.pages() - 1);
      if (np != this.page) {
         this.page = np;
         this.playClick(1.0F + 0.1F * delta);
      }
   }

   private void tryClaim(int tier, boolean premium) {
      boolean unlocked = tier <= this.data.getCurrentTier();
      // In test mode the server lets you re-claim, so don't block on claimed here.
      boolean alreadyClaimed = !this.data.isTestMode() && this.data.isClaimed(tier, premium);
      boolean premiumEligible = !premium || this.data.isPremium();
      // Don't fire a claim for a track that has no reward on this tier.
      TierDefinition def = this.pass.getTier(tier);
      boolean hasReward = def != null && (premium
         ? !def.getPremiumRewards().isEmpty()
         : (!def.getFreeRewards().isEmpty() || def.hasRankReward()));

      if (unlocked && premiumEligible && hasReward && !alreadyClaimed) {
         PacketHandler.sendToServer(new ClaimTierPacket(tier, premium));
         this.playClick(0.8F);
      } else {
         this.playDenied();
      }
   }

   public void applyServerData(PlayerPassData serverData, RewardDispatcher.ClaimResult result, int tier, boolean premium) {
      this.data.copyFrom(serverData);
      this.flashTier = tier;
      this.flashPremium = premium;
      this.flashUntil = System.currentTimeMillis() + 600L;
      this.flashSuccess = result == RewardDispatcher.ClaimResult.SUCCESS;
      if (this.flashSuccess) {
         // Premium claims get the epic fanfare; free claims the lighter chime.
         this.playClaimFx(premium);
      } else {
         this.playDenied();
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      this.changePage(delta > 0.0 ? -1 : 1, false);
      return true;
   }
}
