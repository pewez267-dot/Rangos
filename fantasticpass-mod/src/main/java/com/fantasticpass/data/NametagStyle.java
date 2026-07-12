package com.fantasticpass.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class NametagStyle {
   private int color;
   private boolean bold;
   private boolean italic;
   private boolean underline;
   private boolean strikethrough;
   private boolean gradient;
   private int gradientStart;
   private int gradientEnd;

   public NametagStyle() {
      this.color = 58879;
      this.gradientStart = 58879;
      this.gradientEnd = 16766720;
   }

   public NametagStyle(int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean gradient, int gradientStart, int gradientEnd) {
      this.color = color & 16777215;
      this.bold = bold;
      this.italic = italic;
      this.underline = underline;
      this.strikethrough = strikethrough;
      this.gradient = gradient;
      this.gradientStart = gradientStart & 16777215;
      this.gradientEnd = gradientEnd & 16777215;
   }

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = color & 16777215;
   }

   public boolean isBold() {
      return this.bold;
   }

   public void setBold(boolean bold) {
      this.bold = bold;
   }

   public boolean isItalic() {
      return this.italic;
   }

   public void setItalic(boolean italic) {
      this.italic = italic;
   }

   public boolean isUnderline() {
      return this.underline;
   }

   public void setUnderline(boolean underline) {
      this.underline = underline;
   }

   public boolean isStrikethrough() {
      return this.strikethrough;
   }

   public void setStrikethrough(boolean strikethrough) {
      this.strikethrough = strikethrough;
   }

   public boolean isGradient() {
      return this.gradient;
   }

   public void setGradient(boolean gradient) {
      this.gradient = gradient;
   }

   public int getGradientStart() {
      return this.gradientStart;
   }

   public void setGradientStart(int gradientStart) {
      this.gradientStart = gradientStart & 16777215;
   }

   public int getGradientEnd() {
      return this.gradientEnd;
   }

   public void setGradientEnd(int gradientEnd) {
      this.gradientEnd = gradientEnd & 16777215;
   }

   public NametagStyle copy() {
      return new NametagStyle(this.color, this.bold, this.italic, this.underline, this.strikethrough, this.gradient, this.gradientStart, this.gradientEnd);
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("color", this.color);
      tag.putBoolean("bold", this.bold);
      tag.putBoolean("italic", this.italic);
      tag.putBoolean("underline", this.underline);
      tag.putBoolean("strikethrough", this.strikethrough);
      tag.putBoolean("gradient", this.gradient);
      tag.putInt("gradientStart", this.gradientStart);
      tag.putInt("gradientEnd", this.gradientEnd);
      return tag;
   }

   public static NametagStyle fromNbt(CompoundTag tag) {
      NametagStyle style = new NametagStyle();
      style.color = tag.getInt("color") & 16777215;
      style.bold = tag.getBoolean("bold");
      style.italic = tag.getBoolean("italic");
      style.underline = tag.getBoolean("underline");
      style.strikethrough = tag.getBoolean("strikethrough");
      style.gradient = tag.getBoolean("gradient");
      style.gradientStart = tag.getInt("gradientStart") & 16777215;
      style.gradientEnd = tag.getInt("gradientEnd") & 16777215;
      return style;
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeInt(this.color);
      buf.writeBoolean(this.bold);
      buf.writeBoolean(this.italic);
      buf.writeBoolean(this.underline);
      buf.writeBoolean(this.strikethrough);
      buf.writeBoolean(this.gradient);
      buf.writeInt(this.gradientStart);
      buf.writeInt(this.gradientEnd);
   }

   public static NametagStyle fromBuf(FriendlyByteBuf buf) {
      NametagStyle style = new NametagStyle();
      style.color = buf.readInt() & 16777215;
      style.bold = buf.readBoolean();
      style.italic = buf.readBoolean();
      style.underline = buf.readBoolean();
      style.strikethrough = buf.readBoolean();
      style.gradient = buf.readBoolean();
      style.gradientStart = buf.readInt() & 16777215;
      style.gradientEnd = buf.readInt() & 16777215;
      return style;
   }
}
