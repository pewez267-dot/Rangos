package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.AddOfferPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.RequestManagePacket;
import com.fshop.network.SetPricePacket;
import com.fshop.shop.PlayerShop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Price / currency editor built on the real shop_gui_confirmation.png texture
 * (no hand-drawn panel): the item being priced sits in the centre frame, the
 * red bar lowers the price (-1/-10/-100) and the green bar raises it
 * (+1/+10/+100), the three coins on the gray grid pick the currency, and NO/YES
 * cancel or save. The price can also be typed on the keyboard.
 */
public final class PriceInputScreen extends Screen {
   public enum Mode {
      ADD, EDIT
   }

   private static final long CAP = 999_999_999L;
   private static final int[] STEPS = {1, 10, 100};

   private final PlayerShop shop;
   private final Mode mode;
   private final int ref;
   private int coin;
   private ItemStack itemStack = ItemStack.EMPTY;
   private String buf;
   private int left;
   private int top;

   public PriceInputScreen(PlayerShop shop, Mode mode, int ref, long initial, int coin) {
      super(Component.translatable(mode == Mode.ADD ? "fshop.gui.price.add" : "fshop.gui.price.edit"));
      this.shop = shop;
      this.mode = mode;
      this.ref = ref;
      this.coin = coin;
      this.buf = Long.toString(Math.max(1L, initial));
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
      if (mode == Mode.ADD) {
         this.itemStack = this.minecraft.player.getInventory().getItem(ref).copy();
      } else if (ref >= 0 && ref < shop.getOffers().size()) {
         this.itemStack = shop.getOffers().get(ref).displayStack(1);
      }
   }

   private long price() {
      try {
         return Math.max(1L, Math.min(CAP, Long.parseLong(buf)));
      } catch (NumberFormatException e) {
         return 1L;
      }
   }

   private void setPrice(long v) {
      this.buf = Long.toString(Math.max(1L, Math.min(CAP, v)));
   }

   private boolean inBox(double mx, double my, int[] box) {
      return FShopTheme.inside(mx, my, left + box[0], top + box[1], box[2] - box[0], box[3] - box[1]);
   }

   private void hoverBox(GuiGraphics g, int mouseX, int mouseY, int[] box) {
      if (inBox(mouseX, mouseY, box)) {
         g.fill(left + box[0], top + box[1], left + box[2], top + box[3], 0x55FFFFFF);
      }
   }

   private int coinCellX(int c) {
      return left + FShopTextures.coinCellX(c);
   }

   private int coinCellY() {
      return top + FShopTextures.coinCellY();
   }

   private void confirm() {
      long price = price();
      if (mode == Mode.ADD) {
         PacketHandler.sendToServer(new AddOfferPacket(shop.getId(), ref, price, coin));
      } else {
         PacketHandler.sendToServer(new SetPricePacket(shop.getId(), ref, price, coin));
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
      hoverBox(g, mouseX, mouseY, FShopTextures.YES_BOX);

      // item being priced, centred in the recessed frame
      if (!itemStack.isEmpty()) {
         g.renderFakeItem(itemStack, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8);
      }

      // price readout: a clean centred number in the clear band between the item
      // and the NO/YES buttons (the chosen currency is shown by the ringed coin
      // in the picker below, so no coin icon crowds the item here)
      g.drawCenteredString(this.font, "\u00a76" + price(), left + FShopTextures.ITEM_CX, top + 110, 0xFFFFD24A);

      // currency picker: three coins on the gray grid, selected one ringed
      int coinHov = -1;
      for (int c = 0; c < 3; c++) {
         int cx = coinCellX(c);
         int cy = coinCellY();
         g.blit(FShopTextures.EMPTY_SLOT, cx + 1, cy + 1, 0.0F, 0.0F, 16, 16, 16, 16);
         if (c == coin) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x66FFD24A);
         }
         if (FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL, FShopTextures.CELL)) {
            g.fill(cx, cy, cx + FShopTextures.CELL, cy + FShopTextures.CELL, 0x6682CD47);
            coinHov = c;
         }
         g.renderFakeItem(CoinEconomy.coinIcon(c), cx + 1, cy + 1);
      }

      super.render(g, mouseX, mouseY, partial);

      // tooltips
      for (int i = 0; i < 3; i++) {
         if (inBox(mouseX, mouseY, FShopTextures.MINUS_CELLS[i])) {
            tip(g, mouseX, mouseY, Component.translatable("fshop.gui.price.minus", STEPS[i]));
            return;
         }
         if (inBox(mouseX, mouseY, FShopTextures.PLUS_CELLS[i])) {
            tip(g, mouseX, mouseY, Component.translatable("fshop.gui.price.plus", STEPS[i]));
            return;
         }
      }
      if (coinHov >= 0) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.price.coin_tip",
               Component.translatable(CoinEconomy.coinKey(coinHov))));
      } else if (inBox(mouseX, mouseY, FShopTextures.NO_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.price.cancel_tip"));
      } else if (inBox(mouseX, mouseY, FShopTextures.YES_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable(
               mode == Mode.ADD ? "fshop.gui.price.confirm_tip_add" : "fshop.gui.price.confirm_tip_edit"));
      }
   }

   private void tip(GuiGraphics g, int mouseX, int mouseY, Component c) {
      List<Component> t = new ArrayList<>();
      t.add(c);
      g.renderComponentTooltip(this.font, t, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (int i = 0; i < 3; i++) {
            if (inBox(mx, my, FShopTextures.MINUS_CELLS[i])) {
               setPrice(price() - STEPS[i]);
               com.fshop.client.Sfx.click();
               return true;
            }
            if (inBox(mx, my, FShopTextures.PLUS_CELLS[i])) {
               setPrice(price() + STEPS[i]);
               com.fshop.client.Sfx.click();
               return true;
            }
         }
         for (int c = 0; c < 3; c++) {
            if (FShopTheme.inside(mx, my, coinCellX(c), coinCellY(), FShopTextures.CELL, FShopTextures.CELL)) {
               coin = c;
               com.fshop.client.Sfx.click();
               return true;
            }
         }
         if (inBox(mx, my, FShopTextures.NO_BOX)) {
            com.fshop.client.Sfx.click();
            PacketHandler.sendToServer(new RequestManagePacket(shop.getId()));
            return true;
         }
         if (inBox(mx, my, FShopTextures.YES_BOX)) {
            com.fshop.client.Sfx.success();
            confirm();
            return true;
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean charTyped(char c, int mods) {
      if (c >= '0' && c <= '9') {
         String next = (buf.equals("0") ? "" : buf) + c;
         if (next.length() <= 9) {
            buf = next;
         }
         return true;
      }
      return super.charTyped(c, mods);
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 259) { // backspace
         buf = buf.length() > 1 ? buf.substring(0, buf.length() - 1) : "1";
         return true;
      }
      if (key == 257 || key == 335) { // enter
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
