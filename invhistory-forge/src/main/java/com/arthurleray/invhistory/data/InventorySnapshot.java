package com.arthurleray.invhistory.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A point-in-time copy of a player's inventory.
 * 1.20.1 uses the legacy {@code ItemStack.save}/{@code ItemStack.of} NBT API (no registries needed).
 */
public class InventorySnapshot {
    private final long timestamp;
    private final String reason;
    private final List<SlotData> slots;

    public InventorySnapshot(long timestamp, String reason, List<SlotData> slots) {
        this.timestamp = timestamp;
        this.reason = reason;
        this.slots = slots;
    }

    public static InventorySnapshot capture(Inventory inventory, String reason) {
        ArrayList<SlotData> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag ct = new CompoundTag();
            stack.save(ct);
            slots.add(new SlotData(i, ct));
        }
        return new InventorySnapshot(System.currentTimeMillis(), reason, slots);
    }

    public void restore(Inventory inventory) {
        inventory.clearContent();
        for (SlotData slot : this.slots) {
            if (slot.slot() >= inventory.getContainerSize()) {
                continue;
            }
            inventory.setItem(slot.slot(), ItemStack.of(slot.tag()));
        }
    }


    public long getTimestamp() {
        return this.timestamp;
    }

    public String getReason() {
        return this.reason;
    }

    public List<SlotData> getSlots() {
        return this.slots;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("timestamp", this.timestamp);
        tag.putString("reason", this.reason);
        ListTag itemList = new ListTag();
        for (SlotData slot : this.slots) {
            CompoundTag entry = slot.tag().copy();
            entry.putInt("InvSlot", slot.slot());
            itemList.add(entry);
        }
        tag.put("items", itemList);
        return tag;
    }

    public static InventorySnapshot fromNbt(CompoundTag tag) {
        long timestamp = tag.getLong("timestamp");
        String reason = tag.getString("reason");
        if (reason.isEmpty()) {
            reason = "unknown";
        }
        ListTag itemList = tag.getList("items", 10);
        ArrayList<SlotData> slots = new ArrayList<>();
        for (int i = 0; i < itemList.size(); ++i) {
            CompoundTag entry = itemList.getCompound(i);
            int slot = entry.getInt("InvSlot");
            CompoundTag itemTag = entry.copy();
            itemTag.remove("InvSlot");
            slots.add(new SlotData(slot, itemTag));
        }
        return new InventorySnapshot(timestamp, reason, slots);
    }

    public record SlotData(int slot, CompoundTag tag) {
    }
}
