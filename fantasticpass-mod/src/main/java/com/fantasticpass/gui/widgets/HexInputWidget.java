package com.fantasticpass.gui.widgets;

import com.fantasticpass.data.PassSerializer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class HexInputWidget extends EditBox {
   private final IntConsumer onColorChanged;
   private boolean suppress;

   public HexInputWidget(Font font, int x, int y, int width, int height, IntConsumer onColorChanged) {
      super(font, x, y, width, height, Component.literal("HEX"));
      this.onColorChanged = onColorChanged;
      this.setMaxLength(7);
      this.setFilter(s -> s.matches("#?[0-9a-fA-F]{0,6}"));
      this.setResponder(this::onChanged);
   }

   private void onChanged(String value) {
      if (!this.suppress) {
         int parsed = PassSerializer.parseHex(value, Integer.MIN_VALUE);
         if (parsed != Integer.MIN_VALUE && this.onColorChanged != null) {
            this.onColorChanged.accept(parsed);
         }
      }
   }

   public void setColorSilently(int rgb) {
      this.suppress = true;
      this.setValue(PassSerializer.toHex(rgb));
      this.suppress = false;
   }
}
