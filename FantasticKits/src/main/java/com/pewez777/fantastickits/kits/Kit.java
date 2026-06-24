/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The complete, serializable data model of a single kit.
 *
 * <p>Every field required by the specification is persisted: name, description,
 * owner group, icon, contents (items, including any custom NBT they carry),
 * associated commands, creation date, internal UUID and per-kit security
 * configuration. Serialization is fully NBT-based so kits can be both saved to
 * disk and transported over the network without any lossy conversion.</p>
 */
public final class Kit {

    // ---- NBT keys ----------------------------------------------------------
    private static final String KEY_ID = "Id";
    private static final String KEY_NAME = "Name";
    private static final String KEY_DESCRIPTION = "Description";
    private static final String KEY_OWNER_GROUP = "OwnerGroup";
    private static final String KEY_ICON = "Icon";
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_COMMANDS = "Commands";
    private static final String KEY_CREATED_AT = "CreatedAt";
    private static final String KEY_STRICT = "StrictGroupMatching";
    private static final String KEY_SINGLE_CLAIM = "SingleClaim";

    private String id;
    private String name;
    private String description;
    private String ownerGroup;
    private ItemStack icon;
    private final List<ItemStack> items = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();
    private long createdAt;
    private boolean strictGroupMatching;
    private boolean singleClaim;

    public Kit() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.description = "";
        this.ownerGroup = "";
        this.icon = new ItemStack(Items.CHEST);
        this.createdAt = System.currentTimeMillis();
        this.strictGroupMatching = true;
        this.singleClaim = true;
    }

    // ---- Accessors ---------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isEmpty()) ? UUID.randomUUID().toString() : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public void setOwnerGroup(String ownerGroup) {
        this.ownerGroup = ownerGroup == null ? "" : ownerGroup;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = (icon == null || icon.isEmpty()) ? new ItemStack(Items.CHEST) : icon;
    }

    /** Live, mutable list of kit contents. */
    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> newItems) {
        items.clear();
        if (newItems != null) {
            for (ItemStack stack : newItems) {
                if (stack != null && !stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }
    }

    /** Live, mutable list of associated commands (stored without leading slash). */
    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> newCommands) {
        commands.clear();
        if (newCommands != null) {
            for (String command : newCommands) {
                if (command != null) {
                    String trimmed = command.trim();
                    if (trimmed.startsWith("/")) {
                        trimmed = trimmed.substring(1);
                    }
                    if (!trimmed.isEmpty()) {
                        commands.add(trimmed);
                    }
                }
            }
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isStrictGroupMatching() {
        return strictGroupMatching;
    }

    public void setStrictGroupMatching(boolean strictGroupMatching) {
        this.strictGroupMatching = strictGroupMatching;
    }

    public boolean isSingleClaim() {
        return singleClaim;
    }

    public void setSingleClaim(boolean singleClaim) {
        this.singleClaim = singleClaim;
    }

    // ---- Derived helpers ---------------------------------------------------

    /** Stable, lower-cased storage key derived from the kit name. */
    public String storageKey() {
        return KitManager.normalizeName(name);
    }

    /** Deep copy used for client-side editing of a local copy. */
    public Kit copy() {
        return fromNbt(toNbt());
    }

    // ---- Serialization -----------------------------------------------------

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_ID, id);
        tag.putString(KEY_NAME, name);
        tag.putString(KEY_DESCRIPTION, description);
        tag.putString(KEY_OWNER_GROUP, ownerGroup);
        tag.putLong(KEY_CREATED_AT, createdAt);
        tag.putBoolean(KEY_STRICT, strictGroupMatching);
        tag.putBoolean(KEY_SINGLE_CLAIM, singleClaim);

        CompoundTag iconTag = new CompoundTag();
        icon.save(iconTag);
        tag.put(KEY_ICON, iconTag);

        ListTag itemList = new ListTag();
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stack.save(stackTag);
                itemList.add(stackTag);
            }
        }
        tag.put(KEY_ITEMS, itemList);

        ListTag commandList = new ListTag();
        for (String command : commands) {
            commandList.add(net.minecraft.nbt.StringTag.valueOf(command));
        }
        tag.put(KEY_COMMANDS, commandList);

        return tag;
    }

    public static Kit fromNbt(CompoundTag tag) {
        Kit kit = new Kit();
        if (tag == null) {
            return kit;
        }
        kit.setId(tag.getString(KEY_ID));
        kit.setName(tag.getString(KEY_NAME));
        kit.setDescription(tag.getString(KEY_DESCRIPTION));
        kit.setOwnerGroup(tag.getString(KEY_OWNER_GROUP));
        if (tag.contains(KEY_CREATED_AT)) {
            kit.setCreatedAt(tag.getLong(KEY_CREATED_AT));
        }
        kit.setStrictGroupMatching(!tag.contains(KEY_STRICT) || tag.getBoolean(KEY_STRICT));
        kit.setSingleClaim(!tag.contains(KEY_SINGLE_CLAIM) || tag.getBoolean(KEY_SINGLE_CLAIM));

        if (tag.contains(KEY_ICON, Tag.TAG_COMPOUND)) {
            kit.setIcon(ItemStack.of(tag.getCompound(KEY_ICON)));
        }

        List<ItemStack> loadedItems = new ArrayList<>();
        ListTag itemList = tag.getList(KEY_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            ItemStack stack = ItemStack.of(itemList.getCompound(i));
            if (!stack.isEmpty()) {
                loadedItems.add(stack);
            }
        }
        kit.setItems(loadedItems);

        List<String> loadedCommands = new ArrayList<>();
        ListTag commandList = tag.getList(KEY_COMMANDS, Tag.TAG_STRING);
        for (int i = 0; i < commandList.size(); i++) {
            loadedCommands.add(commandList.getString(i));
        }
        kit.setCommands(loadedCommands);

        return kit;
    }
}
