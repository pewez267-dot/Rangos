package com.claimblocks.data;

import com.claimblocks.ClaimBlocksMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that owns every {@link Claim} on the server. Persisted to
 * {@code <world>/data/claimblocks_data.dat} as NBT.
 */
public class ClaimManager {
    private static ClaimManager instance;

    /** dimension id ("minecraft:overworld") -> claim list */
    private final Map<String, List<Claim>> claimsByDimension = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private boolean dirty = false;

    private ClaimManager() {}

    public static ClaimManager getInstance() {
        if (instance == null) {
            instance = new ClaimManager();
        }
        return instance;
    }

    public void createClaim(World world, BlockPos pos, PlayerEntity owner, int tier, int radius) {
        String dim = world.getRegistryKey().getValue().toString();
        Claim claim = new Claim(pos, owner.getUuid(), owner.getName().getString(), tier, radius, dim);
        claimsByDimension.computeIfAbsent(dim, k -> new ArrayList<>()).add(claim);
        markDirty();
    }

    public void addClaim(Claim claim) {
        claimsByDimension.computeIfAbsent(claim.getDimension(), k -> new ArrayList<>()).add(claim);
        markDirty();
    }

    public void removeClaim(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByDimension.get(dim);
        if (list != null) {
            list.removeIf(c -> c.getCenter().equals(pos));
            markDirty();
        }
    }

    public Claim getClaimAt(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByDimension.get(dim);
        if (list == null) return null;
        for (Claim c : list) {
            if (c.containsPosition(pos)) return c;
        }
        return null;
    }

    public Claim getClaimByCenter(World world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByDimension.get(dim);
        if (list == null) return null;
        for (Claim c : list) {
            if (c.getCenter().equals(pos)) return c;
        }
        return null;
    }

    public boolean wouldOverlap(World world, BlockPos pos, int radius) {
        String dim = world.getRegistryKey().getValue().toString();
        List<Claim> list = claimsByDimension.get(dim);
        if (list == null) return false;
        for (Claim c : list) {
            if (c.overlaps(pos, radius)) return true;
        }
        return false;
    }

    public List<Claim> getAllClaims() {
        List<Claim> all = new ArrayList<>();
        for (List<Claim> list : claimsByDimension.values()) {
            all.addAll(list);
        }
        return all;
    }

    public List<Claim> getClaimsByDimension(String dim) {
        return claimsByDimension.getOrDefault(dim, new ArrayList<>());
    }

    public List<Claim> getClaimsOfPlayer(UUID playerId) {
        List<Claim> result = new ArrayList<>();
        for (List<Claim> list : claimsByDimension.values()) {
            for (Claim c : list) {
                if (c.isOwner(playerId)) result.add(c);
            }
        }
        return result;
    }

    public int clearClaimsOfPlayer(MinecraftServer server, UUID playerId) {
        int removed = 0;
        for (Map.Entry<String, List<Claim>> e : claimsByDimension.entrySet()) {
            List<Claim> list = e.getValue();
            List<Claim> toRemove = new ArrayList<>();
            for (Claim c : list) {
                if (c.isOwner(playerId)) toRemove.add(c);
            }
            for (Claim c : toRemove) {
                // Also try to remove the actual block from the world
                ServerWorld w = serverWorldByDimension(server, c.getDimension());
                if (w != null) {
                    BlockPos p = c.getCenter();
                    if (!w.getBlockState(p).isAir()) {
                        w.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState());
                    }
                }
                list.remove(c);
                removed++;
            }
        }
        if (removed > 0) markDirty();
        return removed;
    }

    private ServerWorld serverWorldByDimension(MinecraftServer server, String dimensionKey) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(dimensionKey)) {
                return world;
            }
        }
        return null;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() { return dirty; }

    public void loadClaims(MinecraftServer server) {
        this.server = server;
        claimsByDimension.clear();
        File file = getClaimsFile(server);
        if (!file.exists()) {
            ClaimBlocksMod.LOGGER.info("No existing claims file, starting fresh.");
            return;
        }
        try {
            NbtCompound root = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            NbtList list = root.getList("claims", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                Claim claim = Claim.fromNbt(list.getCompound(i));
                claimsByDimension.computeIfAbsent(claim.getDimension(), k -> new ArrayList<>()).add(claim);
            }
            dirty = false;
            ClaimBlocksMod.LOGGER.info("Loaded {} claims from disk.", list.size());
        } catch (IOException e) {
            ClaimBlocksMod.LOGGER.error("Failed to load claims", e);
        }
    }

    public void saveClaims(MinecraftServer server) {
        File file = getClaimsFile(server);
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            NbtCompound root = new NbtCompound();
            NbtList list = new NbtList();
            for (Claim c : getAllClaims()) {
                list.add(c.toNbt());
            }
            root.put("claims", list);
            NbtIo.writeCompressed(root, file.toPath());
            dirty = false;
        } catch (IOException e) {
            ClaimBlocksMod.LOGGER.error("Failed to save claims", e);
        }
    }

    private File getClaimsFile(MinecraftServer server) {
        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
        return new File(worldDir, "data/claimblocks_data.dat");
    }

    public Collection<List<Claim>> getClaimsByDimensionMap() {
        return claimsByDimension.values();
    }
}
