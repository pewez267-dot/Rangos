/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.class_1928
 *  net.minecraft.class_1928$class_4310
 *  net.minecraft.class_3218
 *  net.minecraft.class_5218
 *  net.minecraft.server.MinecraftServer
 */
package com.claimblocks.data;

import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.minecraft.class_1928;
import net.minecraft.class_3218;
import net.minecraft.class_5218;
import net.minecraft.server.MinecraftServer;

public final class GlobalFlags {
    private static final String FILE = "global_flags.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GlobalFlags INSTANCE;
    public boolean globalPVP = true;
    public boolean globalMobGriefing = true;
    public boolean globalFireSpread = true;

    private GlobalFlags() {
    }

    public static GlobalFlags getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GlobalFlags();
        }
        return INSTANCE;
    }

    public boolean get(String key) {
        return switch (key) {
            case "globalPVP" -> this.globalPVP;
            case "globalMobGriefing" -> this.globalMobGriefing;
            case "globalFireSpread" -> this.globalFireSpread;
            default -> false;
        };
    }

    public void set(String key, boolean value, MinecraftServer server) {
        switch (key) {
            case "globalPVP": {
                this.globalPVP = value;
                break;
            }
            case "globalMobGriefing": {
                this.globalMobGriefing = value;
                break;
            }
            case "globalFireSpread": {
                this.globalFireSpread = value;
                break;
            }
        }
        this.applyToServer(server);
        this.save(server);
    }

    public void applyToServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        server.method_3815(this.globalPVP);
        class_3218 overworld = server.method_30002();
        if (overworld != null) {
            class_1928 rules = overworld.method_8450();
            ((class_1928.class_4310)rules.method_20746(class_1928.field_19388)).method_20758(this.globalMobGriefing, server);
            ((class_1928.class_4310)rules.method_20746(class_1928.field_19387)).method_20758(this.globalFireSpread, server);
        }
    }

    public void load(MinecraftServer server) {
        Path file = this.file(server);
        if (!Files.exists(file, new LinkOption[0])) {
            ClaimBlocksMod.LOGGER.info("No global_flags.json, defaults applied.");
            this.applyToServer(server);
            return;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString((String)text).getAsJsonObject();
            if (o.has("globalPVP")) {
                this.globalPVP = o.get("globalPVP").getAsBoolean();
            }
            if (o.has("globalMobGriefing")) {
                this.globalMobGriefing = o.get("globalMobGriefing").getAsBoolean();
            }
            if (o.has("globalFireSpread")) {
                this.globalFireSpread = o.get("globalFireSpread").getAsBoolean();
            }
            this.applyToServer(server);
            ClaimBlocksMod.LOGGER.info("Global flags cargadas: PVP={} MobGrief={} FireSpread={}", new Object[]{this.globalPVP, this.globalMobGriefing, this.globalFireSpread});
        }
        catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("No se pudo cargar global_flags.json", (Throwable)e);
        }
    }

    public void save(MinecraftServer server) {
        Path file = this.file(server);
        try {
            JsonObject o = new JsonObject();
            o.addProperty("globalPVP", Boolean.valueOf(this.globalPVP));
            o.addProperty("globalMobGriefing", Boolean.valueOf(this.globalMobGriefing));
            o.addProperty("globalFireSpread", Boolean.valueOf(this.globalFireSpread));
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            Files.writeString(file, (CharSequence)GSON.toJson((JsonElement)o), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            ClaimBlocksMod.LOGGER.error("No se pudo guardar global_flags.json", (Throwable)e);
        }
    }

    private Path file(MinecraftServer s) {
        return s.method_27050(class_5218.field_24188).resolve(FILE);
    }
}

