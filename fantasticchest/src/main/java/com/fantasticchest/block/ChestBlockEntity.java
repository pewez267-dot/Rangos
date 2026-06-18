package com.fantasticchest.block;

import com.fantasticchest.data.ChestDefinition;
import com.fantasticchest.data.ChestRegistry;
import com.fantasticchest.inventory.CompressedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure-state BlockEntity for a Fantastic Chest. <strong>It has no {@code tick()} and runs
 * no logic on any tick</strong> — it only stores data and reacts to explicit operations
 * (extract, edit, refresh) triggered by packets. Quantities are {@code Long} via
 * {@link CompressedInventory} (thread-safe). State changes mark the chunk dirty and push
 * an updated {@link ChestDefinition} to the in-memory {@link ChestRegistry} (async disk).
 */
public final class ChestBlockEntity extends BlockEntity {

    private final CompressedInventory inventory = new CompressedInventory();
    private final CompressedInventory originalStock = new CompressedInventory();
    private final Set<UUID> permitted = ConcurrentHashMap.newKeySet();
    private String chestId = "";
    private String chestName = "";
    private UUID ownerUuid = null;

    public ChestBlockEntity(final BlockPos pos, final BlockState state) {
        super(ModBlocks.CHEST_BLOCK_ENTITY.get(), pos, state);
    }

    // ---- accessors ----

    public CompressedInventory inventory() {
        return this.inventory;
    }

    public CompressedInventory originalStock() {
        return this.originalStock;
    }

    public String getChestId() {
        return this.chestId;
    }

    public String getChestName() {
        return this.chestName;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public Set<UUID> getPermitted() {
        return this.permitted;
    }

    /** Owner or an explicitly permitted player may open the terminal. */
    public boolean canAccess(final UUID uuid) {
        return uuid != null && (uuid.equals(this.ownerUuid) || this.permitted.contains(uuid));
    }

    // ---- mutations (each syncs to registry + marks chunk dirty) ----

    /** Replaces the full configuration (used on placement, creation link and edit). */
    public void applyConfig(final String id, final String name, final UUID owner,
                            final Set<UUID> permittedPlayers,
                            final CompressedInventory newInventory,
                            final CompressedInventory newOriginal) {
        this.chestId = id == null ? "" : id;
        this.chestName = name == null ? "" : name;
        this.ownerUuid = owner;
        this.permitted.clear();
        if (permittedPlayers != null) {
            this.permitted.addAll(permittedPlayers);
        }
        this.inventory.replaceWith(newInventory);
        this.originalStock.replaceWith(newOriginal);
        syncAndSave();
    }

    public void setName(final String name) {
        this.chestName = name == null ? "" : name;
        syncAndSave();
    }

    public void setPermitted(final Set<UUID> permittedPlayers) {
        this.permitted.clear();
        if (permittedPlayers != null) {
            this.permitted.addAll(permittedPlayers);
        }
        syncAndSave();
    }

    /** Atomically extracts up to {@code requested}; returns the amount actually taken. */
    public long extract(final Item item, final long requested) {
        final long taken = this.inventory.extract(item, requested);
        if (taken > 0L) {
            syncAndSave();
        }
        return taken;
    }

    /** Returns overflow back to the chest (player inventory was full). */
    public void returnToStock(final Item item, final long amount) {
        if (item != null && amount > 0L) {
            this.inventory.add(item, amount);
            syncAndSave();
        }
    }

    /** Atomically restores the inventory to the originally configured stock. */
    public void refreshStock() {
        this.inventory.replaceWith(this.originalStock);
        syncAndSave();
    }

    /** Replaces both the live inventory and the configured original stock (edit mode). */
    public void applyStock(final CompressedInventory newInventory, final CompressedInventory newOriginal) {
        this.inventory.replaceWith(newInventory);
        this.originalStock.replaceWith(newOriginal);
        syncAndSave();
    }

    private void syncAndSave() {
        setChanged();
        if (this.level instanceof ServerLevel serverLevel && this.chestId != null && !this.chestId.isBlank()) {
            ChestRegistry.get().put(toDefinition(serverLevel));
        }
    }

    /** Called from the block on placement to register the chest as placed in the registry. */
    public void onPlaced() {
        syncAndSave();
    }

    /** Sets the owner only if none is present yet (e.g. legacy item without fc_owner). */
    public void ensureOwner(final UUID uuid) {
        if (this.ownerUuid == null) {
            this.ownerUuid = uuid;
        }
    }

    public ChestDefinition toDefinition(final ServerLevel serverLevel) {
        final ChestDefinition existing = ChestRegistry.get().get(this.chestId);
        final ChestDefinition def = new ChestDefinition();
        def.id = this.chestId;
        def.name = this.chestName;
        def.ownerUuid = this.ownerUuid == null ? "" : this.ownerUuid.toString();
        def.permitted = new ArrayList<>();
        for (final UUID uuid : this.permitted) {
            def.permitted.add(uuid.toString());
        }
        def.world = serverLevel.dimension().location().toString();
        def.pos = new ChestDefinition.Pos(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
        def.placed = true;
        def.inventory = this.inventory.toIdMap();
        def.originalStock = this.originalStock.toIdMap();
        def.createdAt = (existing != null && !existing.createdAt.isBlank()) ? existing.createdAt : java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
        def.createdBy = (existing != null && !existing.createdBy.isBlank()) ? existing.createdBy : "";
        return def;
    }

    // ---- NBT (LongTag preserves Long) ----

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("fc_id", this.chestId);
        tag.putString("fc_name", this.chestName);
        tag.putString("fc_owner", this.ownerUuid == null ? "" : this.ownerUuid.toString());
        final ListTag permittedTag = new ListTag();
        for (final UUID uuid : this.permitted) {
            permittedTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("fc_permitted", permittedTag);
        tag.put("fc_inventory", this.inventory.toNbt());
        tag.put("fc_original", this.originalStock.toNbt());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        this.chestId = tag.getString("fc_id");
        this.chestName = tag.getString("fc_name");
        final String owner = tag.getString("fc_owner");
        this.ownerUuid = owner.isBlank() ? null : safeUuid(owner);
        this.permitted.clear();
        final ListTag permittedTag = tag.getList("fc_permitted", Tag.TAG_STRING);
        for (int i = 0; i < permittedTag.size(); i++) {
            final UUID uuid = safeUuid(permittedTag.getString(i));
            if (uuid != null) {
                this.permitted.add(uuid);
            }
        }
        this.inventory.loadNbt(tag.getCompound("fc_inventory"));
        this.originalStock.loadNbt(tag.getCompound("fc_original"));
    }

    private static UUID safeUuid(final String s) {
        try {
            return UUID.fromString(s);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /** Registry rows whose permitted set/list is needed as a {@link List} of strings. */
    public List<String> permittedAsStrings() {
        final List<String> out = new ArrayList<>();
        for (final UUID uuid : this.permitted) {
            out.add(uuid.toString());
        }
        return out;
    }
}
