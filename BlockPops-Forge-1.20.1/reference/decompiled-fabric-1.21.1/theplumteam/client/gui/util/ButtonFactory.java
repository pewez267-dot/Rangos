package com.theplumteam.client.gui.util;

import com.theplumteam.client.gui.widget.TabButton;
import net.minecraft.class_2561;
import net.minecraft.class_4185;
import net.minecraft.class_4185.class_4241;

public class ButtonFactory {
   public static class_4185 createTab(int x, int y, int width, int height, class_2561 label, boolean selected, class_4241 onPress) {
      return new TabButton(x, y, width, height, label, selected, onPress);
   }
}
