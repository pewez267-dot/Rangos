package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.Sfx;
import com.fshop.client.ShopWidgets;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.BuyPacket;
import com.fshop.network.OpenShopRequestPacket;
import com.fshop.network.PacketHandler;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Buy confirmation on shop_gui_confirmation.png. The amount is measured in
 * bundles (a bundle = the offer's sale pack); the price is per bundle and the
 * items delivered are bundle*amount. There is NO 64 cap -- you can buy as much
 * as your coins, stock and inventory allow; the "64" button just sets 64.
 */
public final class AmountScreen extends Screen {
   private static final int MAX_AMOUNT = 99999;

   private final PlayerShop shop;
   private final int offerIndex;
   private final long[] balances;
   private final ShopOffer offer;
   private int amount = 1;
   private int left;
   private int top;
   // hold-to-repeat state for the +/- steppers
   private int heldMinus = -1;
   private int heldPlus = -1;
   private int holdTicks;

   public AmountScreen(PlayerShop shop, int offerIndex, long[] balances) {
      super(Component.translatable("fshop.gui.amount.title"));
      this.shop = shop;
      this.offerIndex = offerIndex;
      this.balances = balances;
      this.offer = shop.getOffers().get(offerIndex);
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
      this.amount = clamp(1);
   }

   private int bundle() {
      return Math.max(1, offer.getBundle());
   }

   private int step(int i) {
      return switch (i) {
         case 0 -> 1;
         case 1 -> 32;
         default -> 64;
      };
   }

   private int maxAmount() {
      if (offer.isInfinite()) {
         return MAX_AMOUNT;
      }
      return Math.max(1, offer.getStock() / bundle());
   }

   private int clamp(int v) {
      return Math.max(1, Math.min(v, maxAmount()));
   }

   private long totalItems() {
      return (long) bundle() * amount;
   }

   private long total() {
      return offer.getUnitPrice() * (long) amount;
   }

   private boolean canAfford() {
      return total() <= balances[offer.getCoin()];
   }

   private boolean inStock() {
      return offer.isInfinite() || offer.getStock() >= totalItems();
   }

   private boolean canBuy() {
      return canAfford() && inStock();
   }

   private boolean inBox(double mx, double my, int[] box) {
      return FShopTheme.inside(mx, my, left + box[0], top + box[1], box[2] - box[0], box[3] - box[1]);
   }

   private void hoverBox(GuiGraphics g, int mouseX, int mouseY, int[] box) {
      if (inBox(mouseX, mouseY, box)) {
         g.fill(left + box[0], top + box[1], left + box[2], top + box[3], 0x55FFFFFF);
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.CONFIRMATION, left, top);

      for (int[] box : FShopTextures.MINUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      for (int[] box : FShopTextures.PLUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      hoverBox(g, mouseX, mouseY, FShopTextures.NO_BOX);
      if (canBuy()) {
         hoverBox(g, mouseX, mouseY, FShopTextures.YES_BOX);
      } else {
         int[] y = FShopTextures.YES_BOX;
         g.fill(left + y[0], top + y[1], left + y[2], top + y[3], 0x77000000);
      }
      hoverBox(g, mouseX, mouseY, FShopTextures.SET_STACK_BOX);

      int coinHov = ShopWidgets.renderCoins(g, this.font, this.minecraft.player,
            left, top, mouseX, mouseY, offer.getCoin());

      // item centred in the recess; count = total items delivered (small font)
      var stack = offer.displayStack(1);
      int ix = left + FShopTextures.ITEM_CX - 8;
      int iy = top + FShopTextures.ITEM_CY - 8;
      g.renderFakeItem(stack, ix, iy);
      FShopTheme.drawCount(g, this.font, ix, iy, Long.toString(totalItems()));

      super.render(g, mouseX, mouseY, partial);
      renderTooltips(g, mouseX, mouseY, coinHov);
   }

   private void renderTooltips(GuiGraphics g, int mouseX, int mouseY, int coinHov) {
      if (inBox(mouseX, mouseY, FShopTextures.ITEM_FRAME)) {
         itemBreakdownTooltip(g, mouseX, mouseY);
         return;
      }
      for (int i = 0; i < 3; i++) {
         if (inBox(mouseX, mouseY, FShopTextures.MINUS_CELLS[i])) {
            tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.remove", step(i)));
            return;
         }
         if (inBox(mouseX, mouseY, FShopTextures.PLUS_CELLS[i])) {
            tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.add", step(i)));
            return;
         }
      }
      if (inBox(mouseX, mouseY, FShopTextures.SET_STACK_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.set64"));
         return;
      }
      if (inBox(mouseX, mouseY, FShopTextures.NO_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.amount.tip_no"));
         return;
      }
      if (inBox(mouseX, mouseY, FShopTextures.YES_BOX)) {
         tip(g, mouseX, mouseY, canBuy()
               ? Component.translatable("fshop.gui.amount.tip_yes")
               : Component.translatable(canAfford() ? "fshop.msg.out_of_stock" : "fshop.msg.cannot_afford"));
         return;
      }
      if (coinHov >= 0) {
         List<Component> wt = new ArrayList<>();
         wt.add(Component.translatable("fshop.gui.wallet",
               balances[coinHov], Component.translatable(CoinEconomy.coinKey(coinHov))));
         wt.add(Component.translatable("fshop.gui.wallet_hint").withStyle(ChatFormatting.DARK_GRAY));
         g.renderComponentTooltip(this.font, wt, mouseX, mouseY);
      }
   }

   private void tip(GuiGraphics g, int mouseX, int mouseY, Component c) {
      List<Component> t = new ArrayList<>();
      t.add(c);
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   private void itemBreakdownTooltip(GuiGraphics g, int mouseX, int mouseY) {
      Component coin = Component.translatable(CoinEconomy.coinKey(offer.getCoin()));
      List<Component> t = new ArrayList<>();
      net.minecraft.world.item.ItemStack stack = offer.displayStack(1);
      t.add(stack.getHoverName());
      List<Component> lines = stack.getTooltipLines(this.minecraft.player,
            net.minecraft.world.item.TooltipFlag.Default.NORMAL);
      for (int i = 1; i < lines.size(); i++) {
         t.add(lines.get(i));
      }
      if (stack.isDamageableItem()) {
         int max = stack.getMaxDamage();
         int remaining = max - stack.getDamageValue();
         t.add(Component.translatable("fshop.gui.durability", remaining, max).withStyle(ChatFormatting.GRAY));
      }
      t.add(Component.translatable("fshop.gui.amount.quantity", totalItems()).withStyle(ChatFormatting.GRAY));
      if (bundle() > 1) {
         t.add(Component.translatable("fshop.gui.amount.packs", amount, bundle()).withStyle(ChatFormatting.DARK_GRAY));
      }
      int cc = CoinEconomy.coinColor(offer.getCoin());
      t.add(Component.translatable("fshop.gui.buy_price", offer.getUnitPrice(), coin)
            .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(cc))));
      t.add(Component.translatable("fshop.gui.total_n", total())
            .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(canAfford() ? cc : 0xFFDF2E38))));
      t.add(Component.translatable("fshop.gui.your_balance", balances[offer.getCoin()], coin)
            .withStyle(canAfford() ? ChatFormatting.DARK_GRAY : ChatFormatting.RED));
      if (!inStock()) {
         t.add(Component.empty());
         t.add(Component.translatable("fshop.msg.out_of_stock").withStyle(ChatFormatting.RED));
      } else if (!canAfford()) {
         t.add(Component.empty());
         t.add(Component.translatable("fshop.msg.cannot_afford").withStyle(ChatFormatting.RED));
      }
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      }
      for (int i = 0; i < 3; i++) {
         if (inBox(mx, my, FShopTextures.MINUS_CELLS[i])) {
            amount = clamp(amount - step(i));
            heldMinus = i;
            heldPlus = -1;
            holdTicks = 0;
            Sfx.step();
            return true;
         }
         if (inBox(mx, my, FShopTextures.PLUS_CELLS[i])) {
            amount = clamp(amount + step(i));
            heldPlus = i;
            heldMinus = -1;
            holdTicks = 0;
            Sfx.step();
            return true;
         }
      }
      if (inBox(mx, my, FShopTextures.SET_STACK_BOX)) {
         amount = clamp(64);
         Sfx.click();
         return true;
      }
      if (inBox(mx, my, FShopTextures.NO_BOX)) {
         Sfx.click();
         PacketHandler.sendToServer(new OpenShopRequestPacket(shop.getId()));
         return true;
      }
      if (canBuy() && inBox(mx, my, FShopTextures.YES_BOX)) {
         Sfx.success();
         PacketHandler.sendToServer(new BuyPacket(shop.getId(), offerIndex, amount));
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      this.heldMinus = -1;
      this.heldPlus = -1;
      this.holdTicks = 0;
      return super.mouseReleased(mx, my, button);
   }

   @Override
   public void tick() {
      super.tick();
      int held = this.heldMinus >= 0 ? this.heldMinus : this.heldPlus;
      if (held < 0) {
         this.holdTicks = 0;
         return;
      }
      this.holdTicks++;
      if (this.holdTicks < 6) {
         return; // small delay before auto-repeat kicks in
      }
      int t = this.holdTicks - 6;
      int interval = t > 40 ? 1 : 2;
      if (this.holdTicks % interval != 0) {
         return;
      }
      int mult = t > 70 ? 16 : (t > 40 ? 4 : 1); // accelerate the longer you hold
      int delta = step(held) * mult;
      // No sound on auto-repeat: it fired every tick and sounded noisy.
      this.amount = this.heldMinus >= 0 ? clamp(this.amount - delta) : clamp(this.amount + delta);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
