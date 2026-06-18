package com.fantasticchest.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe compressed inventory: {@code ConcurrentHashMap<Item, Long>} with atomic
 * add/extract, hard {@link Long#MAX_VALUE} overflow protection and lossless NBT/JSON
 * serialisation keyed by {@link ResourceLocation}. Quantities are {@code long} everywhere
 * — never {@code int} — supporting up to 9,223,372,036,854,775,807 per item type.
 */
public final class CompressedInventory {

    private final ConcurrentHashMap<Item, Long> map = new ConcurrentHashMap<>();

    /** Adds {@code amount}, saturating at {@link Long#MAX_VALUE} (never overflows). */
    public void add(final Item item, final long amount) {
        if (item == null || item == Items.AIR || amount <= 0L) {
            return;
        }
        map.merge(item, amount, (a, b) -> (a > Long.MAX_VALUE - b) ? Long.MAX_VALUE : a + b);
    }

    /** Sets an exact quantity (0 or less removes the entry). */
    public void set(final Item item, final long amount) {
        if (item == null || item == Items.AIR) {
            return;
        }
        if (amount <= 0L) {
            map.remove(item);
        } else {
            map.put(item, amount);
        }
    }

    /**
     * Atomically extracts up to {@code requested} units of {@code item}.
     *
     * @return the amount actually removed (0 if none available). The map mutation is a
     *         single atomic {@code compute}, so concurrent extractions can never
     *         over-extract or corrupt the count.
     */
    public long extract(final Item item, final long requested) {
        if (item == null || requested <= 0L) {
            return 0L;
        }
        final long[] taken = {0L};
        map.compute(item, (key, current) -> {
            if (current == null || current <= 0L) {
                return current;
            }
            final long t = Math.min(current, requested);
            taken[0] = t;
            final long left = current - t;
            return left <= 0L ? null : left;
        });
        return taken[0];
    }

    public long get(final Item item) {
        final Long v = map.get(item);
        return v == null ? 0L : v;
    }

    public int distinctCount() {
        return map.size();
    }

    public void clear() {
        map.clear();
    }

    /** Replaces all contents with a snapshot of {@code other} (used by Refresh Stock). */
    public void replaceWith(final CompressedInventory other) {
        map.clear();
        if (other != null) {
            map.putAll(other.map);
        }
    }

    /** An ordered, detached snapshot for read-only iteration (rendering, paging, saving). */
    public Map<Item, Long> snapshot() {
        return new LinkedHashMap<>(map);
    }

    public CompressedInventory copy() {
        final CompressedInventory c = new CompressedInventory();
        c.map.putAll(this.map);
        return c;
    }

    // ---- NBT (LongTag, ResourceLocation keys) ----

    public CompoundTag toNbt() {
        final CompoundTag tag = new CompoundTag();
        for (final Map.Entry<Item, Long> entry : map.entrySet()) {
            final ResourceLocation key = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (key != null && entry.getValue() != null && entry.getValue() > 0L) {
                tag.putLong(key.toString(), entry.getValue());
            }
        }
        return tag;
    }

    public void loadNbt(final CompoundTag tag) {
        map.clear();
        if (tag == null) {
            return;
        }
        for (final String key : tag.getAllKeys()) {
            final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(key));
            if (item != null && item != Items.AIR) {
                final long amount = tag.getLong(key);
                if (amount > 0L) {
                    map.put(item, amount);
                }
            }
        }
    }

    // ---- JSON map (id string -> long) for Gson ----

    public Map<String, Long> toIdMap() {
        final Map<String, Long> out = new LinkedHashMap<>();
        for (final Map.Entry<Item, Long> entry : map.entrySet()) {
            final ResourceLocation key = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (key != null && entry.getValue() != null && entry.getValue() > 0L) {
                out.put(key.toString(), entry.getValue());
            }
        }
        return out;
    }

    public void fromIdMap(final Map<String, Long> idMap) {
        map.clear();
        if (idMap == null) {
            return;
        }
        for (final Map.Entry<String, Long> entry : idMap.entrySet()) {
            final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(entry.getKey()));
            if (item != null && item != Items.AIR && entry.getValue() != null && entry.getValue() > 0L) {
                map.put(item, entry.getValue());
            }
        }
    }
}
