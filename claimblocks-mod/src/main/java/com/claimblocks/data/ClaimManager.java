package com.claimblocks.data;

import com.claimblocks.ClaimBlocksMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton owning every {@link Claim} on the server. Persisted to
 * {@code <world>/claimblocks_data.json}. Saves are triggered after every
 * mutation as well as on server stop.
 *
 * Old v2.x records (with a {@code tier} integer 1-5) are auto-migrated into
 * the new (radius, height) format the first time they are loaded.
 */
public class ClaimManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "claimblocks_data.json";
    private static ClaimManager INSTANCE;

    private final Map<String, List<Claim>> claimsByWorld = new ConcurrentHashMap<>();
    private MinecraftServer server;

    /** OPs that are currently in admin "bypass" mode. In-memory only. */
    private final Set<UUID> bypassPlayers = new HashSet<>();

    /** Pending messages for offline owners. In-memory only (lost on restart). */
    private final Map<UUID, List<Text>> pendingMessages = new HashMap<>();

    private ClaimManager() {}

    public static ClaimManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ClaimManager();
        return INSTANCE;
    }

    public MinecraftServer getServer() { return server; }

    /* ---------------------------------------------------------- mutators */

    public Claim createClaim(World world, BlockPos pos, PlayerEntity owner, ClaimTier tier) {
        String dim = world.getRegistryKey().getValue().toString();
        Claim c = Claim.create(owner.getUuid(), owner.getName().getString(), tier, dim, pos);
        claimsByWorld.computeIfAbsent(dim, k -> new ArrayList<>()).add(c);
        save();
        return c;
    }

    public boolean removeClaim(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByWorld.get(dim);
        if (list == null) return false;
        boolean removed = list.removeIf(c ->
            c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ());
        if (removed) save();
        return removed;
    }

    public int clearClaimsOf(UUID playerId) {
        int total = 0;
        for (Map.Entry<String, List<Claim>> e : claimsByWorld.entrySet()) {
            List<Claim> list = e.getValue();
            List<Claim> toRemove = new ArrayList<>();
            for (Claim c : list) if (c.isOwner(playerId)) toRemove.add(c);
            for (Claim c : toRemove) {
                if (server != null) {
                    ServerWorld w = worldFor(e.getKey());
                    if (w != null) {
                        BlockPos p = c.getCenter();
                        if (!w.getBlockState(p).isAir()) {
                            w.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState());
                        }
                    }
                }
                list.remove(c);
                total++;
            }
        }
        if (total > 0) save();
        return total;
    }

    private ServerWorld worldFor(String dimensionKey) {
        if (server == null) return null;
        for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(dimensionKey)) return w;
        }
        return null;
    }

    /* ----------------------------------------------------------- queries */

    public Claim getClaimAt(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByWorld.get(dim);
        if (list == null) return null;
        for (Claim c : list) if (c.contains(pos)) return c;
        return null;
    }

    public Claim getClaimByCenter(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByWorld.get(dim);
        if (list == null) return null;
        for (Claim c : list) {
            if (c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ()) return c;
        }
        return null;
    }

    public boolean wouldOverlap(World world, BlockPos pos, int radius, int height) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByWorld.get(dim);
        if (list == null) return false;
        for (Claim c : list) if (c.overlapsWith(pos, radius, height)) return true;
        return false;
    }

    public List<Claim> getAllClaims() {
        List<Claim> all = new ArrayList<>();
        for (List<Claim> l : claimsByWorld.values()) all.addAll(l);
        return all;
    }

    public List<Claim> getClaimsOf(UUID playerId) {
        List<Claim> r = new ArrayList<>();
        for (List<Claim> l : claimsByWorld.values()) {
            for (Claim c : l) if (c.isOwner(playerId)) r.add(c);
        }
        return r;
    }

    public List<Claim> getClaimsInWorld(String dim) {
        return Collections.unmodifiableList(claimsByWorld.getOrDefault(dim, new ArrayList<>()));
    }

    /* ------------------------------------------------------ persistence */

    public void save() {
        if (server == null) return;
        Path file = dataFile(server);
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Claim c : getAllClaims()) arr.add(c.toJson());
            root.add("claims", arr);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ClaimBlocksMod.LOGGER.error("Could not save claims to " + file, e);
        }
    }

    public void load(MinecraftServer server) {
        this.server = server;
        claimsByWorld.clear();
        Path file = dataFile(server);
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
            int count = 0;
            int migrated = 0;
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                boolean wasLegacy = !obj.has("radius") && obj.has("tier");
                Claim c = Claim.fromJson(obj);
                claimsByWorld.computeIfAbsent(c.getWorld(), k -> new ArrayList<>()).add(c);
                count++;
                if (wasLegacy) migrated++;
            }
            ClaimBlocksMod.LOGGER.info("Loaded {} claims from {} (migrated {} legacy)",
                count, file, migrated);
            if (migrated > 0) save(); // persist migrated form immediately
        } catch (Exception e) {
            ClaimBlocksMod.LOGGER.error("Could not load claims from " + file, e);
        }
    }

    private Path dataFile(MinecraftServer s) {
        return s.getSavePath(WorldSavePath.ROOT).resolve(DATA_FILE);
    }

    /* ----------------------------------------------------- bypass mode (op) */

    public boolean isBypassing(UUID id) { return bypassPlayers.contains(id); }
    public boolean toggleBypass(UUID id) {
        if (bypassPlayers.contains(id)) { bypassPlayers.remove(id); return false; }
        bypassPlayers.add(id);
        return true;
    }
    public Set<UUID> getBypassPlayers() { return bypassPlayers; }

    /* ----------------------------------------------- pending messages (op) */

    public void queueMessage(UUID owner, Text msg) {
        pendingMessages.computeIfAbsent(owner, k -> new ArrayList<>()).add(msg);
    }

    /** Called from ServerPlayConnectionEvents.JOIN to flush queued messages. */
    public void flushPendingTo(ServerPlayerEntity player) {
        List<Text> msgs = pendingMessages.remove(player.getUuid());
        if (msgs == null) return;
        for (Text t : msgs) player.sendMessage(t, false);
    }
}
