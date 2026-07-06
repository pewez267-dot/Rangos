package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.Sfx;
import com.fshop.client.ShopWidgets;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.AddOfferPacket;
import com.fshop.network.CollectPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.RemoveOfferPacket;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Owner GUI (create / edit) on shop_gui_sell_menu.png: offers in the wooden
 * window (left click = price, right click = remove) showing their real stock,
 * the owner's inventory on the gray grid (click to put an item on sale), the
 * house icon closes it, and pending earnings show as clickable coins seated in
 * the wooden header row beside the house icon (fixed slot per coin type).
 */
public final class ShopManageScreen extends Screen {
   private final PlayerShop shop;
   private int page;
   private int left;
   private int top;

   public ShopManageScreen(PlayerShop shop) {
      super(Component.literal(shop.getName()));
      this.shop = shop;
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
   }

   private int perPage() {
      return FShopTextures.contentCells();
   }

   /** The existing offer this item should merge into, or null. */
   private ShopOffer matchingOffer(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      }
      for (ShopOffer offer : shop.getOffers()) {
         if (ShopOffer.matchesForMerge(offer.getItem(), stack)) {
            return offer;
         }
      }
      return null;
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
   }

   private int earnCellX(int coin) {
      return left + FShopTextures.earnCellX(coin);
   }

   private int earnCellY() {
      return top + FShopTextures.earnCellY();
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.SELL_MENU, left, top);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = left + FShopTextures.contentCellX(i);
         int cy = top + FShopTextures.contentCellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x66FFD24A);
            hovered = start + i;
         }
         ShopOffer offer = offers.get(start + i);
         int ix = left + FShopTextures.contentItemX(i);
         int iy = top + FShopTextures.contentItemY(i);
         g.renderFakeItem(offer.displayStack(1), ix, iy);
         FShopTheme.drawCount(g, this.font, ix, iy, offer.isInfinite() ? "\u221E" : Integer.toString(offer.getStock()));
      }

      // close = house icon (slot 4)
      boolean homeHov = FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.HOME_CELL);
      FShopTextures.hoverCell(g, left, top, FShopTextures.HOME_CELL, homeHov);

      // pending earnings: one coin per type (only if > 0), seated beside the house
      int earnHov = -1;
      for (int c = 0; c < 3; c++) {
         if (shop.getPendingEarnings(c) <= 0) {
            continue;
         }
         int cx = earnCellX(c);
         int cy = earnCellY();
         g.blit(FShopTextures.EMPTY_SLOT, cx + 1, cy + 1, 0.0F, 0.0F, 16, 16, 16, 16);
         if (FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
            earnHov = c;
         }
         var coin = CoinEconomy.coinIcon(c);
         if (!coin.isEmpty()) {
            g.renderFakeItem(coin, cx + 1, cy + 1);
            FShopTheme.drawCount(g, this.font, cx + 1, cy + 1, Long.toString(shop.getPendingEarnings(c)));
         }
      }

      // paging on the wooden edges (slots 27 / 35)
      boolean hp = false;
      boolean hn = false;
      if (pageCount() > 1) {
         hp = page > 0 && FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.PREV_CELL);
         hn = page < pageCount() - 1 && FShopTextures.inCell(mouseX, mouseY, left, top, FShopTextures.NEXT_CELL);
         if (page > 0) {
            FShopTextures.blitIcon(g, FShopTextures.BACK_BUTTON, left, top, FShopTextures.PREV_CELL);
            FShopTextures.hoverCell(g, left, top, FShopTextures.PREV_CELL, hp);
         }
         if (page < pageCount() - 1) {
            FShopTextures.blitIcon(g, FShopTextures.NEXT_BUTTON, left, top, FShopTextures.NEXT_CELL);
            FShopTextures.hoverCell(g, left, top, FShopTextures.NEXT_CELL, hn);
         }
      }

      // owner inventory on the gray grid (click an item to put it on sale)
      int hoveredSlot = ShopWidgets.renderInventory(g, this.font, this.minecraft.player.getInventory(),
            left, top, mouseX, mouseY, true);

      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         offerTooltip(g, offers.get(hovered), mouseX, mouseY);
      } else if (hoveredSlot >= 0) {
         List<Component> t = new ArrayList<>();
         t.add(this.minecraft.player.getInventory().getItem(hoveredSlot).getHoverName());
         t.add(Component.translatable("fshop.gui.click_to_stock").withStyle(ChatFormatting.GREEN));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      } else if (earnHov >= 0) {
         List<Component> t = new ArrayList<>();
         t.add(Component.translatable("fshop.gui.manage.earnings", shop.getPendingEarnings(earnHov),
               Component.translatable(CoinEconomy.coinKey(earnHov))).withStyle(ChatFormatting.GOLD));
         t.add(Component.translatable("fshop.gui.manage.collect_tip").withStyle(ChatFormatting.GREEN));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      } else if (homeHov) {
         singleTip(g, mouseX, mouseY, Component.translatable("fshop.gui.close"));
      } else if (hp) {
         singleTip(g, mouseX, mouseY, Component.translatable("fshop.gui.nav.prev"));
      } else if (hn) {
         singleTip(g, mouseX, mouseY, Component.translatable("fshop.gui.nav.next"));
      }
   }

   private void singleTip(GuiGraphics g, int mouseX, int mouseY, Component c) {
      List<Component> t = new ArrayList<>();
      t.add(c);
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   /** Adds the item's own detail lines (enchantments, lore, durability...) after the name, only if any. */
   private void appendItemDetails(List<Component> t, net.minecraft.world.item.ItemStack stack) {
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
   }

   private void offerTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      net.minecraft.world.item.ItemStack stack = offer.displayStack(1);
      t.add(stack.getHoverName());
      appendItemDetails(t, stack);
      t.add(Component.translatable("fshop.gui.buy_price", offer.getUnitPrice(),
            Component.translatable(CoinEconomy.coinKey(offer.getCoin()))).withStyle(ChatFormatting.GREEN));
      t.add(Component.translatable("fshop.gui.stock", offer.getStock()).withStyle(ChatFormatting.GRAY));
      t.add(Component.empty());
      t.add(Component.translatable("fshop.gui.left_edit_price").withStyle(ChatFormatting.AQUA));
      t.add(Component.translatable("fshop.gui.right_remove").withStyle(ChatFormatting.RED));
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = left + FShopTextures.contentCellX(i);
         int cy = top + FShopTextures.contentCellY(i);
         if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
            int idx = start + i;
            if (button == 1) {
               Sfx.click();
               PacketHandler.sendToServer(new RemoveOfferPacket(shop.getId(), idx));
            } else if (button == 0) {
               Sfx.select();
               this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.EDIT, idx,
                     offers.get(idx).getUnitPrice(), offers.get(idx).getCoin(), offers.get(idx).getBundle()));
            }
            return true;
         }
      }
      if (button == 0) {
         int slot = ShopWidgets.slotAt(this.minecraft.player.getInventory(), left, top, mx, my);
         if (slot >= 0) {
            // If this exact item already has an offer, just restock it at the
            // price the owner already set (no price window). Only brand-new
            // items open the price editor.
            ShopOffer match = matchingOffer(this.minecraft.player.getInventory().getItem(slot));
            if (match != null) {
               Sfx.success();
               PacketHandler.sendToServer(new AddOfferPacket(shop.getId(), slot,
                     match.getUnitPrice(), match.getCoin(), match.getBundle()));
            } else {
               Sfx.select();
               this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.ADD, slot, 1, CoinEconomy.BRONZE, 1));
            }
            return true;
         }
         if (FShopTextures.inCell(mx, my, left, top, FShopTextures.HOME_CELL)) {
            Sfx.click();
            this.onClose();
            return true;
         }
         for (int c = 0; c < 3; c++) {
            if (shop.getPendingEarnings(c) > 0
                  && FShopTheme.inside(mx, my, earnCellX(c), earnCellY(), FShopTextures.CELL, FShopTextures.CELL)) {
               Sfx.success();
               PacketHandler.sendToServer(new CollectPacket(shop.getId()));
               return true;
            }
         }
         if (page > 0 && FShopTextures.inCell(mx, my, left, top, FShopTextures.PREV_CELL)) {
            page--;
            Sfx.page();
            return true;
         }
         if (page < pageCount() - 1 && FShopTextures.inCell(mx, my, left, top, FShopTextures.NEXT_CELL)) {
            page++;
            Sfx.page();
            return true;
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
