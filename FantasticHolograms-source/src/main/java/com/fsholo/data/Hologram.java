/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.minecraft.network.FriendlyByteBuf
 */
package com.fsholo.data;

import com.fsholo.data.HoloLine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public class Hologram {
    public String id;
    public String dimension;
    public double x;
    public double y;
    public double z;
    public double yOffset = 1.0;
    public double lineSpacing = 0.28;
    public float scale = 1.0f;
    public float background = 0.3f;
    public int animation = 0;
    public int animSpeed = 1;
    public int animIntensity = 1;
    public final List<HoloLine> lines = new ArrayList<HoloLine>();

    public Hologram() {
    }

    public Hologram(String id, String dimension, double x, double y, double z) {
        this.id = id;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", this.id);
        o.addProperty("dimension", this.dimension);
        o.addProperty("x", (Number)this.x);
        o.addProperty("y", (Number)this.y);
        o.addProperty("z", (Number)this.z);
        o.addProperty("yOffset", (Number)this.yOffset);
        o.addProperty("lineSpacing", (Number)this.lineSpacing);
        o.addProperty("scale", (Number)Float.valueOf(this.scale));
        o.addProperty("background", (Number)Float.valueOf(this.background));
        o.addProperty("animation", (Number)this.animation);
        o.addProperty("animSpeed", (Number)this.animSpeed);
        o.addProperty("animIntensity", (Number)this.animIntensity);
        JsonArray arr = new JsonArray();
        for (HoloLine l : this.lines) {
            arr.add((JsonElement)l.toJson());
        }
        o.add("lines", (JsonElement)arr);
        return o;
    }

    public static Hologram fromJson(JsonObject o) {
        Hologram h = new Hologram();
        h.id = o.get("id").getAsString();
        h.dimension = o.has("dimension") ? o.get("dimension").getAsString() : "minecraft:overworld";
        h.x = o.get("x").getAsDouble();
        h.y = o.get("y").getAsDouble();
        h.z = o.get("z").getAsDouble();
        if (o.has("yOffset")) {
            h.yOffset = o.get("yOffset").getAsDouble();
        }
        if (o.has("lineSpacing")) {
            h.lineSpacing = o.get("lineSpacing").getAsDouble();
        }
        if (o.has("scale")) {
            h.scale = o.get("scale").getAsFloat();
        }
        if (o.has("background")) {
            h.background = o.get("background").getAsFloat();
        }
        if (o.has("animation")) {
            h.animation = o.get("animation").getAsInt();
        }
        if (o.has("animSpeed")) {
            h.animSpeed = o.get("animSpeed").getAsInt();
        }
        if (o.has("animIntensity")) {
            h.animIntensity = o.get("animIntensity").getAsInt();
        }
        if (o.has("lines")) {
            JsonArray arr = o.getAsJsonArray("lines");
            for (int i = 0; i < arr.size(); ++i) {
                h.lines.add(HoloLine.fromJson(arr.get(i).getAsJsonObject()));
            }
        }
        return h;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.id);
        buf.writeUtf(this.dimension);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.yOffset);
        buf.writeDouble(this.lineSpacing);
        buf.writeFloat(this.scale);
        buf.writeFloat(this.background);
        buf.writeVarInt(this.animation);
        buf.writeVarInt(this.animSpeed);
        buf.writeVarInt(this.animIntensity);
        buf.writeVarInt(this.lines.size());
        for (HoloLine l : this.lines) {
            l.encode(buf);
        }
    }

    public static Hologram decode(FriendlyByteBuf buf) {
        Hologram h = new Hologram();
        h.id = buf.readUtf();
        h.dimension = buf.readUtf();
        h.x = buf.readDouble();
        h.y = buf.readDouble();
        h.z = buf.readDouble();
        h.yOffset = buf.readDouble();
        h.lineSpacing = buf.readDouble();
        h.scale = buf.readFloat();
        h.background = buf.readFloat();
        h.animation = buf.readVarInt();
        h.animSpeed = buf.readVarInt();
        h.animIntensity = buf.readVarInt();
        int n = buf.readVarInt();
        for (int i = 0; i < n; ++i) {
            h.lines.add(HoloLine.decode(buf));
        }
        return h;
    }
}

