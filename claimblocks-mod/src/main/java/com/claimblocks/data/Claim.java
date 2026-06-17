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
 * A single placed claim. Persistable to/from JSON via {@link ClaimManager}.
 *
 * The claim ID is a UUID assigned at construction; the (world, x, y, z)
 * tuple identifies the actual block location in-world.
 */
public class Claim {
    private final UUID claimId;
    private UUID ownerUUID;
    private String ownerName;
    private final int tier;
    private final String world;       // dimension id, e.g. "minecraft:overworld"
    private final int x, y, z;        // centre block pos
    private final List<UUID> members = new ArrayList<>();
    private final List<String> memberNames = new ArrayList<>();
    private final Set<UUID> bannedPlayers = new HashSet<>();
    private final ClaimFlags flags = new ClaimFlags();

    public Claim(UUID claimId, UUID ownerUUID, String ownerName,
                 int tier, String world, int x, int y, int z) {
        this.claimId = claimId;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.tier = tier;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Claim create(UUID ownerUUID, String ownerName, int tier,
                               String world, BlockPos pos) {
        return new Claim(UUID.randomUUID(), ownerUUID, ownerName, tier, world,
                pos.getX(), pos.getY(), pos.getZ());
    }

    public UUID getClaimId()       { return claimId; }
    public UUID getOwnerUUID()     { return ownerUUID; }
    public String getOwnerName()   { return ownerName; }
    public int getTier()           { return tier; }
    public int getRadius()         { return tierRadius(tier); }
    public String getWorld()       { return world; }
    public int getX()              { return x; }
    public int getY()              { return y; }
    public int getZ()              { return z; }
    public BlockPos getCenter()    { return new BlockPos(x, y, z); }
    public List<UUID> getMembers() { return members; }
    public List<String> getMemberNames() { return memberNames; }
    public Set<UUID> getBannedPlayers()  { return bannedPlayers; }
    public ClaimFlags getFlags()   { return flags; }

    public void setOwner(UUID id, String name) { this.ownerUUID = id; this.ownerName = name; }

    public static int tierRadius(int tier) {
        return switch (tier) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            case 4 -> 40;
            case 5 -> 50;
            default -> 0;
        };
    }

    public boolean contains(BlockPos pos) {
        int r = getRadius();
        return Math.abs(pos.getX() - x) <= r
            && Math.abs(pos.getY() - y) <= r
            && Math.abs(pos.getZ() - z) <= r;
    }

    public boolean overlapsWith(BlockPos otherCenter, int otherRadius) {
        int r = getRadius() + otherRadius;
        return Math.abs(otherCenter.getX() - x) <= r
            && Math.abs(otherCenter.getY() - y) <= r
            && Math.abs(otherCenter.getZ() - z) <= r;
    }

    public Box getBoundingBox() {
        int r = getRadius();
        return new Box(x - r, y - r, z - r, x + r + 1, y + r + 1, z + r + 1);
    }

    public boolean isOwner(UUID id)   { return ownerUUID != null && ownerUUID.equals(id); }
    public boolean isOwner(PlayerEntity p) { return isOwner(p.getUuid()); }
    public boolean isMember(UUID id)  { return members.contains(id); }
    public boolean isMember(PlayerEntity p) { return isMember(p.getUuid()); }
    public boolean isBanned(UUID id)  { return bannedPlayers.contains(id); }

    /** Owner, member, or operator (perm level 2) can administer. */
    public boolean canAdminister(PlayerEntity p) {
        return isOwner(p) || isMember(p) || p.hasPermissionLevel(2);
    }

    /** Owner OR member can do protected actions. */
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

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("claimId", claimId.toString());
        o.addProperty("ownerUUID", ownerUUID == null ? "" : ownerUUID.toString());
        o.addProperty("ownerName", ownerName == null ? "" : ownerName);
        o.addProperty("tier", tier);
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
        f.addProperty("blockBuilding",   flags.blockBuilding);
        f.addProperty("blockBreaking",   flags.blockBreaking);
        f.addProperty("blockExplosions", flags.blockExplosions);
        f.addProperty("blockFire",       flags.blockFire);
        f.addProperty("blockMobSpawn",   flags.blockMobSpawn);
        f.addProperty("blockPVP",        flags.blockPVP);
        f.addProperty("blockMobDamage",  flags.blockMobDamage);
        f.addProperty("trespasserAlerts", flags.trespasserAlerts);
        o.add("flags", f);
        return o;
    }

    public static Claim fromJson(JsonObject o) {
        UUID id = o.has("claimId") ? UUID.fromString(o.get("claimId").getAsString()) : UUID.randomUUID();
        UUID owner = o.has("ownerUUID") && !o.get("ownerUUID").getAsString().isEmpty()
            ? UUID.fromString(o.get("ownerUUID").getAsString()) : null;
        String ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
        int tier = o.get("tier").getAsInt();
        String world = o.has("world") ? o.get("world").getAsString() : "minecraft:overworld";
        int x = o.get("x").getAsInt();
        int y = o.get("y").getAsInt();
        int z = o.get("z").getAsInt();
        Claim c = new Claim(id, owner, ownerName, tier, world, x, y, z);
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
            if (f.has("blockBuilding"))    c.flags.blockBuilding    = f.get("blockBuilding").getAsBoolean();
            if (f.has("blockBreaking"))    c.flags.blockBreaking    = f.get("blockBreaking").getAsBoolean();
            if (f.has("blockExplosions"))  c.flags.blockExplosions  = f.get("blockExplosions").getAsBoolean();
            if (f.has("blockFire"))        c.flags.blockFire        = f.get("blockFire").getAsBoolean();
            if (f.has("blockMobSpawn"))    c.flags.blockMobSpawn    = f.get("blockMobSpawn").getAsBoolean();
            if (f.has("blockPVP"))         c.flags.blockPVP         = f.get("blockPVP").getAsBoolean();
            if (f.has("blockMobDamage"))   c.flags.blockMobDamage   = f.get("blockMobDamage").getAsBoolean();
            if (f.has("trespasserAlerts")) c.flags.trespasserAlerts = f.get("trespasserAlerts").getAsBoolean();
        }
        return c;
    }
}
