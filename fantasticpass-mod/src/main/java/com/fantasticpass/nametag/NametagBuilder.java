package com.fantasticpass.nametag;

import com.fantasticpass.data.NametagStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class NametagBuilder {
   private NametagBuilder() {
   }

   public static Component buildLine(NametagData data) {
      MutableComponent line = Component.translatable("fantasticpass.nametag.level", data.getLevel())
         .append(Component.literal(" "))
         .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(11184810)));
      if (data.usePassStyle()) {
         line.append(buildStyledText(data.getText(), data.getStyle()));
      } else {
         line.append(Component.literal(data.getLegacyString()));
      }

      return line;
   }

   public static MutableComponent buildStyledText(String text, NametagStyle style) {
      if (text == null) {
         text = "";
      }

      Style baseFormat = Style.EMPTY
         .withBold(style.isBold())
         .withItalic(style.isItalic())
         .withUnderlined(style.isUnderline())
         .withStrikethrough(style.isStrikethrough());
      if (!style.isGradient()) {
         return Component.literal(text).withStyle(baseFormat.withColor(TextColor.fromRgb(style.getColor())));
      } else {
         MutableComponent result = Component.empty();
         int length = text.length();

         for (int i = 0; i < length; i++) {
            float t = length <= 1 ? 0.0F : (float)i / (float)(length - 1);
            int color = lerpColor(style.getGradientStart(), style.getGradientEnd(), t);
            result.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(baseFormat.withColor(TextColor.fromRgb(color))));
         }

         return result;
      }
   }

   public static int lerpColor(int start, int end, float t) {
      if (t <= 0.0F) {
         return start & 16777215;
      } else if (t >= 1.0F) {
         return end & 16777215;
      } else {
         int sr = start >> 16 & 0xFF;
         int sg = start >> 8 & 0xFF;
         int sb = start & 0xFF;
         int er = end >> 16 & 0xFF;
         int eg = end >> 8 & 0xFF;
         int eb = end & 0xFF;
         int r = Math.round((float)sr + (float)(er - sr) * t);
         int g = Math.round((float)sg + (float)(eg - sg) * t);
         int b = Math.round((float)sb + (float)(eb - sb) * t);
         return r << 16 | g << 8 | b;
      }
   }

   public static MutableComponent accent(String text, ChatFormatting color) {
      return Component.literal(text).withStyle(color);
   }
}
