package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.OpenShopRequestPacket;
import com.fshop.network.PacketHandler;
import com.fshop.shop.ShopSummary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists every registered player shop over the storefront texture. */
public final class ShopBrowseScreen extends Screen {
   private static final int FOOTER = 42;
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
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - (FShopTextures.GH + FOOTER)) / 2;
   }

   private int perPage() {
      return FShopTextures.cells();
   }

   private int pageCount() {
      return Math.max(1, (shops.size() + perPage() - 1) / perPage());
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
      FShopTextures.blitPanel(g, FShopTextures.MENU, left, top);

      List<ShopSummary> list = this.shops;
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < list.size(); i++) {
         int cx = gx(i);
         int cy = gy(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL_W, FShopTextures.CELL_H);
         if (hov) {
            g.fill(cx, cy, cx + FShopTextures.CELL_W, cy + FShopTextures.CELL_H, 0x6682CD47);
            hovered = start + i;
         }
         g.renderFakeItem(list.get(start + i).icon(),
               left + FShopTextures.itemX(i % FShopTextures.GRID_COLS),
               top + FShopTextures.itemY(i / FShopTextures.GRID_COLS));
      }

      renderFooter(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         shopTooltip(g, list.get(hovered), mouseX, mouseY);
      }
   }

   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + FShopTextures.GH;
      int w = FShopTextures.GW;
      g.fill(left, fy, left + w, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + w, fy + 1, FShopTheme.BORDER);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.title"),
            left + w / 2, fy + 6, FShopTheme.GOLD);
      if (shops.isEmpty()) {
         g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.empty"),
               left + w / 2, fy + 26, FShopTheme.TEXT_DIM);
         return;
      }
      int by = fy + 22;
      boolean hp = page > 0 && FShopTheme.inside(mouseX, mouseY, left + 8, by, 46, 16);
      boolean hn = page < pageCount() - 1 && FShopTheme.inside(mouseX, mouseY, left + w - 54, by, 46, 16);
      FShopTheme.button(g, left + 8, by, 46, 16, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
      FShopTheme.button(g, left + w - 54, by, 46, 16, page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
      g.drawCenteredString(this.font, "<", left + 31, by + 4, FShopTheme.TEXT);
      g.drawCenteredString(this.font, ">", left + w - 31, by + 4, FShopTheme.TEXT);
      g.drawCenteredString(this.font, (page + 1) + " / " + pageCount(), left + w / 2, by + 4, FShopTheme.TEXT_DIM);
   }

   private void shopTooltip(GuiGraphics g, ShopSummary s, int mouseX, int mouseY) {
      List<Component> t = new ArrayList<>();
      t.add(Component.literal(s.name()).withStyle(ChatFormatting.GOLD));
      t.add(Component.translatable("fshop.gui.browse.owner", s.ownerName()).withStyle(ChatFormatting.GRAY));
      t.add(Component.translatable("fshop.gui.browse.count", s.offerCount()).withStyle(ChatFormatting.DARK_GRAY));
      t.add(Component.translatable("fshop.gui.browse.from", CoinEconomy.format(s.minPrice()))
            .withStyle(ChatFormatting.GREEN));
      t.add(Component.empty());
      t.add(Component.translatable("fshop.gui.click_to_open").withStyle(ChatFormatting.YELLOW));
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         int start = page * perPage();
         for (int i = 0; i < perPage() && start + i < shops.size(); i++) {
            if (FShopTheme.inside(mx, my, gx(i), gy(i), FShopTextures.CELL_W, FShopTextures.CELL_H)) {
               PacketHandler.sendToServer(new OpenShopRequestPacket(shops.get(start + i).id()));
               return true;
            }
         }
         int by = top + FShopTextures.GH + 22;
         int w = FShopTextures.GW;
         if (page > 0 && FShopTheme.inside(mx, my, left + 8, by, 46, 16)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, left + w - 54, by, 46, 16)) {
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
