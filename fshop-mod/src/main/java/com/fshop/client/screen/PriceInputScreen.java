package com.fshop.client.screen;

import com.fshop.client.FShopTheme;
import com.fshop.network.AddOfferPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.RequestManagePacket;
import com.fshop.network.SetPricePacket;
import com.fshop.shop.PlayerShop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Numeric price entry used when stocking a new item or editing an offer's price. */
public final class PriceInputScreen extends Screen {
   public enum Mode {
      ADD,
      EDIT
   }

   private static final int PANEL_W = 200;
   private static final int PANEL_H = 110;

   private final PlayerShop shop;
   private final Mode mode;
   private final int ref;
   private final long initial;
   private EditBox priceBox;
   private int left;
   private int top;

   public PriceInputScreen(PlayerShop shop, Mode mode, int ref, long initial) {
      super(Component.translatable("fshop.gui.price.title"));
      this.shop = shop;
      this.mode = mode;
      this.ref = ref;
      this.initial = initial;
   }

   @Override
   protected void init() {
      this.left = (this.width - PANEL_W) / 2;
      this.top = (this.height - PANEL_H) / 2;
      this.priceBox = new EditBox(this.font, left + 12, top + 40, PANEL_W - 24, 18,
            Component.translatable("fshop.gui.price.field"));
      this.priceBox.setValue(Long.toString(Math.max(1L, initial)));
      this.priceBox.setFilter(s -> s.matches("\\d{0,10}"));
      this.addRenderableWidget(this.priceBox);
      this.setInitialFocus(this.priceBox);
   }

   private long parsePrice() {
      try {
         return Math.max(0L, Long.parseLong(this.priceBox.getValue().trim()));
      } catch (NumberFormatException e) {
         return 0L;
      }
   }

   private void confirm() {
      long price = parsePrice();
      if (mode == Mode.ADD) {
         PacketHandler.sendToServer(new AddOfferPacket(shop.getId(), ref, price));
      } else {
         PacketHandler.sendToServer(new SetPricePacket(shop.getId(), ref, price));
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTheme.panel(g, left, top, PANEL_W, PANEL_H, FShopTheme.PANEL, FShopTheme.BORDER);
      g.fill(left, top, left + PANEL_W, top + 22, FShopTheme.HEADER);
      g.drawString(this.font, Component.translatable(
            mode == Mode.ADD ? "fshop.gui.price.add" : "fshop.gui.price.edit"),
            left + 10, top + 7, FShopTheme.GOLD, false);
      g.drawString(this.font, Component.translatable("fshop.gui.price.label"), left + 12, top + 30,
            FShopTheme.TEXT_DIM, false);

      int cy = top + PANEL_H - 28;
      boolean okHov = FShopTheme.inside(mouseX, mouseY, left + 12, cy, 84, 18);
      FShopTheme.button(g, left + 12, cy, 84, 18, FShopTheme.BUY, okHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.confirm"), left + 54, cy + 5, FShopTheme.TEXT);
      boolean cancelHov = FShopTheme.inside(mouseX, mouseY, left + PANEL_W - 96, cy, 84, 18);
      FShopTheme.button(g, left + PANEL_W - 96, cy, 84, 18, FShopTheme.DANGER, cancelHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.cancel"), left + PANEL_W - 54, cy + 5, FShopTheme.TEXT);

      super.render(g, mouseX, mouseY, partial);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      int cy = top + PANEL_H - 28;
      if (button == 0 && FShopTheme.inside(mx, my, left + 12, cy, 84, 18)) {
         confirm();
         return true;
      }
      if (button == 0 && FShopTheme.inside(mx, my, left + PANEL_W - 96, cy, 84, 18)) {
         PacketHandler.sendToServer(new RequestManagePacket(shop.getId()));
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 257 || key == 335) { // Enter / numpad Enter
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

