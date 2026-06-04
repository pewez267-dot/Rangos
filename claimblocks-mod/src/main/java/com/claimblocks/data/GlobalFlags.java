package com.claimblocks.data;

import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-wide protection flags that apply OUTSIDE any claim. Persisted in
 * {@code <world>/global_flags.json} alongside the per-claim data.
 *
 * Implementation note: most flags map onto vanilla gamerules so we don't
 * have to write extra event handlers. We mirror them into the gamerules
 * whenever a flag is set, and also restore them on server start.
 */
public final class GlobalFlags {
    private static final String FILE = "global_flags.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GlobalFlags INSTANCE;

    public boolean globalPVP          = true;
    public boolean globalMobGriefing  = true;
    public boolean globalFireSpread   = true;

    private GlobalFlags() {}

    public static GlobalFlags getInstance() {
        if (INSTANCE == null) INSTANCE = new GlobalFlags();
        return INSTANCE;
    }

    public boolean get(String key) {
        return switch (key) {
            case "globalPVP"         -> globalPVP;
            case "globalMobGriefing" -> globalMobGriefing;
            case "globalFireSpread"  -> globalFireSpread;
            default -> false;
        };
    }

    public void set(String key, boolean value, MinecraftServer server) {
        switch (key) {
            case "globalPVP"         -> globalPVP = value;
            case "globalMobGriefing" -> globalMobGriefing = value;
            case "globalFireSpread"  -> globalFireSpread = value;
            default -> {}
        }
        applyToServer(server);
        save(server);
    }

    /** Apply the current flag values to the server's vanilla gamerules. */
    public void applyToServer(MinecraftServer server) {
        if (server == null) return;
        server.setPvpEnabled(globalPVP);
        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            GameRules rules = overworld.getGameRules();
            rules.get(GameRules.DO_MOB_GRIEFING).set(globalMobGriefing, server);
            rules.get(GameRules.DO_FIRE_TICK).set(globalFireSpread, server);
        }
    }

    public void load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.exists(file)) {
            ClaimBlocksMod.LOGGER.info("No global_flags.json, defaults applied.");
            applyToServer(server);
            return;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(text).getAsJsonObject();
            if (o.has("globalPVP"))         globalPVP         = o.get("globalPVP").getAsBoolean();
            if (o.has("globalMobGriefing")) globalMobGriefing = o.get("globalMobGriefing").getAsBoolean();
            if (o.has("globalFireSpread"))  globalFireSpread  = o.get("globalFireSpread").getAsBoolean();
            applyToServer(server);
            ClaimBlocksMod.LOGGER.info("Global flags cargadas: PVP={} MobGrief={} FireSpread={}",
                globalPVP, globalMobGriefing, globalFireSpread);
        } catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("No se pudo cargar global_flags.json", e);
        }
    }

    public void save(MinecraftServer server) {
        Path file = file(server);
        try {
            JsonObject o = new JsonObject();
            o.addProperty("globalPVP", globalPVP);
            o.addProperty("globalMobGriefing", globalMobGriefing);
            o.addProperty("globalFireSpread", globalFireSpread);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ClaimBlocksMod.LOGGER.error("No se pudo guardar global_flags.json", e);
        }
    }

    private Path file(MinecraftServer s) {
        return s.getSavePath(WorldSavePath.ROOT).resolve(FILE);
    }
}
