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
    private boolean rainbow;
    private int rainbowStyle;

    public NametagStyle() {
        this.color = 58879;
        this.gradientStart = 58879;
        this.gradientEnd = 16766720;
        this.rainbow = false;
        this.rainbowStyle = 0;
    }

    public NametagStyle(int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean gradient, int gradientStart, int gradientEnd) {
        this.color = color & 0xFFFFFF;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.gradient = gradient;
        this.gradientStart = gradientStart & 0xFFFFFF;
        this.gradientEnd = gradientEnd & 0xFFFFFF;
    }

    public NametagStyle(int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean gradient, int gradientStart, int gradientEnd, boolean rainbow, int rainbowStyle) {
        this(color, bold, italic, underline, strikethrough, gradient, gradientStart, gradientEnd);
        this.rainbow = rainbow;
        this.rainbowStyle = rainbowStyle;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
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
        this.gradientStart = gradientStart & 0xFFFFFF;
    }

    public int getGradientEnd() {
        return this.gradientEnd;
    }

    public void setGradientEnd(int gradientEnd) {
        this.gradientEnd = gradientEnd & 0xFFFFFF;
    }

    public boolean isRainbow() {
        return this.rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public int getRainbowStyle() {
        return this.rainbowStyle;
    }

    public void setRainbowStyle(int rainbowStyle) {
        this.rainbowStyle = rainbowStyle;
    }

    public NametagStyle copy() {
        return new NametagStyle(this.color, this.bold, this.italic, this.underline, this.strikethrough, this.gradient, this.gradientStart, this.gradientEnd, this.rainbow, this.rainbowStyle);
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
        tag.putBoolean("rainbow", this.rainbow);
        tag.putInt("rainbowStyle", this.rainbowStyle);
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
        style.rainbow = tag.getBoolean("rainbow");
        style.rainbowStyle = tag.getInt("rainbowStyle");
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
        buf.writeBoolean(this.rainbow);
        buf.writeInt(this.rainbowStyle);
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
        style.rainbow = buf.readBoolean();
        style.rainbowStyle = buf.readInt();
        return style;
    }
}
