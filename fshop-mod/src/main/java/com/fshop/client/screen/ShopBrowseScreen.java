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
   private static final int FOOTER = 26;
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
      return FShopTextures.winCells();
   }

   private int pageCount() {
      return Math.max(1, (shops.size() + perPage() - 1) / perPage());
   }

   private int cellX(int i) {
      return left + FShopTextures.winX(i % FShopTextures.WIN_COLS);
   }

   private int cellY(int i) {
      return top + FShopTextures.winY(i / FShopTextures.WIN_COLS);
   }



   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.MENU, left, top);

      // title on the beam under the awning
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.title"),
            left + 128, top + 40, 0xFFFFF0C0);

      List<ShopSummary> list = this.shops;
      int start = page * perPage();
      int hovered = -1;
      for (int i = 0; i < perPage() && start + i < list.size(); i++) {
         int gx = cellX(i);
         int gy = cellY(i);
         boolean hov = FShopTheme.inside(mouseX, mouseY, gx, gy, 18, 18);
         if (hov) {
            g.fill(gx, gy, gx + 18, gy + 18, 0x6682CD47);
            hovered = start + i;
         }
         g.renderFakeItem(list.get(start + i).icon(), gx + 1, gy + 1);
      }

      if (list.isEmpty()) {
         g.drawCenteredString(this.font, Component.translatable("fshop.gui.browse.empty"),
               left + 128, top + 96, 0xFFB0483A);
      }

      renderFooter(g, mouseX, mouseY);
      super.render(g, mouseX, mouseY, partial);
      if (hovered >= 0) {
         shopTooltip(g, list.get(hovered), mouseX, mouseY);
      }
   }

   private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
      int fy = top + FShopTextures.GH;
      g.fill(left, fy, left + FShopTextures.GW, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + FShopTextures.GW, fy + 1, FShopTheme.BORDER);
      boolean hp = page > 0 && FShopTheme.inside(mouseX, mouseY, left + 8, fy + 5, 40, 16);
      boolean hn = page < pageCount() - 1 && FShopTheme.inside(mouseX, mouseY, left + FShopTextures.GW - 48, fy + 5, 40, 16);
      FShopTheme.button(g, left + 8, fy + 5, 40, 16, page > 0 ? FShopTheme.SELL : FShopTheme.BORDER, hp);
      FShopTheme.button(g, left + FShopTextures.GW - 48, fy + 5, 40, 16,
            page < pageCount() - 1 ? FShopTheme.SELL : FShopTheme.BORDER, hn);
      g.drawCenteredString(this.font, "<", left + 28, fy + 9, FShopTheme.TEXT);
      g.drawCenteredString(this.font, ">", left + FShopTextures.GW - 28, fy + 9, FShopTheme.TEXT);
      g.drawCenteredString(this.font, (page + 1) + " / " + pageCount(), left + 128, fy + 9, FShopTheme.TEXT_DIM);
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
            if (FShopTheme.inside(mx, my, cellX(i), cellY(i), 18, 18)) {
               PacketHandler.sendToServer(new OpenShopRequestPacket(shops.get(start + i).id()));
               return true;
            }
         }
         int fy = top + FShopTextures.GH;
         if (page > 0 && FShopTheme.inside(mx, my, left + 8, fy + 5, 40, 16)) {
            page--;
            return true;
         }
         if (page < pageCount() - 1 && FShopTheme.inside(mx, my, left + FShopTextures.GW - 48, fy + 5, 40, 16)) {
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
