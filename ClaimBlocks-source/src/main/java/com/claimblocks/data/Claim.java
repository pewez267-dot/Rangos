/*
 * Decompiled with CFR 0.152.
 */
package com.claimblocks.data;

import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimTier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class Claim {
    private final UUID claimId;
    private UUID ownerUUID;
    private String ownerName;
    private String tierId;
    private final int radius;
    private final int height;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private long createdAt;
    private UUID groupId;
    private final List<UUID> members = new ArrayList<UUID>();
    private final List<String> memberNames = new ArrayList<String>();
    private final Set<UUID> bannedPlayers = new HashSet<UUID>();
    private final ClaimFlags flags = new ClaimFlags();

    public Claim(UUID claimId, UUID ownerUUID, String ownerName, String tierId, int radius, int height, String world, int x, int y, int z) {
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
        this.createdAt = System.currentTimeMillis();
    }

    public static Claim create(UUID owner, String ownerName, ClaimTier tier, String world, BlockPos pos) {
        return new Claim(UUID.randomUUID(), owner, ownerName, tier.id, tier.radius, tier.height, world, pos.getX(), pos.getY(), pos.getZ());
    }

    public UUID getClaimId() {
        return this.claimId;
    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public String getTierId() {
        return this.tierId;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getHeight() {
        return this.effectiveHeight();
    }

    public int getOwnHeight() {
        return this.height;
    }

    // ---- Grupo (unir protecciones) ----
    public UUID getGroupId() {
        return this.groupId;
    }

    public void setGroupId(UUID id) {
        this.groupId = id;
    }

    public boolean isGrouped() {
        return this.groupId != null;
    }

    public Claim getMother() {
        if (this.groupId == null) {
            return null;
        }
        return ClaimManager.getInstance().getMotherClaim(this.groupId);
    }

    public boolean isGroupMother() {
        if (this.groupId == null) {
            return false;
        }
        ClaimGroup g = ClaimManager.getInstance().getGroup(this.groupId);
        return g != null && this.claimId.equals(g.getMotherClaimId());
    }

    // Altura efectiva: si esta en un grupo y NO es la nodriza, usa la altura de la nodriza.
    public int effectiveHeight() {
        if (this.groupId != null) {
            Claim m = ClaimManager.getInstance().getMotherClaim(this.groupId);
            if (m != null && m != this) {
                return m.height;
            }
        }
        return this.height;
    }

    public String getWorld() {
        return this.world;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public BlockPos getCenter() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public List<UUID> getMembers() {
        return this.members;
    }

    public List<String> getMemberNames() {
        return this.memberNames;
    }

    public Set<UUID> getBannedPlayers() {
        return this.bannedPlayers;
    }

    // Si esta agrupada y NO es la nodriza, adopta las flags de la nodriza.
    public ClaimFlags getFlags() {
        if (this.groupId != null) {
            Claim m = ClaimManager.getInstance().getMotherClaim(this.groupId);
            if (m != null && m != this) {
                return m.flags;
            }
        }
        return this.flags;
    }

    // Flags PROPIAS (sin resolver por grupo) — para persistencia y para editar la nodriza.
    public ClaimFlags getOwnFlags() {
        return this.flags;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(long t) {
        this.createdAt = t;
    }

    public ClaimTier getTier() {
        ClaimTier t;
        return this.tierId != null && (t = ClaimTier.byId(this.tierId)) != null ? t : ClaimTier.closestMatch(this.radius, this.height);
    }

    public String sizeLabel() {
        if (this.tierId != null && this.tierId.startsWith("claimstone_")) {
            return this.tierId.substring("claimstone_".length());
        }
        ClaimTier t = this.getTier();
        return t == null ? this.radius + "x" + this.radius : t.label();
    }

    public boolean contains(BlockPos pos) {
        int h = this.effectiveHeight();
        return Math.abs(pos.getX() - this.x) <= this.radius && Math.abs(pos.getZ() - this.z) <= this.radius && pos.getY() - this.y <= h && this.y - pos.getY() <= h;
    }

    public boolean overlapsWith(BlockPos otherCenter, int otherRadius, int otherHeight) {
        return Math.abs(otherCenter.getX() - this.x) < this.radius + otherRadius && Math.abs(otherCenter.getZ() - this.z) < this.radius + otherRadius && Math.abs(otherCenter.getY() - this.y) < this.height + otherHeight;
    }

    public AABB getBoundingBox() {
        return new AABB((double)(this.x - this.radius), (double)(this.y - this.height), (double)(this.z - this.radius), (double)(this.x + this.radius + 1), (double)(this.y + this.height + 1), (double)(this.z + this.radius + 1));
    }

    public boolean isOwner(UUID id) {
        return this.ownerUUID != null && this.ownerUUID.equals(id);
    }

    public boolean isOwner(Player p) {
        return this.isOwner(p.getUUID());
    }

    public boolean isMember(UUID id) {
        return this.members.contains(id);
    }

    public boolean isMember(Player p) {
        return this.isMember(p.getUUID());
    }

    public boolean isBanned(UUID id) {
        if (this.groupId != null) {
            Claim m = ClaimManager.getInstance().getMotherClaim(this.groupId);
            if (m != null && m != this) {
                return m.bannedPlayers.contains(id);
            }
        }
        return this.bannedPlayers.contains(id);
    }

    public boolean canModify(Player p) {
        if (this.isOwner(p) || this.isMember(p) || p.hasPermissions(2)) {
            return true;
        }
        // Cualquier jugador registrado en el grupo puede modificar toda la zona unida.
        if (this.groupId != null) {
            ClaimGroup g = ClaimManager.getInstance().getGroup(this.groupId);
            if (g != null && g.isRegistered(p.getUUID())) {
                return true;
            }
        }
        return false;
    }

    public void addMember(UUID id, String name) {
        if (!this.members.contains(id)) {
            this.members.add(id);
            this.memberNames.add(name == null ? "" : name);
        }
    }

    public void removeMember(UUID id) {
        int idx = this.members.indexOf(id);
        if (idx >= 0) {
            this.members.remove(idx);
            if (idx < this.memberNames.size()) {
                this.memberNames.remove(idx);
            }
        }
    }

    public void banPlayer(UUID id) {
        this.bannedPlayers.add(id);
        this.removeMember(id);
    }

    public void unbanPlayer(UUID id) {
        this.bannedPlayers.remove(id);
    }

    public void setOwner(UUID id, String name) {
        this.ownerUUID = id;
        this.ownerName = name;
    }

    public void setTierId(String id) {
        this.tierId = id;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("claimId", this.claimId.toString());
        o.addProperty("ownerUUID", this.ownerUUID == null ? "" : this.ownerUUID.toString());
        o.addProperty("ownerName", this.ownerName == null ? "" : this.ownerName);
        if (this.tierId != null) {
            o.addProperty("tierId", this.tierId);
        }
        o.addProperty("radius", (Number)this.radius);
        o.addProperty("height", (Number)this.height);
        o.addProperty("world", this.world);
        o.addProperty("x", (Number)this.x);
        o.addProperty("y", (Number)this.y);
        o.addProperty("z", (Number)this.z);
        o.addProperty("createdAt", (Number)this.createdAt);
        if (this.groupId != null) {
            o.addProperty("groupId", this.groupId.toString());
        }
        JsonArray mem = new JsonArray();
        for (UUID uUID : this.members) {
            mem.add(uUID.toString());
        }
        o.add("members", (JsonElement)mem);
        JsonArray memN = new JsonArray();
        for (String string : this.memberNames) {
            memN.add(string == null ? "" : string);
        }
        o.add("memberNames", (JsonElement)memN);
        JsonArray jsonArray = new JsonArray();
        for (UUID id : this.bannedPlayers) {
            jsonArray.add(id.toString());
        }
        o.add("bannedPlayers", (JsonElement)jsonArray);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("blockBuilding", Boolean.valueOf(this.flags.blockBuilding));
        jsonObject.addProperty("blockBreaking", Boolean.valueOf(this.flags.blockBreaking));
        jsonObject.addProperty("blockExplosions", Boolean.valueOf(this.flags.blockExplosions));
        jsonObject.addProperty("blockFire", Boolean.valueOf(this.flags.blockFire));
        jsonObject.addProperty("blockMobSpawn", Boolean.valueOf(this.flags.blockMobSpawn));
        jsonObject.addProperty("blockPVP", Boolean.valueOf(this.flags.blockPVP));
        jsonObject.addProperty("blockMobDamage", Boolean.valueOf(this.flags.blockMobDamage));
        jsonObject.addProperty("trespasserAlerts", Boolean.valueOf(this.flags.trespasserAlerts));
        jsonObject.addProperty("blockItemUse", Boolean.valueOf(this.flags.blockItemUse));
        jsonObject.addProperty("blockEntityInteract", Boolean.valueOf(this.flags.blockEntityInteract));
        jsonObject.addProperty("blockTrampling", Boolean.valueOf(this.flags.blockTrampling));
        jsonObject.addProperty("blockFluids", Boolean.valueOf(this.flags.blockFluids));
        jsonObject.addProperty("pvpAll", Boolean.valueOf(this.flags.pvpAll));
        jsonObject.addProperty("blockTreeChopping", Boolean.valueOf(this.flags.blockTreeChopping));
        jsonObject.addProperty("publicMode", Boolean.valueOf(this.flags.publicMode));
        jsonObject.addProperty("showWelcome", Boolean.valueOf(this.flags.showWelcome));
        jsonObject.addProperty("welcomeMessage", this.flags.welcomeMessage == null ? "" : this.flags.welcomeMessage);
        jsonObject.addProperty("showLeave", Boolean.valueOf(this.flags.showLeave));
        jsonObject.addProperty("leaveMessage", this.flags.leaveMessage == null ? "" : this.flags.leaveMessage);
        jsonObject.addProperty("showBorder", Boolean.valueOf(this.flags.showBorder));
        jsonObject.addProperty("showParticles", Boolean.valueOf(this.flags.showParticles));
        jsonObject.addProperty("borderParticle", this.flags.borderParticle == null ? "happy" : this.flags.borderParticle);
        jsonObject.addProperty("particleDensity", (Number)this.flags.particleDensity);
        jsonObject.addProperty("burnHostiles", Boolean.valueOf(this.flags.burnHostiles));
        jsonObject.addProperty("effectRegeneration", Boolean.valueOf(this.flags.effectRegeneration));
        jsonObject.addProperty("effectResistance", Boolean.valueOf(this.flags.effectResistance));
        jsonObject.addProperty("effectSpeed", Boolean.valueOf(this.flags.effectSpeed));
        jsonObject.addProperty("blockAnimalKilling", Boolean.valueOf(this.flags.blockAnimalKilling));
        jsonObject.addProperty("blockChestAccess", Boolean.valueOf(this.flags.blockChestAccess));
        jsonObject.addProperty("blockCropHarvest", Boolean.valueOf(this.flags.blockCropHarvest));
        jsonObject.addProperty("blockAnvilUse", Boolean.valueOf(this.flags.blockAnvilUse));
        jsonObject.addProperty("blockEnderPearl", Boolean.valueOf(this.flags.blockEnderPearl));
        jsonObject.addProperty("blockSignEditing", Boolean.valueOf(this.flags.blockSignEditing));
        jsonObject.addProperty("allowFlight", Boolean.valueOf(this.flags.allowFlight));
        jsonObject.addProperty("blockDoorsAccess", Boolean.valueOf(this.flags.blockDoorsAccess));
        o.add("flags", (JsonElement)jsonObject);
        return o;
    }

    public static Claim fromJson(JsonObject o) {
        JsonArray arr;
        String tierId;
        int height;
        int radius;
        UUID id = o.has("claimId") ? UUID.fromString(o.get("claimId").getAsString()) : UUID.randomUUID();
        UUID owner = o.has("ownerUUID") && !o.get("ownerUUID").getAsString().isEmpty() ? UUID.fromString(o.get("ownerUUID").getAsString()) : null;
        String ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
        String world = o.has("world") ? o.get("world").getAsString() : "minecraft:overworld";
        int x = o.get("x").getAsInt();
        int y = o.get("y").getAsInt();
        int z = o.get("z").getAsInt();
        if (o.has("radius") && o.has("height")) {
            radius = o.get("radius").getAsInt();
            height = o.get("height").getAsInt();
            tierId = o.has("tierId") ? o.get("tierId").getAsString() : null;
        } else if (o.has("tier")) {
            int legacy = o.get("tier").getAsInt();
            ClaimTier t = ClaimTier.byLegacyTier(legacy);
            if (t == null) {
                t = ClaimTier.VALUES[0];
            }
            radius = t.radius;
            height = t.height;
            tierId = t.id;
        } else {
            radius = 10;
            height = 15;
            tierId = "claimstone_10x10";
        }
        Claim c = new Claim(id, owner, ownerName, tierId, radius, height, world, x, y, z);
        long l = c.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : 0L;
        if (o.has("groupId") && !o.get("groupId").getAsString().isEmpty()) {
            c.groupId = UUID.fromString(o.get("groupId").getAsString());
        }
        if (o.has("members")) {
            arr = o.getAsJsonArray("members");
            JsonArray names = o.has("memberNames") ? o.getAsJsonArray("memberNames") : new JsonArray();
            for (int i = 0; i < arr.size(); ++i) {
                UUID mid = UUID.fromString(arr.get(i).getAsString());
                String mname = i < names.size() ? names.get(i).getAsString() : "";
                c.addMember(mid, mname);
            }
        }
        if (o.has("bannedPlayers")) {
            arr = o.getAsJsonArray("bannedPlayers");
            for (int i = 0; i < arr.size(); ++i) {
                c.bannedPlayers.add(UUID.fromString(arr.get(i).getAsString()));
            }
        }
        if (o.has("flags")) {
            JsonObject f = o.getAsJsonObject("flags");
            Claim.applyBool(f, "blockBuilding", v -> {
                c.flags.blockBuilding = v;
            });
            Claim.applyBool(f, "blockBreaking", v -> {
                c.flags.blockBreaking = v;
            });
            Claim.applyBool(f, "blockExplosions", v -> {
                c.flags.blockExplosions = v;
            });
            Claim.applyBool(f, "blockFire", v -> {
                c.flags.blockFire = v;
            });
            Claim.applyBool(f, "blockMobSpawn", v -> {
                c.flags.blockMobSpawn = v;
            });
            Claim.applyBool(f, "blockPVP", v -> {
                c.flags.blockPVP = v;
            });
            Claim.applyBool(f, "blockMobDamage", v -> {
                c.flags.blockMobDamage = v;
            });
            Claim.applyBool(f, "trespasserAlerts", v -> {
                c.flags.trespasserAlerts = v;
            });
            Claim.applyBool(f, "blockItemUse", v -> {
                c.flags.blockItemUse = v;
            });
            Claim.applyBool(f, "blockEntityInteract", v -> {
                c.flags.blockEntityInteract = v;
            });
            Claim.applyBool(f, "blockTrampling", v -> {
                c.flags.blockTrampling = v;
            });
            Claim.applyBool(f, "blockFluids", v -> {
                c.flags.blockFluids = v;
            });
            Claim.applyBool(f, "pvpAll", v -> {
                c.flags.pvpAll = v;
            });
            Claim.applyBool(f, "blockTreeChopping", v -> {
                c.flags.blockTreeChopping = v;
            });
            Claim.applyBool(f, "publicMode", v -> {
                c.flags.publicMode = v;
            });
            Claim.applyBool(f, "showWelcome", v -> {
                c.flags.showWelcome = v;
            });
            if (f.has("welcomeMessage")) {
                c.flags.welcomeMessage = f.get("welcomeMessage").getAsString();
            }
            Claim.applyBool(f, "showLeave", v -> {
                c.flags.showLeave = v;
            });
            if (f.has("leaveMessage")) {
                c.flags.leaveMessage = f.get("leaveMessage").getAsString();
            }
            Claim.applyBool(f, "showBorder", v -> {
                c.flags.showBorder = v;
            });
            Claim.applyBool(f, "showParticles", v -> {
                c.flags.showParticles = v;
            });
            if (f.has("borderParticle")) {
                c.flags.borderParticle = f.get("borderParticle").getAsString();
            }
            if (f.has("particleDensity")) {
                c.flags.particleDensity = f.get("particleDensity").getAsInt();
            }
            Claim.applyBool(f, "burnHostiles", v -> {
                c.flags.burnHostiles = v;
            });
            Claim.applyBool(f, "effectRegeneration", v -> {
                c.flags.effectRegeneration = v;
            });
            Claim.applyBool(f, "effectResistance", v -> {
                c.flags.effectResistance = v;
            });
            Claim.applyBool(f, "effectSpeed", v -> {
                c.flags.effectSpeed = v;
            });
            Claim.applyBool(f, "blockAnimalKilling", v -> {
                c.flags.blockAnimalKilling = v;
            });
            Claim.applyBool(f, "blockChestAccess", v -> {
                c.flags.blockChestAccess = v;
            });
            Claim.applyBool(f, "blockCropHarvest", v -> {
                c.flags.blockCropHarvest = v;
            });
            Claim.applyBool(f, "blockAnvilUse", v -> {
                c.flags.blockAnvilUse = v;
            });
            Claim.applyBool(f, "blockEnderPearl", v -> {
                c.flags.blockEnderPearl = v;
            });
            Claim.applyBool(f, "blockSignEditing", v -> {
                c.flags.blockSignEditing = v;
            });
            Claim.applyBool(f, "allowFlight", v -> {
                c.flags.allowFlight = v;
            });
            Claim.applyBool(f, "blockDoorsAccess", v -> {
                c.flags.blockDoorsAccess = v;
            });
        }
        return c;
    }

    private static void applyBool(JsonObject f, String key, BoolSetter setter) {
        if (f.has(key)) {
            setter.set(f.get(key).getAsBoolean());
        }
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

