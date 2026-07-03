package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.CollectPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.RemoveOfferPacket;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Owner GUI: offers over the gray grid; add stock via the inventory picker. */
public final class ShopManageScreen extends Screen {
   private static final int FOOTER = 42;
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
      this.top = (this.height - (FShopTextures.GH + FOOTER)) / 2;
   }

   private int perPage() {
      return FShopTextures.cells();
   }

   private int pageCount() {
      return Math.max(1, (shop.getOffers().size() + perPage() - 1) / perPage());
   }

   private int gx(int i) {
      return left + FShopTextures.cellX(i % FShopTextures.GRID_COLS);
   }

   private int gy(int i) {
      return top + FShopTextures.cellY(i / FShopTextures.GRID_COLS);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.SELL_MENU, left, top);

      List<ShopOffer> offers = shop.getOffers();
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < offers.size(); i++) {
         int cx = gx(i);
         int cy = gy(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL_W, FShopTextures.CELL_H);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL_W, cy + FShopTextures.CELL_H, 0x66FFD24A);
            hovered = start + i;
         }
         ShopOffer offer = offers.get(start + i);
         int ix = left + FShopTextures.itemX(i % FShopTextures.GRID_COLS);
         int iy = top + FShopTextures.itemY(i / FShopTextures.GRID_COLS);
         g.renderFakeItem(offer.displayStack(1), ix, iy);
         g.renderItemDecorations(this.font, offer.displayStack(Math.min(offer.getStock(), 64)), ix, iy);
      }

      renderFooter(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         offerTooltip(g, offers.get(hovered), mouseX, mouseY);
      }
   }

   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + FShopTextures.GH;
      int w = FShopTextures.GW;
      g.fill(left, fy, left + w, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + w, fy + 1, FShopTheme.BORDER);
      g.drawCenteredString(this.font, shop.getName(), left + w / 2, fy + 6, FShopTheme.GOLD);

      int by = fy + 22;
      boolean addHov = FShopTheme.inside(mouseX, mouseY, left + 8, by, 78, 16);
      FShopTheme.button(g, left + 8, by, 78, 16, FShopTheme.BUY, addHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.manage.add"), left + 47, by + 4, FShopTheme.TEXT);

      boolean canCollect = shop.getPendingEarnings() > 0;
      boolean colHov = canCollect && FShopTheme.inside(mouseX, mouseY, left + 90, by, 78, 16);
      FShopTheme.button(g, left + 90, by, 78, 16, canCollect ? FShopTheme.GOLD : FShopTheme.BORDER, colHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.manage.collect_short",
            CoinEconomy.formatShort(shop.getPendingEarnings())), left + 129, by + 4, FShopTheme.TEXT);

      boolean closeHov = FShopTheme.inside(mouseX, mouseY, left + w - 74, by, 66, 16);
      FShopTheme.button(g, left + w - 74, by, 66, 16, FShopTheme.DANGER, closeHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.close"), left + w - 41, by + 4, FShopTheme.TEXT);

      if (pageCount() > 1) {
         g.drawString(this.font, (page + 1) + "/" + pageCount(), left + 8, fy + 6, FShopTheme.TEXT_DIM, false);
      }
   }

   private void offerTooltip(GuiGraphics g, ShopOffer offer, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(offer.displayStack(1).getHoverName());
      t.add(Component.translatable("fshop.gui.buy_price", CoinEconomy.format(offer.getUnitPrice()))
            .withStyle(ChatFormatting.GREEN));
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
         if (FShopTheme.inside(mx, my, gx(i), gy(i), FShopTextures.CELL_W, FShopTextures.CELL_H)) {
            int idx = start + i;
            if (button == 1) {
               PacketHandler.sendToServer(new RemoveOfferPacket(shop.getId(), idx));
            } else if (button == 0) {
               this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.EDIT, idx,
                     offers.get(idx).getUnitPrice()));
            }
            return true;
         }
      }
      if (button == 0) {
         int by = top + FShopTextures.GH + 22;
         int w = FShopTextures.GW;
         if (FShopTheme.inside(mx, my, left + 8, by, 78, 16)) {
            this.minecraft.setScreen(new InventoryPickerScreen(shop));
            return true;
         }
         if (shop.getPendingEarnings() > 0 && FShopTheme.inside(mx, my, left + 90, by, 78, 16)) {
            PacketHandler.sendToServer(new CollectPacket(shop.getId()));
            return true;
         }
         if (FShopTheme.inside(mx, my, left + w - 74, by, 66, 16)) {
            this.onClose();
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
