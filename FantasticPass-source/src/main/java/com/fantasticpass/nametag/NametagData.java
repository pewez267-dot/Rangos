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
    // Estilo del texto "Nivel: N" (label del nivel). Editable por el admin (gratis/premium).
    private final NametagStyle levelStyle;

    /** Estilo por defecto del label de nivel: gris (conserva el aspecto original). */
    public static NametagStyle defaultLevelStyle() {
        return new NametagStyle(0xAAAAAA, false, false, false, false, false, 0xAAAAAA, 0xAAAAAA, false, 0);
    }

    public NametagData(int level, boolean hasLine, boolean usePassStyle, String text, NametagStyle style, String legacyString) {
        this(level, hasLine, usePassStyle, text, style, legacyString, null);
    }

    public NametagData(int level, boolean hasLine, boolean usePassStyle, String text, NametagStyle style, String legacyString, NametagStyle levelStyle) {
        this.level = level;
        this.hasLine = hasLine;
        this.usePassStyle = usePassStyle;
        this.text = text == null ? "" : text;
        this.style = style == null ? new NametagStyle() : style;
        this.legacyString = legacyString == null ? "" : legacyString;
        this.levelStyle = levelStyle == null ? defaultLevelStyle() : levelStyle;
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

    public NametagStyle getLevelStyle() {
        return this.levelStyle;
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(this.level);
        buf.writeBoolean(this.hasLine);
        buf.writeBoolean(this.usePassStyle);
        buf.writeUtf(this.text);
        this.style.toBuf(buf);
        buf.writeUtf(this.legacyString);
        this.levelStyle.toBuf(buf);
    }

    public static NametagData fromBuf(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        boolean hasLine = buf.readBoolean();
        boolean usePassStyle = buf.readBoolean();
        String text = buf.readUtf();
        NametagStyle style = NametagStyle.fromBuf(buf);
        String legacyString = buf.readUtf();
        NametagStyle levelStyle = NametagStyle.fromBuf(buf);
        return new NametagData(level, hasLine, usePassStyle, text, style, legacyString, levelStyle);
    }
}
