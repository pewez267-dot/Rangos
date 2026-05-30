package com.theplumteam.block;

import net.minecraft.class_3542;
import net.minecraft.class_3620;

public enum PopBlockColor implements class_3542 {
   ORIGINAL("original", class_3620.field_16003),
   BLACK("black", class_3620.field_16009),
   BLUE("blue", class_3620.field_15984),
   BROWN("brown", class_3620.field_15977),
   CYAN("cyan", class_3620.field_16026),
   GRAY("gray", class_3620.field_15978),
   GREEN("green", class_3620.field_15995),
   LIGHT_BLUE("light_blue", class_3620.field_16024),
   LIGHT_GRAY("light_gray", class_3620.field_15993),
   LIME("lime", class_3620.field_15997),
   MAGENTA("magenta", class_3620.field_15998),
   ORANGE("orange", class_3620.field_15987),
   PINK("pink", class_3620.field_16030),
   PURPLE("purple", class_3620.field_16014),
   RED("red", class_3620.field_16020),
   YELLOW("yellow", class_3620.field_16010);

   private final String name;
   private final class_3620 mapColor;

   private PopBlockColor(String name, class_3620 mapColor) {
      this.name = name;
      this.mapColor = mapColor;
   }

   public String method_15434() {
      return this.name;
   }

   public class_3620 getMapColor() {
      return this.mapColor;
   }

   public String getTextureName() {
      return this.name;
   }
}
