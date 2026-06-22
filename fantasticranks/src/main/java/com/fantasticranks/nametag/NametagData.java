package com.fantasticranks.nametag;

import com.fantasticranks.data.NametagStyle;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Immutable snapshot of everything a client needs to render a player's rank line. Synced
 * from server to clients via the nametag update packet.
 */
public final class NametagData {

    private final int level;
    private final boolean hasLine;
    private final String text;
    private final NametagStyle style;

    public NametagData(int level, boolean hasLine, String text, NametagStyle style) {
        this.level = level;
        this.hasLine = hasLine;
        this.text = text == null ? "" : text;
        this.style = style == null ? new NametagStyle() : style;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasLine() {
        return hasLine;
    }

    public String getText() {
        return text;
    }

    public NametagStyle getStyle() {
        return style;
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeBoolean(hasLine);
        buf.writeUtf(text);
        style.toBuf(buf);
    }

    public static NametagData fromBuf(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        boolean hasLine = buf.readBoolean();
        String text = buf.readUtf();
        NametagStyle style = NametagStyle.fromBuf(buf);
        return new NametagData(level, hasLine, text, style);
    }
}
