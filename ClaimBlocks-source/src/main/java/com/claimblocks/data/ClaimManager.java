/*
 * Decompiled with CFR 0.152.
 */
package com.claimblocks.data;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimTier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
    private final Map<String, List<Claim>> claimsByWorld = new ConcurrentHashMap<String, List<Claim>>();
    private MinecraftServer server;
    private final Set<UUID> bypassPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<Component>> pendingMessages = new ConcurrentHashMap<UUID, List<Component>>();
    private final Map<UUID, ClaimGroup> groups = new ConcurrentHashMap<UUID, ClaimGroup>();
    private final Map<UUID, Claim> claimIndex = new ConcurrentHashMap<UUID, Claim>();

    private ClaimManager() {
    }

    public static ClaimManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClaimManager();
        }
        return INSTANCE;
    }

    public static int getMaxClaimsPerPlayer() {
        return MAX_CLAIMS_PER_PLAYER;
    }

    public static void setMaxClaimsPerPlayer(int n) {
        MAX_CLAIMS_PER_PLAYER = Math.max(0, n);
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public Claim createClaim(Level world, BlockPos pos, Player owner, ClaimTier tier) {
        String dim = world.dimension().location().toString();
        Claim c = Claim.create(owner.getUUID(), owner.getName().getString(), tier, dim, pos);
        if (tier != null) {
            String var7;
            switch (var7 = tier.id) {
                case "claimstone_500x500": {
                    c.getFlags().effectRegeneration = true;
                    c.getFlags().effectResistance = true;
                    c.getFlags().effectSpeed = true;
                    c.getFlags().allowFlight = true;
                    break;
                }
                case "claimstone_300x300": {
                    c.getFlags().effectRegeneration = true;
                    c.getFlags().effectResistance = true;
                    c.getFlags().effectSpeed = true;
                    break;
                }
                case "claimstone_250x250": {
                    c.getFlags().effectRegeneration = true;
                }
            }
        }
        this.claimsByWorld.computeIfAbsent(dim, k -> Collections.synchronizedList(new ArrayList())).add(c);
        this.claimIndex.put(c.getClaimId(), c);
        this.save();
        return c;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean removeClaim(Level world, BlockPos pos) {
        boolean removed;
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) {
            return false;
        }
        Claim found = null;
        List<Claim> list2 = list;
        synchronized (list2) {
            for (Claim c : list) {
                if (c.getX() == pos.getX() && c.getY() == pos.getY() && c.getZ() == pos.getZ()) {
                    found = c;
                    break;
                }
            }
            if (found != null) {
                list.remove(found);
            }
        }
        if (found != null) {
            this.claimIndex.remove(found.getClaimId());
            this.onClaimRemoved(found);
            this.save();
            return true;
        }
        return false;
    }

    // Limpieza de grupo al eliminar una claim: si era la nodriza, se disuelve el grupo.
    private void onClaimRemoved(Claim c) {
        if (c.getGroupId() == null) {
            return;
        }
        ClaimGroup g = this.groups.get(c.getGroupId());
        if (g != null && c.getClaimId().equals(g.getMotherClaimId())) {
            // Se rompio la nodriza -> disolver el grupo y romper/devolver las piedras solapadas.
            this.dissolveGroupBreaking(g.getGroupId());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int clearClaimsOf(UUID playerId) {
        int total = 0;
        for (Map.Entry<String, List<Claim>> e : this.claimsByWorld.entrySet()) {
            List<Claim> list = e.getValue();
            ArrayList<Claim> toRemove = new ArrayList<Claim>();
            List<Claim> list2 = list;
            synchronized (list2) {
                for (Claim c : list) {
                    if (!c.isOwner(playerId)) continue;
                    toRemove.add(c);
                }
            }
            for (Claim cx : toRemove) {
                BlockPos p;
                ServerLevel w;
                if (this.server != null && (w = this.worldFor(e.getKey())) != null && ClaimBlocks.isClaimConcreteForTier(w.getBlockState(p = cx.getCenter()).getBlock(), cx.getTier())) {
                    w.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
                List<Claim> list3 = list;
                synchronized (list3) {
                    list.remove(cx);
                }
                this.claimIndex.remove(cx.getClaimId());
                this.onClaimRemoved(cx);
                ++total;
            }
        }
        if (total > 0) {
            this.save();
        }
        return total;
    }

    public boolean transferOwnership(Claim claim, UUID newOwnerId, String newOwnerName) {
        if (claim != null && newOwnerId != null) {
            claim.setOwner(newOwnerId, newOwnerName);
            this.save();
            return true;
        }
        return false;
    }

    private ServerLevel worldFor(String dimensionKey) {
        if (this.server == null) {
            return null;
        }
        for (ServerLevel w : this.server.getAllLevels()) {
            if (!w.dimension().location().toString().equals(dimensionKey)) continue;
            return w;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Claim getClaimAt(Level world, BlockPos pos) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) {
            return null;
        }
        List<Claim> list2 = list;
        synchronized (list2) {
            for (Claim c : list) {
                if (!c.contains(pos)) continue;
                return c;
            }
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Claim getClaimByCenter(Level world, BlockPos pos) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) {
            return null;
        }
        List<Claim> list2 = list;
        synchronized (list2) {
            for (Claim c : list) {
                if (c.getX() != pos.getX() || c.getY() != pos.getY() || c.getZ() != pos.getZ()) continue;
                return c;
            }
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean wouldOverlap(Level world, BlockPos pos, int radius, int height) {
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) {
            return false;
        }
        List<Claim> list2 = list;
        synchronized (list2) {
            for (Claim c : list) {
                if (!c.overlapsWith(pos, radius, height)) continue;
                return true;
            }
            return false;
        }
    }

    // Devuelve todas las claims que se solaparian con una nueva en pos/radius/height.
    public List<Claim> overlappingClaims(Level world, BlockPos pos, int radius, int height) {
        ArrayList<Claim> out = new ArrayList<Claim>();
        String dim = world.dimension().location().toString();
        List<Claim> list = this.claimsByWorld.get(dim);
        if (list == null) {
            return out;
        }
        synchronized (list) {
            for (Claim c : list) {
                if (c.overlapsWith(pos, radius, height)) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    // ==================== GRUPOS (unir protecciones) ====================
    public ClaimGroup getGroup(UUID groupId) {
        return groupId == null ? null : this.groups.get(groupId);
    }

    public ClaimGroup getGroupOf(Claim claim) {
        return claim == null ? null : this.getGroup(claim.getGroupId());
    }

    public Claim findClaimById(UUID id) {
        return id == null ? null : this.claimIndex.get(id);
    }

    public Claim getMotherClaim(UUID groupId) {
        ClaimGroup g = this.getGroup(groupId);
        if (g == null || g.getMotherClaimId() == null) {
            return null;
        }
        return this.claimIndex.get(g.getMotherClaimId());
    }

    // Crea un grupo anclado en la piedra nodriza (mother). El dueno queda registrado.
    public ClaimGroup createGroup(Claim mother, String name) {
        UUID gid = UUID.randomUUID();
        ClaimGroup g = new ClaimGroup(gid, name, mother.getClaimId(), mother.getOwnerUUID());
        this.groups.put(gid, g);
        mother.setGroupId(gid);
        this.save();
        return g;
    }

    public void registerPlayer(UUID groupId, UUID playerId) {
        ClaimGroup g = this.getGroup(groupId);
        if (g != null) {
            g.register(playerId);
            this.save();
        }
    }

    public boolean isRegistered(UUID groupId, UUID playerId) {
        ClaimGroup g = this.getGroup(groupId);
        return g != null && g.isRegistered(playerId);
    }

    // Primer grupo en el que el jugador esta registrado (para salir del grupo).
    public ClaimGroup getGroupByRegistered(UUID playerId) {
        for (ClaimGroup g : this.groups.values()) {
            if (g.isRegistered(playerId)) {
                return g;
            }
        }
        return null;
    }

    // Une una claim recien colocada a un grupo existente.
    public void joinClaimToGroup(Claim claim, UUID groupId) {
        if (claim != null && this.groups.containsKey(groupId)) {
            claim.setGroupId(groupId);
            this.save();
        }
    }

    // Todas las claims que pertenecen a un grupo (en cualquier mundo).
    public List<Claim> getGroupClaims(UUID groupId) {
        ArrayList<Claim> out = new ArrayList<Claim>();
        if (groupId == null) {
            return out;
        }
        for (Claim c : this.getAllClaims()) {
            if (groupId.equals(c.getGroupId())) {
                out.add(c);
            }
        }
        return out;
    }

    // Disuelve el grupo: desliga TODAS sus claims (vuelven a ser independientes).
    public void dissolveGroup(UUID groupId) {
        if (this.groups.remove(groupId) == null) {
            return;
        }
        for (Claim c : this.getAllClaims()) {
            if (groupId.equals(c.getGroupId())) {
                c.setGroupId(null);
            }
        }
        this.save();
    }

    // Disuelve el grupo ROMPIENDO las piedras solapadas (todas menos la nodriza) y
    // devolviendo cada una a su dueno (o al suelo si no hay espacio/esta offline).
    public void dissolveGroupBreaking(UUID groupId) {
        ClaimGroup g = this.groups.get(groupId);
        if (g == null) {
            return;
        }
        Claim mother = this.getMotherClaim(groupId);
        UUID motherClaimId = mother != null ? mother.getClaimId() : g.getMotherClaimId();
        for (Claim c : this.getGroupClaims(groupId)) {
            if (motherClaimId != null && c.getClaimId().equals(motherClaimId)) {
                continue; // la piedra nodriza NO se rompe
            }
            this.breakAndReturn(c);
        }
        this.groups.remove(groupId);
        for (Claim c : this.getAllClaims()) {
            if (groupId.equals(c.getGroupId())) {
                c.setGroupId(null);
            }
        }
        this.save();
    }

    // Un miembro sale del grupo: rompe SUS piedras del grupo (devueltas) y se desregistra.
    // Si es el dueno de la nodriza -> disuelve el grupo entero rompiendo lo solapado.
    public void leaveGroupBreaking(UUID groupId, UUID playerId) {
        ClaimGroup g = this.getGroup(groupId);
        if (g == null) {
            return;
        }
        if (playerId != null && playerId.equals(g.getMotherOwnerId())) {
            this.dissolveGroupBreaking(groupId);
            return;
        }
        g.unregister(playerId);
        for (Claim c : this.getGroupClaims(groupId)) {
            if (c.isOwner(playerId)) {
                this.breakAndReturn(c);
            }
        }
        this.save();
    }

    // Rompe una claim: quita el bloque de piedra, devuelve el item al dueno (o lo suelta)
    // y elimina la claim del registro.
    private void breakAndReturn(Claim c) {
        ServerLevel w = this.worldFor(c.getWorld());
        BlockPos p = c.getCenter();
        ClaimTier tier = c.getTier();
        if (w != null && tier != null && ClaimBlocks.isClaimConcreteForTier(w.getBlockState(p).getBlock(), tier)) {
            w.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
        if (w != null && tier != null) {
            net.minecraft.world.item.ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
            ServerPlayer owner = (this.server == null || c.getOwnerUUID() == null) ? null : this.server.getPlayerList().getPlayer(c.getOwnerUUID());
            if (owner != null) {
                if (!owner.getInventory().add(stack)) {
                    owner.drop(stack, false);
                }
            } else {
                w.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(w, (double) p.getX() + 0.5, (double) p.getY() + 0.5, (double) p.getZ() + 0.5, stack));
            }
        }
        List<Claim> list = this.claimsByWorld.get(c.getWorld());
        if (list != null) {
            synchronized (list) {
                list.remove(c);
            }
        }
        this.claimIndex.remove(c.getClaimId());
    }

    // Un jugador sale del grupo: se desregistra y sus claims del grupo se desligan.
    public void removePlayerFromGroup(UUID groupId, UUID playerId) {
        ClaimGroup g = this.getGroup(groupId);
        if (g == null) {
            return;
        }
        // Si sale el dueno de la nodriza, se disuelve el grupo entero.
        if (playerId != null && playerId.equals(g.getMotherOwnerId())) {
            this.dissolveGroup(groupId);
            return;
        }
        g.unregister(playerId);
        for (Claim c : this.getGroupClaims(groupId)) {
            if (c.isOwner(playerId)) {
                c.setGroupId(null);
            }
        }
        this.save();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Claim> getAllClaims() {
        ArrayList<Claim> all = new ArrayList<Claim>();
        Iterator<List<Claim>> iterator = this.claimsByWorld.values().iterator();
        while (iterator.hasNext()) {
            List<Claim> l;
            List<Claim> list = l = iterator.next();
            synchronized (list) {
                all.addAll(l);
            }
        }
        return all;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Claim> getClaimsOf(UUID playerId) {
        ArrayList<Claim> r = new ArrayList<Claim>();
        Iterator<List<Claim>> iterator = this.claimsByWorld.values().iterator();
        while (iterator.hasNext()) {
            List<Claim> l;
            List<Claim> list = l = iterator.next();
            synchronized (list) {
                for (Claim c : l) {
                    if (!c.isOwner(playerId)) continue;
                    r.add(c);
                }
            }
        }
        return r;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Claim> getClaimsInWorld(String dim) {
        List l;
        List list = l = this.claimsByWorld.getOrDefault(dim, Collections.emptyList());
        synchronized (list) {
            return new ArrayList<Claim>(l);
        }
    }

    public void save() {
        if (this.server != null) {
            Path file = this.dataFile(this.server);
            try {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                for (Claim c : this.getAllClaims()) {
                    arr.add((JsonElement)c.toJson());
                }
                root.add("claims", (JsonElement)arr);
                JsonArray garr = new JsonArray();
                for (ClaimGroup g : this.groups.values()) {
                    garr.add((JsonElement)g.toJson());
                }
                root.add("groups", (JsonElement)garr);
                Files.createDirectories(file.getParent(), new FileAttribute[0]);
                Files.writeString(file, (CharSequence)GSON.toJson((JsonElement)root), StandardCharsets.UTF_8, new OpenOption[0]);
            }
            catch (IOException var6) {
                ClaimBlocksMod.LOGGER.error("Could not save claims to " + file, (Throwable)var6);
            }
        }
    }

    public void load(MinecraftServer server) {
        this.server = server;
        this.claimsByWorld.clear();
        this.claimIndex.clear();
        this.groups.clear();
        this.loadConfig(server);
        Path file = this.dataFile(server);
        if (!Files.exists(file, new LinkOption[0])) {
            ClaimBlocksMod.LOGGER.info("No existing claims file at {}, starting fresh.", (Object)file);
        } else {
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (text.isBlank()) {
                    return;
                }
                JsonElement el = JsonParser.parseString((String)text);
                if (!el.isJsonObject()) {
                    return;
                }
                JsonArray arr = el.getAsJsonObject().getAsJsonArray("claims");
                if (arr == null) {
                    return;
                }
                int count = 0;
                int migrated = 0;
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    boolean wasLegacy = !obj.has("radius") && obj.has("tier");
                    Claim c = Claim.fromJson(obj);
                    this.claimsByWorld.computeIfAbsent(c.getWorld(), k -> Collections.synchronizedList(new ArrayList())).add(c);
                    this.claimIndex.put(c.getClaimId(), c);
                    ++count;
                    if (!wasLegacy) continue;
                    ++migrated;
                }
                JsonArray garr = el.getAsJsonObject().getAsJsonArray("groups");
                if (garr != null) {
                    for (JsonElement ge : garr) {
                        ClaimGroup g = ClaimGroup.fromJson(ge.getAsJsonObject());
                        this.groups.put(g.getGroupId(), g);
                    }
                }
                // Sanea grupos huerfanos: si la nodriza ya no existe, desliga sus claims.
                java.util.List<UUID> dead = new ArrayList<UUID>();
                for (ClaimGroup g : this.groups.values()) {
                    if (g.getMotherClaimId() == null || this.claimIndex.get(g.getMotherClaimId()) == null) {
                        dead.add(g.getGroupId());
                    }
                }
                for (UUID gid : dead) {
                    this.groups.remove(gid);
                    for (Claim c : this.getAllClaims()) {
                        if (gid.equals(c.getGroupId())) {
                            c.setGroupId(null);
                        }
                    }
                }
                ClaimBlocksMod.LOGGER.info("Loaded {} claims from {} (migrated {} legacy)", new Object[]{count, file, migrated});
                if (migrated > 0) {
                    this.save();
                }
            }
            catch (Exception var13) {
                ClaimBlocksMod.LOGGER.error("Could not load claims from " + file, (Throwable)var13);
            }
        }
    }

    private void loadConfig(MinecraftServer s) {
        Path cfg = s.getWorldPath(LevelResource.ROOT).resolve(CONFIG_FILE);
        try {
            JsonObject o;
            if (!Files.exists(cfg, new LinkOption[0])) {
                JsonObject obj = new JsonObject();
                obj.addProperty("maxClaimsPerPlayer", (Number)0);
                obj.addProperty("_doc_maxClaimsPerPlayer", "0 = unlimited; max claims a non-OP player can own");
                Files.createDirectories(cfg.getParent(), new FileAttribute[0]);
                Files.writeString(cfg, (CharSequence)GSON.toJson((JsonElement)obj), StandardCharsets.UTF_8, new OpenOption[0]);
                return;
            }
            JsonElement el = JsonParser.parseString((String)Files.readString(cfg, StandardCharsets.UTF_8));
            if (el != null && el.isJsonObject() && (o = el.getAsJsonObject()).has("maxClaimsPerPlayer")) {
                ClaimManager.setMaxClaimsPerPlayer(o.get("maxClaimsPerPlayer").getAsInt());
            }
        }
        catch (Exception var5) {
            ClaimBlocksMod.LOGGER.error("Could not load config " + cfg, (Throwable)var5);
        }
    }

    private Path dataFile(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve(DATA_FILE);
    }

    public boolean isBypassing(UUID id) {
        return this.bypassPlayers.contains(id);
    }

    public boolean toggleBypass(UUID id) {
        if (this.bypassPlayers.contains(id)) {
            this.bypassPlayers.remove(id);
            return false;
        }
        this.bypassPlayers.add(id);
        return true;
    }

    public Set<UUID> getBypassPlayers() {
        return this.bypassPlayers;
    }

    public void queueMessage(UUID owner, Component msg) {
        this.pendingMessages.computeIfAbsent(owner, k -> Collections.synchronizedList(new ArrayList())).add(msg);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void flushPendingTo(ServerPlayer player) {
        List<Component> msgs = this.pendingMessages.remove(player.getUUID());
        if (msgs != null) {
            List<Component> list = msgs;
            synchronized (list) {
                for (Component t : msgs) {
                    player.displayClientMessage(t, false);
                }
            }
        }
    }

    public void onPlayerDisconnect(UUID id) {
        this.bypassPlayers.remove(id);
    }
}

