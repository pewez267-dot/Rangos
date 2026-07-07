/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  net.minecraft.network.FriendlyByteBuf
 */
package com.fsholo.data;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

public class HoloLine {
    public String text = "Texto";
    public String color = "#FFFFFF";
    public boolean bold = false;
    public boolean italic = false;
    public boolean underline = false;
    public boolean strikethrough = false;
    public boolean obfuscated = false;
    public boolean shadow = true;
    public boolean gradient = false;
    public String gradFrom = "#FF5555";
    public String gradTo = "#55AAFF";
    public boolean rainbow = false;
    public int rainbowStyle = 0;
    public boolean particles = false;
    public int particleStyle = 0;
    public int particleMovement = 0;
    public int particleAnchor = 0;
    public int particleDensity = 1;
    public int particleSpeed = 1;
    public int particleSize = 1;
    public int particleSpread = 1;
    public float particleOffX = 0.0f;
    public float particleOffY = 0.0f;
    public int particleRate = 1;

    public HoloLine() {
    }

    public HoloLine(String text) {
        this.text = text;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("text", this.text);
        o.addProperty("color", this.color);
        o.addProperty("bold", Boolean.valueOf(this.bold));
        o.addProperty("italic", Boolean.valueOf(this.italic));
        o.addProperty("underline", Boolean.valueOf(this.underline));
        o.addProperty("strikethrough", Boolean.valueOf(this.strikethrough));
        o.addProperty("obfuscated", Boolean.valueOf(this.obfuscated));
        o.addProperty("shadow", Boolean.valueOf(this.shadow));
        o.addProperty("gradient", Boolean.valueOf(this.gradient));
        o.addProperty("gradFrom", this.gradFrom);
        o.addProperty("gradTo", this.gradTo);
        o.addProperty("rainbow", Boolean.valueOf(this.rainbow));
        o.addProperty("rainbowStyle", (Number)this.rainbowStyle);
        o.addProperty("particles", Boolean.valueOf(this.particles));
        o.addProperty("particleStyle", (Number)this.particleStyle);
        o.addProperty("particleMovement", (Number)this.particleMovement);
        o.addProperty("particleAnchor", (Number)this.particleAnchor);
        o.addProperty("particleDensity", (Number)this.particleDensity);
        o.addProperty("particleSpeed", (Number)this.particleSpeed);
        o.addProperty("particleSize", (Number)this.particleSize);
        o.addProperty("particleSpread", (Number)this.particleSpread);
        o.addProperty("particleOffX", (Number)Float.valueOf(this.particleOffX));
        o.addProperty("particleOffY", (Number)Float.valueOf(this.particleOffY));
        o.addProperty("particleRate", (Number)this.particleRate);
        return o;
    }

