package com.fshop.client.screen;

import com.fshop.client.FShopTheme;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.AddOfferPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.RequestManagePacket;
import com.fshop.network.SetPricePacket;
import com.fshop.shop.PlayerShop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Set the price and choose the coin (bronze/silver/gold) for an offer. */
public final class PriceInputScreen extends Screen {
   public enum Mode {
      ADD, EDIT
   }

   private static final int PW = 210, PH = 150;
   private final PlayerShop shop;
   private final Mode mode;
   private final int ref;
   private final long initial;
   private int coin;
   private EditBox priceBox;
   private int left;
   private int top;

   public PriceInputScreen(PlayerShop shop, Mode mode, int ref, long initial, int coin) {
      super(Component.translatable("fshop.gui.price.title"));
      this.shop = shop;
      this.mode = mode;
      this.ref = ref;
      this.initial = initial;
      this.coin = coin;
   }

   @Override
   protected void init() {
      this.left = (this.width - PW) / 2;
      this.top = (this.height - PH) / 2;
      this.priceBox = new EditBox(this.font, left + 16, top + 44, PW - 32, 18, Component.literal("precio"));
      this.priceBox.setValue(Long.toString(Math.max(1L, initial)));
      this.priceBox.setFilter(s -> s.matches("\\d{0,10}"));
      this.addRenderableWidget(this.priceBox);
      this.setInitialFocus(this.priceBox);
   }

   private int coinBtnX(int c) {
      return left + 16 + c * 60;
   }

   private int coinBtnY() {
      return top + 78;
   }

   private long parsePrice() {
      try {
         return Math.max(1L, Long.parseLong(this.priceBox.getValue().trim()));
      } catch (NumberFormatException e) {
         return 1L;
      }
   }

   private void confirm() {
      long price = parsePrice();
      if (mode == Mode.ADD) {
         PacketHandler.sendToServer(new AddOfferPacket(shop.getId(), ref, price, coin));
      } else {
         PacketHandler.sendToServer(new SetPricePacket(shop.getId(), ref, price, coin));
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTheme.panel(g, left, top, PW, PH, FShopTheme.PANEL, FShopTheme.BORDER);
      g.fill(left, top, left + PW, top + 22, FShopTheme.HEADER);
      g.drawString(this.font, Component.translatable(mode == Mode.ADD ? "fshop.gui.price.add" : "fshop.gui.price.edit"),
            left + 10, top + 7, FShopTheme.GOLD, false);
      g.drawString(this.font, Component.translatable("fshop.gui.price.label"), left + 16, top + 32,
            FShopTheme.TEXT_DIM, false);
      g.drawString(this.font, Component.translatable("fshop.gui.price.coin"), left + 16, top + 68,
            FShopTheme.TEXT_DIM, false);

      // coin selector: gold(2) silver(1) bronze(0) shown left-to-right as 0,1,2
      for (int c = 0; c < 3; c++) {
         int x = coinBtnX(c);
         boolean sel = c == coin;
         boolean hov = FShopTheme.inside(mouseX, mouseY, x, coinBtnY(), 52, 22);
         FShopTheme.button(g, x, coinBtnY(), 52, 22, sel ? FShopTheme.GOLD : FShopTheme.BORDER, hov || sel);
         g.renderFakeItem(CoinEconomy.coinIcon(c), x + 3, coinBtnY() + 3);
         g.drawString(this.font, Component.translatable(CoinEconomy.coinKey(c)), x + 21, coinBtnY() + 7,
               sel ? FShopTheme.TEXT : FShopTheme.TEXT_DIM, false);
      }

      int cy = top + PH - 28;
      boolean okHov = FShopTheme.inside(mouseX, mouseY, left + 16, cy, 84, 18);
      FShopTheme.button(g, left + 16, cy, 84, 18, FShopTheme.BUY, okHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.confirm"), left + 58, cy + 5, FShopTheme.TEXT);
      boolean caHov = FShopTheme.inside(mouseX, mouseY, left + PW - 100, cy, 84, 18);
      FShopTheme.button(g, left + PW - 100, cy, 84, 18, FShopTheme.DANGER, caHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.cancel"), left + PW - 58, cy + 5, FShopTheme.TEXT);

      super.render(g, mouseX, mouseY, partial);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (int c = 0; c < 3; c++) {
            if (FShopTheme.inside(mx, my, coinBtnX(c), coinBtnY(), 52, 22)) {
               coin = c;
               return true;
            }
         }
         int cy = top + PH - 28;
         if (FShopTheme.inside(mx, my, left + 16, cy, 84, 18)) {
            confirm();
            return true;
         }
         if (FShopTheme.inside(mx, my, left + PW - 100, cy, 84, 18)) {
            PacketHandler.sendToServer(new RequestManagePacket(shop.getId()));
            return true;
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 257 || key == 335) {
         confirm();
         return true;
      }
      return super.keyPressed(key, scan, mods);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
