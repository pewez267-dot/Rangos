package com.claimblocks.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory representation of a single placed claim. Persisted as NBT through
 * {@link ClaimManager}.
 */
public class Claim {
    private final BlockPos center;
    private UUID ownerId;
    private String ownerName;
    private final int tier;
    private final int radius;
    private final String dimension;
    private final List<UUID> members = new ArrayList<>();
    private final List<String> memberNames = new ArrayList<>();
    private final Set<UUID> banned = new HashSet<>();
    private final ClaimFlags flags;

    public Claim(BlockPos center, UUID ownerId, String ownerName, int tier, int radius, String dimension) {
        this.center = center.toImmutable();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.tier = tier;
        this.radius = radius;
        this.dimension = dimension;
        this.flags = new ClaimFlags();
    }

    public boolean containsPosition(BlockPos pos) {
        return Math.abs(pos.getX() - center.getX()) <= radius
            && Math.abs(pos.getY() - center.getY()) <= radius
            && Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    /** Axis-aligned bounding box of the protected cube. */
    public Box getBoundingBox() {
        return new Box(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius + 1, center.getY() + radius + 1, center.getZ() + radius + 1
        );
    }

    public boolean overlaps(BlockPos otherCenter, int otherRadius) {
        return Math.abs(otherCenter.getX() - center.getX()) <= (radius + otherRadius)
            && Math.abs(otherCenter.getY() - center.getY()) <= (radius + otherRadius)
            && Math.abs(otherCenter.getZ() - center.getZ()) <= (radius + otherRadius);
    }

    public boolean isOwner(PlayerEntity player) {
        return ownerId != null && ownerId.equals(player.getUuid());
    }

    public boolean isOwner(UUID id) {
        return ownerId != null && ownerId.equals(id);
    }

    public boolean isMember(PlayerEntity player) {
        return members.contains(player.getUuid());
    }

    public boolean isBanned(UUID id) {
        return banned.contains(id);
    }

    public boolean canModify(PlayerEntity player) {
        return isOwner(player) || isMember(player) || player.hasPermissionLevel(2);
    }

    public void addMember(UUID uuid, String name) {
        if (!members.contains(uuid)) {
            members.add(uuid);
            memberNames.add(name);
        }
    }

    public void removeMember(UUID uuid) {
        int idx = members.indexOf(uuid);
        if (idx >= 0) {
            members.remove(idx);
            if (idx < memberNames.size()) memberNames.remove(idx);
        }
    }

    public void banPlayer(UUID uuid) {
        banned.add(uuid);
        removeMember(uuid);
    }

    public void unbanPlayer(UUID uuid) {
        banned.remove(uuid);
    }

    public List<UUID> getMembers() { return members; }
    public List<String> getMemberNames() { return memberNames; }
    public Set<UUID> getBanned() { return banned; }
    public ClaimFlags getFlags() { return flags; }
    public BlockPos getCenter() { return center; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getOwnerUUID() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public int getTier() { return tier; }
    public int getRadius() { return radius; }
    public String getDimension() { return dimension; }

    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("centerX", center.getX());
        nbt.putInt("centerY", center.getY());
        nbt.putInt("centerZ", center.getZ());
        if (ownerId != null) {
            nbt.putUuid("ownerId", ownerId);
        }
        nbt.putString("ownerName", ownerName == null ? "" : ownerName);
        nbt.putInt("tier", tier);
        nbt.putInt("radius", radius);
        nbt.putString("dimension", dimension);

        NbtList memberList = new NbtList();
        for (UUID m : members) {
            NbtCompound c = new NbtCompound();
            c.putUuid("id", m);
            memberList.add(c);
        }
        nbt.put("members", memberList);

        NbtList nameList = new NbtList();
        for (String n : memberNames) {
            nameList.add(NbtString.of(n == null ? "" : n));
        }
        nbt.put("memberNames", nameList);

        NbtList bannedList = new NbtList();
        for (UUID b : banned) {
            NbtCompound c = new NbtCompound();
            c.putUuid("id", b);
            bannedList.add(c);
        }
        nbt.put("banned", bannedList);

        nbt.put("flags", flags.toNbt());
        return nbt;
    }

    public static Claim fromNbt(NbtCompound nbt) {
        BlockPos center = new BlockPos(
            nbt.getInt("centerX"),
            nbt.getInt("centerY"),
            nbt.getInt("centerZ")
        );
        UUID ownerId = nbt.containsUuid("ownerId") ? nbt.getUuid("ownerId") : null;
        String ownerName = nbt.getString("ownerName");
        int tier = nbt.getInt("tier");
        int radius = nbt.getInt("radius");
        String dim = nbt.getString("dimension");
        Claim claim = new Claim(center, ownerId, ownerName, tier, radius, dim);

        NbtList memberList = nbt.getList("members", NbtElement.COMPOUND_TYPE);
        NbtList nameList = nbt.getList("memberNames", NbtElement.STRING_TYPE);
        for (int i = 0; i < memberList.size(); i++) {
            UUID id = memberList.getCompound(i).getUuid("id");
            String name = i < nameList.size() ? nameList.getString(i) : "";
            claim.addMember(id, name);
        }

        NbtList bannedList = nbt.getList("banned", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < bannedList.size(); i++) {
            claim.banned.add(bannedList.getCompound(i).getUuid("id"));
        }

        if (nbt.contains("flags")) {
            ClaimFlags loaded = ClaimFlags.fromNbt(nbt.getCompound("flags"));
            // Copy values into the existing flags object since Claim.flags is final
            claim.flags.setCreeping(loaded.isCreeping());
            claim.flags.setBreaking(loaded.isBreaking());
            claim.flags.setExplosions(loaded.isExplosions());
            claim.flags.setFire(loaded.isFire());
            claim.flags.setMobs(loaded.isMobs());
            claim.flags.setPvp(loaded.isPvp());
            claim.flags.setMobDamage(loaded.isMobDamage());
            claim.flags.setTrespasserAlerts(loaded.isTrespasserAlerts());
        }
        return claim;
    }

    @Override
    public String toString() {
        return "Claim{tier=" + tier + ", center=" + center + ", owner=" + ownerName + "}";
    }
}