    public static HoloLine fromJson(JsonObject o) {
        HoloLine l = new HoloLine();
        if (o.has("text")) {
            l.text = o.get("text").getAsString();
        }
        if (o.has("color")) {
            l.color = o.get("color").getAsString();
        }
        if (o.has("bold")) {
            l.bold = o.get("bold").getAsBoolean();
        }
        if (o.has("italic")) {
            l.italic = o.get("italic").getAsBoolean();
        }
        if (o.has("underline")) {
            l.underline = o.get("underline").getAsBoolean();
        }
        if (o.has("strikethrough")) {
            l.strikethrough = o.get("strikethrough").getAsBoolean();
        }
        if (o.has("obfuscated")) {
            l.obfuscated = o.get("obfuscated").getAsBoolean();
        }
        if (o.has("shadow")) {
            l.shadow = o.get("shadow").getAsBoolean();
        }
        if (o.has("gradient")) {
            l.gradient = o.get("gradient").getAsBoolean();
        }
        if (o.has("gradFrom")) {
            l.gradFrom = o.get("gradFrom").getAsString();
        }
        if (o.has("gradTo")) {
            l.gradTo = o.get("gradTo").getAsString();
        }
        if (o.has("rainbow")) {
            l.rainbow = o.get("rainbow").getAsBoolean();
        }
        if (o.has("rainbowStyle")) {
            l.rainbowStyle = o.get("rainbowStyle").getAsInt();
        }
        if (o.has("particles")) {
            l.particles = o.get("particles").getAsBoolean();
        }
        if (o.has("particleStyle")) {
            l.particleStyle = o.get("particleStyle").getAsInt();
        }
        if (o.has("particleMovement")) {
            l.particleMovement = o.get("particleMovement").getAsInt();
        }
        if (o.has("particleAnchor")) {
            l.particleAnchor = o.get("particleAnchor").getAsInt();
        }
        if (o.has("particleDensity")) {
            l.particleDensity = o.get("particleDensity").getAsInt();
        }
        if (o.has("particleSpeed")) {
            l.particleSpeed = o.get("particleSpeed").getAsInt();
        }
        if (o.has("particleSize")) {
            l.particleSize = o.get("particleSize").getAsInt();
        }
        if (o.has("particleSpread")) {
            l.particleSpread = o.get("particleSpread").getAsInt();
        }
        if (o.has("particleOffX")) {
            l.particleOffX = o.get("particleOffX").getAsFloat();
        }
        if (o.has("particleOffY")) {
            l.particleOffY = o.get("particleOffY").getAsFloat();
        }
        if (o.has("particleRate")) {
            l.particleRate = o.get("particleRate").getAsInt();
        }
        return l;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.text);
        buf.writeUtf(this.color);
        buf.writeBoolean(this.bold);
        buf.writeBoolean(this.italic);
        buf.writeBoolean(this.underline);
        buf.writeBoolean(this.strikethrough);
        buf.writeBoolean(this.obfuscated);
        buf.writeBoolean(this.shadow);
        buf.writeBoolean(this.gradient);
        buf.writeUtf(this.gradFrom);
        buf.writeUtf(this.gradTo);
        buf.writeBoolean(this.rainbow);
        buf.writeVarInt(this.rainbowStyle);
        buf.writeBoolean(this.particles);
        buf.writeVarInt(this.particleStyle);
        buf.writeVarInt(this.particleMovement);
        buf.writeVarInt(this.particleAnchor);
        buf.writeVarInt(this.particleDensity);
        buf.writeVarInt(this.particleSpeed);
        buf.writeVarInt(this.particleSize);
        buf.writeVarInt(this.particleSpread);
        buf.writeFloat(this.particleOffX);
        buf.writeFloat(this.particleOffY);
        buf.writeVarInt(this.particleRate);
    }

    public static HoloLine decode(FriendlyByteBuf buf) {
        HoloLine l = new HoloLine();
        l.text = buf.readUtf();
        l.color = buf.readUtf();
        l.bold = buf.readBoolean();
        l.italic = buf.readBoolean();
        l.underline = buf.readBoolean();
        l.strikethrough = buf.readBoolean();
        l.obfuscated = buf.readBoolean();
        l.shadow = buf.readBoolean();
        l.gradient = buf.readBoolean();
        l.gradFrom = buf.readUtf();
        l.gradTo = buf.readUtf();
        l.rainbow = buf.readBoolean();
        l.rainbowStyle = buf.readVarInt();
        l.particles = buf.readBoolean();
        l.particleStyle = buf.readVarInt();
        l.particleMovement = buf.readVarInt();
        l.particleAnchor = buf.readVarInt();
        l.particleDensity = buf.readVarInt();
        l.particleSpeed = buf.readVarInt();
        l.particleSize = buf.readVarInt();
        l.particleSpread = buf.readVarInt();
        l.particleOffX = buf.readFloat();
        l.particleOffY = buf.readFloat();
        l.particleRate = buf.readVarInt();
        return l;
    }

    public HoloLine copy() {
        HoloLine l = new HoloLine();
        l.text = this.text;
        l.color = this.color;
        l.bold = this.bold;
        l.italic = this.italic;
        l.underline = this.underline;
        l.strikethrough = this.strikethrough;
        l.obfuscated = this.obfuscated;
        l.shadow = this.shadow;
        l.gradient = this.gradient;
        l.gradFrom = this.gradFrom;
        l.gradTo = this.gradTo;
        l.rainbow = this.rainbow;
        l.rainbowStyle = this.rainbowStyle;
        l.particles = this.particles;
        l.particleStyle = this.particleStyle;
        l.particleMovement = this.particleMovement;
        l.particleAnchor = this.particleAnchor;
        l.particleDensity = this.particleDensity;
        l.particleSpeed = this.particleSpeed;
        l.particleSize = this.particleSize;
        l.particleSpread = this.particleSpread;
        l.particleOffX = this.particleOffX;
        l.particleOffY = this.particleOffY;
        l.particleRate = this.particleRate;
        return l;
    }
}

