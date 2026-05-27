package com.claimblocks.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Single claim. Now stores a horizontal {@code radius} (X/Z) and a vertical
 * {@code height} (Y, applied symmetrically up and down) instead of a single
 * tier id, so persistence is forward-compatible if tiers change later.
 *
 * The {@link ClaimTier} the claim was created from is also kept (when known)
 * so the menu/visualisation can show the right colour.
 */
public class Claim {
    private final UUID claimId;
    private UUID ownerUUID;
    private String ownerName;
    /** Original tier id ("claimstone_NxN"), may be null for migrated old data. */
    private String tierId;
    private final int radius;
    private final int height;
    private final String world;
    private final int x, y, z;
    private final List<UUID> members = new ArrayList<>();
    private final List<String> memberNames = new ArrayList<>();
    private final Set<UUID> bannedPlayers = new HashSet<>();
    private final ClaimFlags flags = new ClaimFlags();

    public Claim(UUID claimId, UUID ownerUUID, String ownerName, String tierId,
                 int radius, int height, String world, int x, int y, int z) {
        this.claimId = claimId;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.tierId = tierId;
        this.radius = radius;
        this.height = height;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Claim create(UUID owner, String ownerName, ClaimTier tier,
                               String world, BlockPos pos) {
        return new Claim(UUID.randomUUID(), owner, ownerName, tier.id,
            tier.radius, tier.height, world, pos.getX(), pos.getY(), pos.getZ());
    }

    public UUID getClaimId()       { return claimId; }
    public UUID getOwnerUUID()     { return ownerUUID; }
    public String getOwnerName()   { return ownerName; }
    public String getTierId()      { return tierId; }
    public int getRadius()         { return radius; }
    public int getHeight()         { return height; }
    public String getWorld()       { return world; }
    public int getX()              { return x; }
    public int getY()              { return y; }
    public int getZ()              { return z; }
    public BlockPos getCenter()    { return new BlockPos(x, y, z); }
    public List<UUID> getMembers() { return members; }
    public List<String> getMemberNames() { return memberNames; }
    public Set<UUID> getBannedPlayers()  { return bannedPlayers; }
    public ClaimFlags getFlags()   { return flags; }

    public ClaimTier getTier() {
        if (tierId != null) {
            ClaimTier t = ClaimTier.byId(tierId);
            if (t != null) return t;
        }
        return ClaimTier.closestMatch(radius, height);
    }

    /** Human-readable size label, e.g. "100x100". */
    public String sizeLabel() {
        int side = radius * 2;
        return side + "x" + side;
    }

    /** Returns true if pos is inside the protected prism. */
    public boolean contains(BlockPos pos) {
        return Math.abs(pos.getX() - x) <= radius
            && Math.abs(pos.getZ() - z) <= radius
            && (pos.getY() - y) <= height
            && (y - pos.getY()) <= height;
    }

    public boolean overlapsWith(BlockPos otherCenter, int otherRadius, int otherHeight) {
        return Math.abs(otherCenter.getX() - x) <= (radius + otherRadius)
            && Math.abs(otherCenter.getZ() - z) <= (radius + otherRadius)
            && Math.abs(otherCenter.getY() - y) <= (height + otherHeight);
    }

    public Box getBoundingBox() {
        return new Box(x - radius, y - height, z - radius,
                       x + radius + 1, y + height + 1, z + radius + 1);
    }

    public boolean isOwner(UUID id)        { return ownerUUID != null && ownerUUID.equals(id); }
    public boolean isOwner(PlayerEntity p) { return isOwner(p.getUuid()); }
    public boolean isMember(UUID id)       { return members.contains(id); }
    public boolean isMember(PlayerEntity p){ return isMember(p.getUuid()); }
    public boolean isBanned(UUID id)       { return bannedPlayers.contains(id); }

    public boolean canModify(PlayerEntity p) {
        return isOwner(p) || isMember(p) || p.hasPermissionLevel(2);
    }

    public void addMember(UUID id, String name) {
        if (!members.contains(id)) {
            members.add(id);
            memberNames.add(name == null ? "" : name);
        }
    }

    public void removeMember(UUID id) {
        int idx = members.indexOf(id);
        if (idx >= 0) {
            members.remove(idx);
            if (idx < memberNames.size()) memberNames.remove(idx);
        }
    }

    public void banPlayer(UUID id) {
        bannedPlayers.add(id);
        removeMember(id);
    }

    public void unbanPlayer(UUID id) {
        bannedPlayers.remove(id);
    }

    public void setOwner(UUID id, String name) {
        this.ownerUUID = id;
        this.ownerName = name;
    }

    public void setTierId(String id) { this.tierId = id; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("claimId", claimId.toString());
        o.addProperty("ownerUUID", ownerUUID == null ? "" : ownerUUID.toString());
        o.addProperty("ownerName", ownerName == null ? "" : ownerName);
        if (tierId != null) o.addProperty("tierId", tierId);
        o.addProperty("radius", radius);
        o.addProperty("height", height);
        o.addProperty("world", world);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        JsonArray mem = new JsonArray();
        for (UUID m : members) mem.add(m.toString());
        o.add("members", mem);
        JsonArray memN = new JsonArray();
        for (String n : memberNames) memN.add(n == null ? "" : n);
        o.add("memberNames", memN);
        JsonArray ban = new JsonArray();
        for (UUID id : bannedPlayers) ban.add(id.toString());
        o.add("bannedPlayers", ban);
        JsonObject f = new JsonObject();
        f.addProperty("blockBuilding",       flags.blockBuilding);
        f.addProperty("blockBreaking",       flags.blockBreaking);
        f.addProperty("blockExplosions",     flags.blockExplosions);
        f.addProperty("blockFire",           flags.blockFire);
        f.addProperty("blockMobSpawn",       flags.blockMobSpawn);
        f.addProperty("blockPVP",            flags.blockPVP);
        f.addProperty("blockMobDamage",      flags.blockMobDamage);
        f.addProperty("trespasserAlerts",    flags.trespasserAlerts);
        f.addProperty("blockItemUse",        flags.blockItemUse);
        f.addProperty("blockEntityInteract", flags.blockEntityInteract);
        f.addProperty("blockTrampling",      flags.blockTrampling);
        f.addProperty("blockFluids",         flags.blockFluids);
        f.addProperty("pvpAll",              flags.pvpAll);
        f.addProperty("blockTreeChopping",   flags.blockTreeChopping);
        f.addProperty("publicMode",          flags.publicMode);
        f.addProperty("showWelcome",         flags.showWelcome);
        f.addProperty("welcomeMessage",      flags.welcomeMessage == null ? "" : flags.welcomeMessage);
        o.add("flags", f);
        return o;
    }

    /**
     * Loads a claim from JSON. Auto-migrates v2.x records that used a
     * "tier" integer 1-5 into the new (radius, height) format.
     */
    public static Claim fromJson(JsonObject o) {
        UUID id = o.has("claimId") ? UUID.fromString(o.get("claimId").getAsString()) : UUID.randomUUID();
        UUID owner = o.has("ownerUUID") && !o.get("ownerUUID").getAsString().isEmpty()
            ? UUID.fromString(o.get("ownerUUID").getAsString()) : null;
        String ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
        String world = o.has("world") ? o.get("world").getAsString() : "minecraft:overworld";
        int x = o.get("x").getAsInt();
        int y = o.get("y").getAsInt();
        int z = o.get("z").getAsInt();

        int radius;
        int height;
        String tierId;
        if (o.has("radius") && o.has("height")) {
            radius = o.get("radius").getAsInt();
            height = o.get("height").getAsInt();
            tierId = o.has("tierId") ? o.get("tierId").getAsString() : null;
        } else if (o.has("tier")) {
            // Legacy v2.x record - migrate
            int legacy = o.get("tier").getAsInt();
            ClaimTier t = ClaimTier.byLegacyTier(legacy);
            if (t == null) t = ClaimTier.VALUES[0];
            radius = t.radius;
            height = t.height;
            tierId = t.id;
        } else {
            radius = 10;
            height = 15;
            tierId = "claimstone_10x10";
        }

        Claim c = new Claim(id, owner, ownerName, tierId, radius, height, world, x, y, z);
        if (o.has("members")) {
            JsonArray arr = o.getAsJsonArray("members");
            JsonArray names = o.has("memberNames") ? o.getAsJsonArray("memberNames") : new JsonArray();
            for (int i = 0; i < arr.size(); i++) {
                UUID mid = UUID.fromString(arr.get(i).getAsString());
                String mname = i < names.size() ? names.get(i).getAsString() : "";
                c.addMember(mid, mname);
            }
        }
        if (o.has("bannedPlayers")) {
            JsonArray arr = o.getAsJsonArray("bannedPlayers");
            for (int i = 0; i < arr.size(); i++) {
                c.bannedPlayers.add(UUID.fromString(arr.get(i).getAsString()));
            }
        }
        if (o.has("flags")) {
            JsonObject f = o.getAsJsonObject("flags");
            if (f.has("blockBuilding"))       c.flags.blockBuilding       = f.get("blockBuilding").getAsBoolean();
            if (f.has("blockBreaking"))       c.flags.blockBreaking       = f.get("blockBreaking").getAsBoolean();
            if (f.has("blockExplosions"))     c.flags.blockExplosions     = f.get("blockExplosions").getAsBoolean();
            if (f.has("blockFire"))           c.flags.blockFire           = f.get("blockFire").getAsBoolean();
            if (f.has("blockMobSpawn"))       c.flags.blockMobSpawn       = f.get("blockMobSpawn").getAsBoolean();
            if (f.has("blockPVP"))            c.flags.blockPVP            = f.get("blockPVP").getAsBoolean();
            if (f.has("blockMobDamage"))      c.flags.blockMobDamage      = f.get("blockMobDamage").getAsBoolean();
            if (f.has("trespasserAlerts"))    c.flags.trespasserAlerts    = f.get("trespasserAlerts").getAsBoolean();
            if (f.has("blockItemUse"))        c.flags.blockItemUse        = f.get("blockItemUse").getAsBoolean();
            if (f.has("blockEntityInteract")) c.flags.blockEntityInteract = f.get("blockEntityInteract").getAsBoolean();
            if (f.has("blockTrampling"))      c.flags.blockTrampling      = f.get("blockTrampling").getAsBoolean();
            if (f.has("blockFluids"))         c.flags.blockFluids         = f.get("blockFluids").getAsBoolean();
            if (f.has("pvpAll"))              c.flags.pvpAll              = f.get("pvpAll").getAsBoolean();
            if (f.has("blockTreeChopping"))   c.flags.blockTreeChopping   = f.get("blockTreeChopping").getAsBoolean();
            if (f.has("publicMode"))          c.flags.publicMode          = f.get("publicMode").getAsBoolean();
            if (f.has("showWelcome"))         c.flags.showWelcome         = f.get("showWelcome").getAsBoolean();
            if (f.has("welcomeMessage"))      c.flags.welcomeMessage      = f.get("welcomeMessage").getAsString();
        }
        return c;
    }
}
