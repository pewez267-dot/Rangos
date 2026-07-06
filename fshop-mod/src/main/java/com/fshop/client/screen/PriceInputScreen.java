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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

/**
 * Price / currency / amount editor on the real shop_gui_confirmation.png
 * texture: the item sits in the centre frame, the red bar lowers the price
 * (-1/-10/-100), the green bar raises it (+1/+10/+100), the three coins pick
 * the currency and NO/YES cancel or save. The price can also be typed.
 *
 * <p>Docked to the right (same card style as the main shop's search box) is the
 * "Cantidad por venta" panel that lets the seller decide how many units each
 * payment delivers: type any custom amount, or use the "Por unidad" / "Stack
 * completo" buttons. The panel width is measured from the actual (translated)
 * text at runtime so nothing is ever cramped, clipped or overlapping.
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
   private int bundleW;
   private int bundleLabelH;
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int titleY;
   private int hintY;

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
         this.itemStack = this.minecraft.player.getInventory().getItem(ref).copy();
      } else if (ref >= 0 && ref < shop.getOffers().size()) {
         this.itemStack = shop.getOffers().get(ref).displayStack(1);
      }
      // Pin to count=1 so the raw inventory count isn't painted over our readout.
      if (!this.itemStack.isEmpty()) {
         this.itemStack.setCount(1);
      }

      // --- "Cantidad por venta" card, docked to the right of the storefront ---
      // Width is measured from the real translated strings so the title, the
      // buttons and the wrapped hint always fit with margin (fixes the old
      // cramped / overlapping panel).
      String label = I18n.get("fshop.gui.price.bundle_label");
      String oneTxt = I18n.get("fshop.gui.price.bundle_one");
      String stackTxt = I18n.get("fshop.gui.price.bundle_stack");
      String hint = I18n.get("fshop.gui.price.bundle_hint");
      int longestButton = Math.max(this.font.width(oneTxt), this.font.width(stackTxt)) + 12;
      this.bundleW = Math.max(74, Math.max(this.font.width(label), longestButton));

      int px = left + FShopTextures.GW + 6;
      int py = top + 72;
      this.titleY = py;
      this.hintY = py + 11;
      this.bundleLabelH = this.font.wordWrapHeight(hint, this.bundleW);
      int fieldY = this.hintY + this.bundleLabelH + 4;

      this.panelX = px - 5;
      this.panelY = py - 5;
      this.panelW = this.bundleW + 10;
      this.panelH = (fieldY + 18 + 18 + 16 + 5) - this.panelY;

      this.bundleBox = new EditBox(this.font, px, fieldY, this.bundleW, 14,
            Component.translatable("fshop.gui.price.bundle_field"));
      this.bundleBox.setMaxLength(5);
      this.bundleBox.setValue(this.bundleBuf);
      this.bundleBox.setBordered(true);
      this.bundleBox.setTextColor(0xFFF5E6C8);
      this.bundleBox.setHint(Component.translatable("fshop.gui.price.bundle_field"));
      this.bundleBox.setResponder(s -> this.bundleBuf = s);
      this.bundleBox.setTooltip(Tooltip.create(Component.translatable("fshop.gui.price.bundle_tip")));
      addRenderableWidget(this.bundleBox);

      addRenderableWidget(Button.builder(Component.translatable("fshop.gui.price.bundle_one"), b -> {
         this.bundleBuf = "1";
         this.bundleBox.setValue("1");
         Sfx.click();
      }).tooltip(Tooltip.create(Component.translatable("fshop.gui.price.bundle_one_tip")))
            .bounds(px, fieldY + 18, this.bundleW, 16).build());
      addRenderableWidget(Button.builder(Component.translatable("fshop.gui.price.bundle_stack"), b -> {
         int max = Math.max(1, this.itemStack.getMaxStackSize());
         this.bundleBuf = Integer.toString(max);
         this.bundleBox.setValue(this.bundleBuf);
         Sfx.click();
      }).tooltip(Tooltip.create(Component.translatable("fshop.gui.price.bundle_stack_tip")))
            .bounds(px, fieldY + 36, this.bundleW, 16).build());
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
      try {
         return Math.max(1, Math.min(max, Integer.parseInt(bundleBuf.trim())));
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
      FShopTheme.footerHint(g, this.font, this.width, this.height,
            Component.translatable("fshop.gui.price.hint"));
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

      // item being priced, centred in the recessed frame; count shows the bundle
      if (!itemStack.isEmpty()) {
         g.renderFakeItem(itemStack, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8);
         if (bundle() > 1) {
            FShopTheme.drawCount(g, this.font, left + FShopTextures.ITEM_CX - 8, top + FShopTextures.ITEM_CY - 8,
                  Integer.toString(bundle()));
         }
      }

      // price readout: centred number tinted with the currency colour
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
    * The docked "Cantidad por venta" card (same visual language as the main
    * shop's search box): a title, a short word-wrapped explanation, the custom
    * amount box and the two preset buttons (rendered by {@code init()}). All
    * dimensions come from the measured text so it always fits.
    */
   private void renderBundlePanel(GuiGraphics g) {
      g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE6241C14);
      g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0x88FFE6B0);
      g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0x88000000);
      g.fill(panelX, panelY, panelX + 1, panelY + panelH, 0x88FFE6B0);
      g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0x88000000);

      int tx = panelX + 5;
      g.drawString(this.font, Component.translatable("fshop.gui.price.bundle_label"),
            tx, titleY, 0xFFEBD9AE, false);
      g.drawWordWrap(this.font, FormattedText.of(I18n.get("fshop.gui.price.bundle_hint")),
            tx, hintY, this.bundleW, 0xFFB9A98A);
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
