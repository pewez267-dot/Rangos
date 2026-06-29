package com.fantasticpass.nametag;

import com.fantasticpass.data.NametagStyle;
import net.minecraft.network.FriendlyByteBuf;

public final class NametagData {
   private final int level;
   private final boolean hasLine;
   private final boolean usePassStyle;
   private final String text;
   private final NametagStyle style;
   private final String legacyString;

   public NametagData(int level, boolean hasLine, boolean usePassStyle, String text, NametagStyle style, String legacyString) {
      this.level = level;
      this.hasLine = hasLine;
      this.usePassStyle = usePassStyle;
      this.text = text == null ? "" : text;
      this.style = style == null ? new NametagStyle() : style;
      this.legacyString = legacyString == null ? "" : legacyString;
   }

   public int getLevel() {
      return this.level;
   }

   public boolean hasLine() {
      return this.hasLine;
   }

   public boolean usePassStyle() {
      return this.usePassStyle;
   }

   public String getText() {
      return this.text;
   }

   public NametagStyle getStyle() {
      return this.style;
   }

   public String getLegacyString() {
      return this.legacyString;
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeVarInt(this.level);
      buf.writeBoolean(this.hasLine);
      buf.writeBoolean(this.usePassStyle);
      buf.writeUtf(this.text);
      this.style.toBuf(buf);
      buf.writeUtf(this.legacyString);
   }

   public static NametagData fromBuf(FriendlyByteBuf buf) {
      int level = buf.readVarInt();
      boolean hasLine = buf.readBoolean();
      boolean usePassStyle = buf.readBoolean();
      String text = buf.readUtf();
      NametagStyle style = NametagStyle.fromBuf(buf);
      String legacyString = buf.readUtf();
      return new NametagData(level, hasLine, usePassStyle, text, style, legacyString);
   }
}
