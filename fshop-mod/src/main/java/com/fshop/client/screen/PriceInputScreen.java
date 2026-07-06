package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.client.Sfx;
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
 * Price / currency editor built directly on the real shop_gui_confirmation.png
 * texture (no hand-drawn panels): the item being priced sits in the centre
 * frame, the red bar lowers the price (-1/-10/-100) and the green bar raises it
 * (+1/+10/+100), the three coins on the gray grid pick the currency, and NO/YES
 * cancel or save. The price can also be typed on the keyboard.
 *
 * <p>How many units each sale delivers (the bundle) is chosen on the separate
 * real-texture {@link SellAmountScreen}: clicking the centred item here reopens
 * that picker, and the current amount is shown as the item's count so the
 * seller always sees whether they are selling by unit, pack or full stack.
 */
public final class PriceInputScreen extends Screen {
   private static final long CAP = 999_999_999L;
   private static final int[] STEPS = {1, 10, 100};

   public enum Mode {
      ADD, EDIT
   }

   private final PlayerShop shop;
   private final Mode mode;
   private final int ref;
   private final int bundle;
   private int coin;
   private ItemStack itemStack = ItemStack.EMPTY;
   private String buf;
   private int left;
   private int top;
   private int heldMinus = -1;
   private int heldPlus = -1;
   private int holdTicks;

   public PriceInputScreen(PlayerShop shop, Mode mode, int ref, long initial, int coin, int bundle) {
      super(Component.translatable(mode == Mode.ADD ? "fshop.gui.price.add" : "fshop.gui.price.edit"));
      this.shop = shop;
      this.mode = mode;
      this.ref = ref;
      this.coin = coin;
      this.buf = Long.toString(Math.max(1L, initial));
      this.bundle = Math.max(1, bundle);
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
      // Pin to count=1: the real inventory stack can have any count, and drawing
      // that raw number on the icon competes with our own price/bundle readout.
      if (!this.itemStack.isEmpty()) {
         this.itemStack.setCount(1);
      }
   }

   private long price() {
      try {
         return Math.max(1L, Math.min(CAP, Long.parseLong(buf)));
      } catch (NumberFormatException e) {
         return 1L;
      }
   }

   private int bundle() {
      int max = this.itemStack.isEmpty() ? 64 : Math.max(1, this.itemStack.getMaxStackSize());
      return Math.max(1, Math.min(max, this.bundle));
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

   private void openAmount() {
      Sfx.select();
      this.minecraft.setScreen(new SellAmountScreen(shop, mode, ref, price(), coin, bundle()));
   }

   private void confirm() {
      long price = price();
      int bundle = bundle();
      if (mode == Mode.ADD) {
         PacketHandler.sendToServer(new AddOfferPacket(shop.getId(), ref, price, coin, bundle));
      } else {
         PacketHandler.sendToServer(new SetPricePacket(shop.getId(), ref, price, coin, bundle));
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.CONFIRMATION, left, top);
      FShopTheme.footerHint(g, this.font, this.width, this.height,
            Component.translatable("fshop.gui.price.hint"));

      for (int[] box : FShopTextures.MINUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      for (int[] box : FShopTextures.PLUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      hoverBox(g, mouseX, mouseY, FShopTextures.NO_BOX);
      hoverBox(g, mouseX, mouseY, FShopTextures.YES_BOX);
      hoverBox(g, mouseX, mouseY, FShopTextures.SET_STACK_BOX);

      // item being priced, centred in the recessed frame (clickable = change the
      // per-sale amount); its count shows the current bundle when > 1
      boolean itemHov = inBox(mouseX, mouseY, FShopTextures.ITEM_FRAME);
      if (itemHov) {
         g.fill(left + FShopTextures.ITEM_FRAME[0], top + FShopTextures.ITEM_FRAME[1],
               left + FShopTextures.ITEM_FRAME[2], top + FShopTextures.ITEM_FRAME[3], 0x6682CD47);
      }
      if (!itemStack.isEmpty()) {
         g.renderFakeItem(itemStack, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8);
         if (bundle() > 1) {
            FShopTheme.drawCount(g, this.font, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8,
                  Integer.toString(bundle()));
         }
      }

      // price readout: centred number tinted with the currency colour (bronze =
      // orange, silver = silver, gold = gold) so the price type is obvious
      g.drawCenteredString(this.font, CoinEconomy.coinColorCode(coin) + price(),
            left + FShopTextures.ITEM_CX, top + 110, CoinEconomy.coinColor(coin));

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
      if (itemHov) {
         List<Component> t = new ArrayList<>();
         t.add(itemStack.getHoverName());
         t.add(Component.translatable("fshop.gui.price.bundle_current", bundle()));
         t.add(Component.translatable("fshop.gui.price.bundle_change"));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      } else if (inBox(mouseX, mouseY, FShopTextures.SET_STACK_BOX)) {
         tip(g, mouseX, mouseY, Component.translatable("fshop.gui.price.set64"));
      } else if (coinHov >= 0) {
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
               heldMinus = i;
               heldPlus = -1;
               holdTicks = 0;
               Sfx.step();
               return true;
            }
            if (inBox(mx, my, FShopTextures.PLUS_CELLS[i])) {
               setPrice(price() + STEPS[i]);
               heldPlus = i;
               heldMinus = -1;
               holdTicks = 0;
               Sfx.step();
               return true;
            }
         }
         if (inBox(mx, my, FShopTextures.ITEM_FRAME)) {
            openAmount();
            return true;
         }
         if (inBox(mx, my, FShopTextures.SET_STACK_BOX)) {
            setPrice(64);
            Sfx.click();
            return true;
         }
         for (int c = 0; c < 3; c++) {
            if (FShopTheme.inside(mx, my, coinCellX(c), coinCellY(), FShopTextures.CELL, FShopTextures.CELL)) {
               coin = c;
               Sfx.click();
               return true;
            }
         }
         if (inBox(mx, my, FShopTextures.NO_BOX)) {
            Sfx.click();
            PacketHandler.sendToServer(new RequestManagePacket(shop.getId()));
            return true;
         }
         if (inBox(mx, my, FShopTextures.YES_BOX)) {
            Sfx.success();
            confirm();
            return true;
         }
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
         return;
      }
      int t = this.holdTicks - 6;
      int interval = t > 40 ? 1 : 2;
      if (this.holdTicks % interval != 0) {
         return;
      }
      int mult = t > 70 ? 16 : (t > 40 ? 4 : 1);
      long delta = (long) STEPS[held] * mult;
      long before = price();
      setPrice(this.heldMinus >= 0 ? before - delta : before + delta);
      if (price() != before) {
         Sfx.step();
      }
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
