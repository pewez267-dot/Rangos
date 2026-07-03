package com.fshop.client.screen;

import com.fshop.client.FShopTextures;
import com.fshop.client.FShopTheme;
import com.fshop.network.PacketHandler;
import com.fshop.network.RequestManagePacket;
import com.fshop.shop.PlayerShop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Pick an item from your inventory to put on sale in your shop. */
public final class InventoryPickerScreen extends Screen {
   private static final int FOOTER = 42;
   private final PlayerShop shop;
   private int left;
   private int top;

   public InventoryPickerScreen(PlayerShop shop) {
      super(Component.translatable("fshop.gui.picker.title"));
      this.shop = shop;
   }

   @Override
   protected void init() {
      this.left = (this.width - FShopTextures.GW) / 2;
      this.top = (this.height - (FShopTextures.GH + FOOTER)) / 2;
   }

   private int invSlot(int row, int col) {
      return row < 3 ? 9 + row * 9 + col : col;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      FShopTextures.blitPanel(g, FShopTextures.STACK, left, top);

      var inv = this.minecraft.player.getInventory();
      int hoveredSlot = -1;
      for (int row = 0; row < FShopTextures.GRID_ROWS; row++) {
         for (int col = 0; col < FShopTextures.GRID_COLS; col++) {
            int cx = left + FShopTextures.cellX(col);
            int cy = top + FShopTextures.cellY(row);
            boolean hov = FShopTheme.inside(mouseX, mouseY, cx, cy, FShopTextures.CELL_W, FShopTextures.CELL_H);
            ItemStack st = inv.getItem(invSlot(row, col));
            if (hov && !st.isEmpty()) {
               g.fill(cx, cy, cx + FShopTextures.CELL_W, cy + FShopTextures.CELL_H, 0x6682CD47);
               hoveredSlot = invSlot(row, col);
            }
            if (!st.isEmpty()) {
               g.renderFakeItem(st, left + FShopTextures.itemX(col), top + FShopTextures.itemY(row));
               g.renderItemDecorations(this.font, st, left + FShopTextures.itemX(col), top + FShopTextures.itemY(row));
            }
         }
      }

      int fy = top + FShopTextures.GH;
      int w = FShopTextures.GW;
      g.fill(left, fy, left + w, fy + FOOTER, FShopTheme.HEADER);
      g.fill(left, fy, left + w, fy + 1, FShopTheme.BORDER);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.picker.title"),
            left + w / 2, fy + 6, FShopTheme.GOLD);
      boolean backHov = FShopTheme.inside(mouseX, mouseY, left + 8, fy + 22, 60, 16);
      FShopTheme.button(g, left + 8, fy + 22, 60, 16, FShopTheme.SELL, backHov);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.back"), left + 38, fy + 26, FShopTheme.TEXT);
      g.drawCenteredString(this.font, Component.translatable("fshop.gui.picker.hint"),
            left + w / 2 + 30, fy + 26, FShopTheme.TEXT_DIM);

      super.render(g, mouseX, mouseY, partial);
      if (hoveredSlot >= 0) {
         List<Component> t = new ArrayList<>();
         t.add(inv.getItem(hoveredSlot).getHoverName());
         t.add(Component.translatable("fshop.gui.click_to_stock").withStyle(ChatFormatting.GREEN));
         g.renderComponentTooltip(this.font, t, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (int row = 0; row < FShopTextures.GRID_ROWS; row++) {
            for (int col = 0; col < FShopTextures.GRID_COLS; col++) {
               int cx = left + FShopTextures.cellX(col);
               int cy = top + FShopTextures.cellY(row);
               if (FShopTheme.inside(mx, my, cx, cy, FShopTextures.CELL_W, FShopTextures.CELL_H)) {
                  int slot = invSlot(row, col);
                  if (!this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                     this.minecraft.setScreen(new PriceInputScreen(shop, PriceInputScreen.Mode.ADD, slot, 1));
                     return true;
                  }
               }
            }
         }
         if (FShopTheme.inside(mx, my, left + 8, top + FShopTextures.GH + 22, 60, 16)) {
            PacketHandler.sendToServer(new RequestManagePacket(shop.getId()));
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
