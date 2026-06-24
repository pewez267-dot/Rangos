package com.fantasticranks.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Complete visual configuration of a rank nametag line. Pure data with NBT and network
 * serialization; usable on both logical sides. Component construction lives in
 * {@code com.fantasticranks.nametag.NametagBuilder}.
 */
public final class NametagStyle {

    /** Solid color (used when {@link #gradient} is false), packed 0xRRGGBB. */
    private int color;

    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;

    /** When true, the text is colored with a per-character interpolation. */
    private boolean gradient;
    private int gradientStart;
    private int gradientEnd;

    public NametagStyle() {
        this.color = 0x00E5FF;
        this.gradientStart = 0x00E5FF;
        this.gradientEnd = 0xFFD700;
    }

    public NametagStyle(int color, boolean bold, boolean italic, boolean underline,
                        boolean strikethrough, boolean gradient, int gradientStart, int gradientEnd) {
        this.color = color & 0xFFFFFF;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.gradient = gradient;
        this.gradientStart = gradientStart & 0xFFFFFF;
        this.gradientEnd = gradientEnd & 0xFFFFFF;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public boolean isGradient() {
        return gradient;
    }

    public void setGradient(boolean gradient) {
        this.gradient = gradient;
    }

    public int getGradientStart() {
        return gradientStart;
    }

    public void setGradientStart(int gradientStart) {
        this.gradientStart = gradientStart & 0xFFFFFF;
    }

    public int getGradientEnd() {
        return gradientEnd;
    }

    public void setGradientEnd(int gradientEnd) {
        this.gradientEnd = gradientEnd & 0xFFFFFF;
    }

    public NametagStyle copy() {
        return new NametagStyle(color, bold, italic, underline, strikethrough,
                gradient, gradientStart, gradientEnd);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("color", color);
        tag.putBoolean("bold", bold);
        tag.putBoolean("italic", italic);
        tag.putBoolean("underline", underline);
        tag.putBoolean("strikethrough", strikethrough);
        tag.putBoolean("gradient", gradient);
        tag.putInt("gradientStart", gradientStart);
        tag.putInt("gradientEnd", gradientEnd);
        return tag;
    }

    public static NametagStyle fromNbt(CompoundTag tag) {
        NametagStyle style = new NametagStyle();
        style.color = tag.getInt("color") & 0xFFFFFF;
        style.bold = tag.getBoolean("bold");
        style.italic = tag.getBoolean("italic");
        style.underline = tag.getBoolean("underline");
        style.strikethrough = tag.getBoolean("strikethrough");
        style.gradient = tag.getBoolean("gradient");
        style.gradientStart = tag.getInt("gradientStart") & 0xFFFFFF;
        style.gradientEnd = tag.getInt("gradientEnd") & 0xFFFFFF;
        return style;
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeBoolean(bold);
        buf.writeBoolean(italic);
        buf.writeBoolean(underline);
        buf.writeBoolean(strikethrough);
        buf.writeBoolean(gradient);
        buf.writeInt(gradientStart);
        buf.writeInt(gradientEnd);
    }

    public static NametagStyle fromBuf(FriendlyByteBuf buf) {
        NametagStyle style = new NametagStyle();
        style.color = buf.readInt() & 0xFFFFFF;
        style.bold = buf.readBoolean();
        style.italic = buf.readBoolean();
        style.underline = buf.readBoolean();
        style.strikethrough = buf.readBoolean();
        style.gradient = buf.readBoolean();
        style.gradientStart = buf.readInt() & 0xFFFFFF;
        style.gradientEnd = buf.readInt() & 0xFFFFFF;
        return style;
    }
}
