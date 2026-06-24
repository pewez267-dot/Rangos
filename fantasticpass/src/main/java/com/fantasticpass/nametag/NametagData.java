package com.fantasticpass.nametag;

import com.fantasticpass.data.NametagStyle;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Immutable snapshot of everything a client needs to render a player's extra
 * nametag line. Synced from server to clients via the nametag update packet.
 *
 * <ul>
 *   <li>{@code level} – the player's current tier in the active pass.</li>
 *   <li>{@code hasLine} – whether any extra line should be drawn at all.</li>
 *   <li>{@code usePassStyle} – when true, render {@code text} with {@code style};
 *       when false, render {@code legacyString} (e.g. a Fantastic Ranks rank that
 *       already contains legacy formatting codes).</li>
 * </ul>
 */
public final class NametagData {

    private final int level;
    private final boolean hasLine;
    private final boolean usePassStyle;
    private final String text;
    private final NametagStyle style;
    private final String legacyString;

    public NametagData(int level, boolean hasLine, boolean usePassStyle,
                       String text, NametagStyle style, String legacyString) {
        this.level = level;
        this.hasLine = hasLine;
        this.usePassStyle = usePassStyle;
        this.text = text == null ? "" : text;
        this.style = style == null ? new NametagStyle() : style;
        this.legacyString = legacyString == null ? "" : legacyString;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasLine() {
        return hasLine;
    }

    public boolean usePassStyle() {
        return usePassStyle;
    }

    public String getText() {
        return text;
    }

    public NametagStyle getStyle() {
        return style;
    }

    public String getLegacyString() {
        return legacyString;
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeBoolean(hasLine);
        buf.writeBoolean(usePassStyle);
        buf.writeUtf(text);
        style.toBuf(buf);
        buf.writeUtf(legacyString);
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
