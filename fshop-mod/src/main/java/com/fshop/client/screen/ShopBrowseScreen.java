package com.fshop.client.screen;

import com.fshop.client.FShopTheme;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.OpenShopRequestPacket;
import com.fshop.network.PacketHandler;
import com.fshop.shop.ShopSummary;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists every registered player shop; click one to browse it. */
public final class ShopBrowseScreen extends Screen {
   private static final int PANEL_W = 280;
   private static final int PANEL_H = 214;
   private static final int ROWS_PER_PAGE = 6;
   private static final int ROW_H = 28;

   private final List<ShopSummary> shops;
   private int page;
   private int left;
   private int top;

   public ShopBrowseScreen(List<ShopSummary> shops) {
      super(Component.translatable("fshop.gui.browse.title"));
      this.shops = shops;
   }

   @Override
   protected void init() {
      this.left = (this.width - PANEL_W) / 2;
      this.top = (this.height - PANEL_H) / 2;
   }

   private int pageCount() {
      return Math.max(1, (this.shops.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
   }



   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTheme.panel(g, left, top, PANEL_W, PANEL_H, FShopTheme.PANEL, FShopTheme.BORDER);
      g.fill(left, top, left + PANEL_W, top + 22, FShopTheme.HEADER);
      g.drawString(this.font, Component.translatable("fshop.gui.browse.title"), left + 10, top + 7,
            FShopTheme.GOLD, false);
      String pageStr = (page + 1) + "/" + pageCount();
      g.drawString(this.font, pageStr, left + PANEL_W - 10 - this.font.width(pageStr), top + 7,
            FShopTheme.TEXT_DIM, false);

      if (shops.isEmpty()) {
         g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.empty"),
               left + PANEL_W / 2, top + PANEL_H / 2 - 4, FShopTheme.TEXT_DIM);
      }

      int start = page * ROWS_PER_PAGE;
      for (int i = 0; i < ROWS_PER_PAGE && start + i < shops.size(); i++) {
         ShopSummary s = shops.get(start + i);
         int ry = top + 30 + i * ROW_H;
         boolean hov = FShopTheme.inside(mouseX, mouseY, left + 8, ry, PANEL_W - 16, ROW_H - 4);
         FShopTheme.button(g, left + 8, ry, PANEL_W - 16, ROW_H - 4, FShopTheme.BUY, hov);
         g.renderFakeItem(s.icon(), left + 13, ry + 4);
         g.drawString(this.font, s.name(), left + 34, ry + 4, FShopTheme.TEXT, false);
         g.drawString(this.font, Component.translatable("fshop.gui.browse.owner", s.ownerName()),
               left + 34, ry + 14, FShopTheme.TEXT_DIM, false);
         String priceLabel = Component.translatable("fshop.gui.browse.from",
               CoinEconomy.format(s.minPrice())).getString();
         g.drawString(this.font, priceLabel, left + PANEL_W - 16 - this.font.width(priceLabel), ry + 9,
               FShopTheme.BUY, false);
      }

      // pagination
      drawNav(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
   }

   private void drawNav(GuiGraphics g, int mouseX, int mouseY) {
      int y = top + PANEL_H - 22;
      boolean hp = FShopTheme.inside(mouseX, mouseY, left + 8, y, 60, 16) && page > 0;
      boolean hn = FShopTheme.inside(mouseX, mouseY, left + PANEL_W - 68, y, 60, 16) && page < pageCount() - 1;
      FShopTheme.button(g, left + 8, y, 60, 16, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
      FShopTheme.button(g, left + PANEL_W - 68, y, 60, 16, page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
      g.drawCenteredString(this.font, "<", left + 38, y + 4, FShopTheme.TEXT);
      g.drawCenteredString(this.font, ">", left + PANEL_W - 38, y + 4, FShopTheme.TEXT);
   }


   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         int start = page * ROWS_PER_PAGE;
         for (int i = 0; i < ROWS_PER_PAGE && start + i < shops.size(); i++) {
            int ry = top + 30 + i * ROW_H;
            if (FShopTheme.inside(mx, my, left + 8, ry, PANEL_W - 16, ROW_H - 4)) {
               PacketHandler.sendToServer(new OpenShopRequestPacket(shops.get(start + i).id()));
               return true;
            }
         }
         int y = top + PANEL_H - 22;
         if (page > 0 && FShopTheme.inside(mx, my, left + 8, y, 60, 16)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, left + PANEL_W - 68, y, 60, 16)) {
            page++;
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
