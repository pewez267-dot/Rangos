package com.claimblocks.data;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

public class ClaimManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "claimblocks_data.json";
    private static final String CONFIG_FILE = "claimblocks_config.json";

    private static int MAX_CLAIMS_PER_PLAYER = 0;

    private static ClaimManager INSTANCE;
    private final Map<String, List<Claim>> claimsByWorld = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private final Set<UUID> bypassPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<Component>> pendingMessages = new ConcurrentHashMap<>();

    private ClaimManager() {}

    public static ClaimManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ClaimManager();
        return INSTANCE;
    }

    public static int getMaxClaimsPerPlayer() { return MAX_CLAIMS_PER_PLAYER; }
    public static void setMaxClaimsPerPlayer(int n) { MAX_CLAIMS_PER_PLAYER = Math.max(0, n); }

    public MinecraftServer getServer() { return this.server; }

    public Claim createClaim(Level world, BlockPos pos, Player owner, ClaimTier tier) {
        String dim = world.dimension().location().toString();
        Claim c = Claim.create(owner.getUUID(), owner.getName().getString(), tier, dim, pos);
        if (tier != null) {
            switch (tier.id) {
                case "claimstone_500x500" -> {
                    c.getFlags().effectRegeneration = true;
                    c.getFlags().effectResistance = true;
                    c.getFlags().effectSpeed = true;
                    c.getFlags().allowFlight = true;
                }
                case "claimstone_300x300" -> {
                    c.getFlags().effectRegeneration = true;
                    c.getFlags().effectResistance = true;
                    c.getFlags().effectSpeed = true;
                }
                case "claimstone_250x250" -> c.getFlags().effectRegeneration = true;
            }
        }
        this.claimsByWorld.computeIfAbsent(dim, k -> Collections.synchronizedList(new ArrayList<>())).add(c);
        this.save();
        return c;
    }

    public boolean removeClaim(Level world, BlockPos pos) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) return false;
        boolean removed;
        synchronized (list) {
            removed = list.removeIf(c -> c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ());
        }
        if (removed) this.save();
        return removed;
    }

    public int clearClaimsOf(UUID playerId) {
        int total = 0;
        for (Map.Entry<String, List<Claim>> e : this.claimsByWorld.entrySet()) {
            List<Claim> list = e.getValue();
            List<Claim> toRemove = new ArrayList<>();
            synchronized (list) {
                for (Claim c : list) if (c.isOwner(playerId)) toRemove.add(c);
            }
            for (Claim c : toRemove) {
                if (this.server != null) {
                    ServerLevel w = this.worldFor(e.getKey());
                    if (w != null) {
                        BlockPos p = c.getCenter();
                        if (ClaimBlocks.isClaimConcreteForTier(w.getBlockState(p).getBlock(), c.getTier())) {
                            w.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                synchronized (list) { list.remove(c); }
                ++total;
            }
        }
        if (total > 0) this.save();
        return total;
    }

    public boolean transferOwnership(Claim claim, UUID newOwnerId, String newOwnerName) {
        if (claim == null || newOwnerId == null) return false;
        claim.setOwner(newOwnerId, newOwnerName);
        this.save();
        return true;
    }

    private ServerLevel worldFor(String dimensionKey) {
        if (this.server == null) return null;
        for (ServerLevel w : this.server.getAllLevels()) {
            if (w.dimension().location().toString().equals(dimensionKey)) return w;
        }
        return null;
    }

    public Claim getClaimAt(Level world, BlockPos pos) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) return null;
        synchronized (list) {
            for (Claim c : list) if (c.contains(pos)) return c;
        }
        return null;
    }

    public Claim getClaimByCenter(Level world, BlockPos pos) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) return null;
        synchronized (list) {
            for (Claim c : list) {
                if (c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ()) return c;
            }
        }
        return null;
    }

    public boolean wouldOverlap(Level world, BlockPos pos, int radius, int height) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) return false;
        synchronized (list) {
            for (Claim c : list) if (c.overlapsWith(pos, radius, height)) return true;
        }
        return false;
    }

    public List<Claim> getAllClaims() {
        ArrayList<Claim> all = new ArrayList<>();
        for (List<Claim> l : this.claimsByWorld.values()) {
            synchronized (l) { all.addAll(l); }
        }
        return all;
    }

    public List<Claim> getClaimsOf(UUID playerId) {
        ArrayList<Claim> r = new ArrayList<>();
        for (List<Claim> l : this.claimsByWorld.values()) {
            synchronized (l) {
                for (Claim c : l) if (c.isOwner(playerId)) r.add(c);
            }
        }
        return r;
    }

    public List<Claim> getClaimsInWorld(String dim) {
        List<Claim> l = this.claimsByWorld.getOrDefault(dim, Collections.emptyList());
        synchronized (l) { return new ArrayList<>(l); }
    }

    public void save() {
        if (this.server == null) return;
        Path file = this.dataFile(this.server);
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Claim c : this.getAllClaims()) arr.add(c.toJson());
            root.add("claims", arr);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ClaimBlocksMod.LOGGER.error("Could not save claims to " + file, e);
        }
    }

    public void load(MinecraftServer server) {
        this.server = server;
        this.claimsByWorld.clear();
        loadConfig(server);
        Path file = this.dataFile(server);
        if (!Files.exists(file)) {
            ClaimBlocksMod.LOGGER.info("No existing claims file at {}, starting fresh.", file);
            return;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) return;
            JsonElement el = JsonParser.parseString(text);
            if (!el.isJsonObject()) return;
            JsonArray arr = el.getAsJsonObject().getAsJsonArray("claims");
            if (arr == null) return;
            int count = 0, migrated = 0;
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                boolean wasLegacy = !obj.has("radius") && obj.has("tier");
                Claim c = Claim.fromJson(obj);
                this.claimsByWorld.computeIfAbsent(c.getWorld(), k -> Collections.synchronizedList(new ArrayList<>())).add(c);
                ++count;
                if (wasLegacy) ++migrated;
            }
            ClaimBlocksMod.LOGGER.info("Loaded {} claims from {} (migrated {} legacy)", count, file, migrated);
            if (migrated > 0) this.save();
        } catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("Could not load claims from " + file, e);
        }
    }

    private void loadConfig(MinecraftServer s) {
        Path cfg = s.getWorldPath(LevelResource.ROOT).resolve(CONFIG_FILE);
        try {
            if (!Files.exists(cfg)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("maxClaimsPerPlayer", 0);
                obj.addProperty("_doc_maxClaimsPerPlayer", "0 = unlimited; max claims a non-OP player can own");
                Files.createDirectories(cfg.getParent());
                Files.writeString(cfg, GSON.toJson(obj), StandardCharsets.UTF_8);
                return;
            }
            JsonElement el = JsonParser.parseString(Files.readString(cfg, StandardCharsets.UTF_8));
            if (el != null && el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                if (o.has("maxClaimsPerPlayer")) setMaxClaimsPerPlayer(o.get("maxClaimsPerPlayer").getAsInt());
            }
        } catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("Could not load config " + cfg, e);
        }
    }

    private Path dataFile(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve(DATA_FILE);
    }

    public boolean isBypassing(UUID id) { return this.bypassPlayers.contains(id); }

    public boolean toggleBypass(UUID id) {
        if (this.bypassPlayers.contains(id)) { this.bypassPlayers.remove(id); return false; }
        this.bypassPlayers.add(id); return true;
    }

    public Set<UUID> getBypassPlayers() { return this.bypassPlayers; }

    public void queueMessage(UUID owner, Component msg) {
        this.pendingMessages.computeIfAbsent(owner, k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
    }

    public void flushPendingTo(ServerPlayer player) {
        List<Component> msgs = this.pendingMessages.remove(player.getUUID());
        if (msgs == null) return;
        synchronized (msgs) {
            for (Component t : msgs) player.displayClientMessage(t, false);
        }
    }

    public void onPlayerDisconnect(UUID id) {
        this.bypassPlayers.remove(id);
    }
}
