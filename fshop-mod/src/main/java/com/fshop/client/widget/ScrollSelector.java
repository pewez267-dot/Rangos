package com.fshop.client.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Scrollable, searchable list with an item icon per row (ported/adapted from the
 * Fantastic Crates editor). Used by the main-shop creator to browse the whole
 * item registry and the current offers.
 */
public class ScrollSelector<T> extends AbstractWidget {
   private final List<T> all = new ArrayList<>();
   private final List<T> filtered = new ArrayList<>();
   private final Function<T, String> displayName;
   private final Function<T, String> filterText;
   private final Function<T, ItemStack> icon;
   private Consumer<T> onSelect = t -> {
   };
   private final int rowHeight;
   private int scroll;
   private int selectedIndex = -1;
   private String query = "";

   public ScrollSelector(int x, int y, int width, int height, int rowHeight,
         Function<T, String> displayName, Function<T, String> filterText, Function<T, ItemStack> icon) {
      super(x, y, width, height, Component.empty());
      this.rowHeight = rowHeight;
      this.displayName = displayName;
      this.filterText = filterText;
      this.icon = icon;
   }

   public ScrollSelector<T> onSelect(Consumer<T> cb) {
      this.onSelect = cb;
      return this;
   }

   public void setItems(List<T> items) {
      this.all.clear();
      this.all.addAll(items);
      applyFilter();
   }

   public void setQuery(String q) {
      this.query = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
      applyFilter();
   }

   private void applyFilter() {
      this.filtered.clear();
      if (this.query.isEmpty()) {
         this.filtered.addAll(this.all);
      } else {
         for (T t : this.all) {
            if (this.filterText.apply(t).toLowerCase(Locale.ROOT).contains(this.query)) {
               this.filtered.add(t);
            }
         }
      }
      this.scroll = 0;
      this.selectedIndex = -1;
   }

   private int visibleRows() {
      return Math.max(1, this.height / this.rowHeight);
   }

   private int maxScroll() {
      return Math.max(0, this.filtered.size() - visibleRows());
   }

   @Override
   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
      g.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xC01A1A1A);
      g.fill(getX(), getY(), getX() + this.width, getY() + 1, 0xFF3A3A3A);
      g.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, 0xFF3A3A3A);
      Font font = Minecraft.getInstance().font;
      int rows = visibleRows();
      for (int i = 0; i < rows; i++) {
         int index = this.scroll + i;
         if (index < 0 || index >= this.filtered.size()) {
            break;
         }
         T entry = this.filtered.get(index);
         int rowY = getY() + i * this.rowHeight;
         boolean hovered = mouseX >= getX() && mouseX < getX() + this.width - 6 && mouseY >= rowY && mouseY < rowY + this.rowHeight;
         if (index == this.selectedIndex) {
            g.fill(getX(), rowY, getX() + this.width - 6, rowY + this.rowHeight, 0xFF2E6BBF);
         } else if (hovered) {
            g.fill(getX(), rowY, getX() + this.width - 6, rowY + this.rowHeight, 0x40FFFFFF);
         }
         int textX = getX() + 3;
         if (this.icon != null) {
            ItemStack stack = this.icon.apply(entry);
            if (stack != null && !stack.isEmpty()) {
               g.renderFakeItem(stack, getX() + 1, rowY + (this.rowHeight - 16) / 2);
            }
            textX = getX() + 20;
         }
         String name = this.displayName.apply(entry);
         String trimmed = font.plainSubstrByWidth(name, this.width - (textX - getX()) - 8);
         g.drawString(font, trimmed, textX, rowY + (this.rowHeight - 8) / 2, 0xE0E0E0, false);
      }
      if (maxScroll() > 0) {
         int barX = getX() + this.width - 5;
         g.fill(barX, getY(), barX + 4, getY() + this.height, 0x60000000);
         int trackH = this.height;
         int thumbH = Math.max(10, trackH * visibleRows() / Math.max(1, this.filtered.size()));
         int thumbY = getY() + (trackH - thumbH) * this.scroll / Math.max(1, maxScroll());
         g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF808080);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!isMouseOver(mouseX, mouseY) || button != 0) {
         return false;
      }
      int row = (int) ((mouseY - getY()) / this.rowHeight);
      int index = this.scroll + row;
      if (index >= 0 && index < this.filtered.size() && mouseX < getX() + this.width - 6) {
         this.selectedIndex = index;
         this.onSelect.accept(this.filtered.get(index));
         return true;
      }
      return false;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (!isMouseOver(mouseX, mouseY)) {
         return false;
      }
      this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.signum(delta)));
      return true;
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return mouseX >= getX() && mouseX < getX() + this.width && mouseY >= getY() && mouseY < getY() + this.height;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput out) {
   }
}
