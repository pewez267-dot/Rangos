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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Price / currency / bundle editor built on the real shop_gui_confirmation.png
 * texture (no hand-drawn panel): the item being priced sits in the centre
 * frame, the red bar lowers the price (-1/-10/-100) and the green bar raises
 * it (+1/+10/+100), the three coins on the gray grid pick the currency, and
 * NO/YES cancel or save. The price can also be typed on the keyboard.
 *
 * <p>To the right of the storefront (same docked style as the search box on
 * the main shop) sits the "Cantidad por venta" panel: a short explanation of
 * what the field does, a text box to type any custom bundle size, and two
 * clear buttons - "Vender por unidad" (1) and "Vender el stack completo"
 * (the item's max stack size) - matching the flexibility the admin creator
 * gives for the server shop, but worded so it is obvious at a glance.
 */
public final class PriceInputScreen extends Screen {
   private static final long CAP = 999_999_999L;
   private static final int[] STEPS = {1, 10, 100};
   private static final int BUNDLE_W = 84;
   private static final int BUNDLE_PANEL_TOP = 78;
   private static final int BUNDLE_HINT_Y = 11;

   public enum Mode {
      ADD, EDIT
   }

   private final PlayerShop shop;
   private final Mode mode;
   private final int ref;
   private int coin;
   private ItemStack itemStack = ItemStack.EMPTY;
   private String buf;
   private String bundleBuf;
   private int left;
   private int top;
   private int heldMinus = -1;
   private int heldPlus = -1;
   private int holdTicks;
   private EditBox bundleBox;
   private int bundleLabelH;
   private int bundlePanelX;
   private int bundlePanelY;
   private int bundlePanelW;
   private int bundlePanelH;

   public PriceInputScreen(PlayerShop shop, Mode mode, int ref, long initial, int coin, int bundle) {
      super(Component.translatable(mode == Mode.ADD ? "fshop.gui.price.add" : "fshop.gui.price.edit"));
      this.shop = shop;
      this.mode = mode;
      this.ref = ref;
      this.coin = coin;
      this.buf = Long.toString(Math.max(1L, initial));
      this.bundleBuf = Integer.toString(Math.max(1, bundle));
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - FShopTextures.GH) / 2;
      if (mode == Mode.ADD) {
         // Display-only copy pinned to count=1: the real inventory stack can
         // have any count (e.g. 14), and Minecraft always paints that number
         // as a vanilla decoration on the icon. Left uncapped, that raw count
         // rendered on top of our own price/bundle readout below the item,
         // producing the large misplaced number seen in-game. Pinning it to 1
         // removes the vanilla decoration entirely (our own bundle count via
         // FShopTheme.drawCount below still shows when bundle() > 1).
         this.itemStack = this.minecraft.player.getInventory().getItem(ref).copy();
         this.itemStack.setCount(1);
      } else if (ref >= 0 && ref < shop.getOffers().size()) {
         this.itemStack = shop.getOffers().get(ref).displayStack(1);
      }

      // "Cantidad por venta" side panel, docked right next to the storefront,
      // mirroring the search box style used on the main shop's buy screen.
      // Layout top to bottom: title + explanatory text (rendered by
      // renderBundlePanel), the custom-amount text box, then the two clear
      // action buttons - "Vender por unidad" and "Vender el stack completo".
      // All positions are computed once here and reused by renderBundlePanel
      // so the panel background always matches the widgets exactly.
      int px = left + FShopTextures.GW + 6;
      int py = top + BUNDLE_PANEL_TOP;
      this.bundleLabelH = this.font.wordWrapHeight(
            net.minecraft.client.resources.language.I18n.get("fshop.gui.price.bundle_hint"), BUNDLE_W + 2);
      int fieldY = py + BUNDLE_HINT_Y + this.bundleLabelH + 5;

      this.bundlePanelX = px - 4;
      this.bundlePanelY = py - 4;
      this.bundlePanelW = BUNDLE_W + 12;
      this.bundlePanelH = (fieldY + 36 + 16 + 6) - this.bundlePanelY;

      this.bundleBox = new EditBox(this.font, px, fieldY, BUNDLE_W, 14,
            Component.translatable("fshop.gui.price.bundle_field"));
      this.bundleBox.setMaxLength(5);
      this.bundleBox.setValue(this.bundleBuf);
      this.bundleBox.setBordered(true);
      this.bundleBox.setTextColor(0xFFF5E6C8);
      this.bundleBox.setResponder(s -> this.bundleBuf = s);
      this.bundleBox.setTooltip(Tooltip.create(Component.translatable("fshop.gui.price.bundle_tip")));
      addRenderableWidget(this.bundleBox);

      addRenderableWidget(Button.builder(Component.translatable("fshop.gui.price.bundle_one"), b -> {
         this.bundleBuf = "1";
         this.bundleBox.setValue("1");
         Sfx.click();
      }).tooltip(Tooltip.create(Component.translatable("fshop.gui.price.bundle_one_tip")))
            .bounds(px, fieldY + 18, BUNDLE_W, 16).build());
      addRenderableWidget(Button.builder(Component.translatable("fshop.gui.price.bundle_stack"), b -> {
         int max = Math.max(1, this.itemStack.getMaxStackSize());
         this.bundleBuf = Integer.toString(max);
         this.bundleBox.setValue(this.bundleBuf);
         Sfx.click();
      }).tooltip(Tooltip.create(Component.translatable("fshop.gui.price.bundle_stack_tip")))
            .bounds(px, fieldY + 36, BUNDLE_W, 16).build());
   }

   private long price() {
      try {
         return Math.max(1L, Math.min(CAP, Long.parseLong(buf)));
      } catch (NumberFormatException e) {
         return 1L;
      }
   }

   private int bundle() {
      try {
         return Math.max(1, Math.min(this.itemStack.getMaxStackSize(), Integer.parseInt(bundleBuf.trim())));
      } catch (NumberFormatException e) {
         return 1;
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
      renderBundlePanel(g);

      for (int[] box : FShopTextures.MINUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      for (int[] box : FShopTextures.PLUS_CELLS) {
         hoverBox(g, mouseX, mouseY, box);
      }
      hoverBox(g, mouseX, mouseY, FShopTextures.NO_BOX);
      hoverBox(g, mouseX, mouseY, FShopTextures.YES_BOX);
      hoverBox(g, mouseX, mouseY, FShopTextures.SET_STACK_BOX);

      // item being priced, centred in the recessed frame
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
      if (inBox(mouseX, mouseY, FShopTextures.SET_STACK_BOX)) {
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

   /**
    * Small docked panel to the right of the storefront (same visual language as
    * the main shop's search box): a title, a short plain-language explanation
    * of what the field controls, the custom-amount box and the two buttons
    * ("Vender por unidad" / "Vender el stack completo") rendered by
    * {@code init()}. This lets the seller choose to sell one at a time, by the
    * full stack, or any custom bundle size, exactly like the admin creator.
    */
   private void renderBundlePanel(GuiGraphics g) {
      int px = this.bundlePanelX;
      int py = this.bundlePanelY;
      int pw = this.bundlePanelW;
      int ph = this.bundlePanelH;
      g.fill(px, py, px + pw, py + ph, 0xB2241C14);
      g.fill(px, py, px + pw, py + 1, 0x66FFE6B0);
      g.fill(px, py + ph - 1, px + pw, py + ph, 0x66000000);
      g.fill(px, py, px + 1, py + ph, 0x66FFE6B0);
      g.fill(px + pw - 1, py, px + pw, py + ph, 0x66000000);

      // Title: what this panel is for.
      g.drawString(this.font, Component.translatable("fshop.gui.price.bundle_label"),
            px + 4, py + 3, 0xFFEBD9AE, false);

      // Plain-language explanation, word-wrapped so it never overflows the panel.
      g.drawWordWrap(this.font, net.minecraft.network.chat.FormattedText.of(
            net.minecraft.client.resources.language.I18n.get("fshop.gui.price.bundle_hint")),
            px + 4, py + BUNDLE_HINT_Y, pw - 8, 0xFFB9A98A);
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
      if (this.bundleBox != null && this.bundleBox.isFocused()) {
         return super.charTyped(c, mods);
      }
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
      if (this.bundleBox != null && this.bundleBox.isFocused()) {
         return super.keyPressed(key, scan, mods);
      }
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
