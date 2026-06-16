package com.fantastickits.data;

import com.fantastickits.FantasticKits;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Authoritative, server-side store of every kit definition, persisted to
 * {@code config/fantastickits/kits.json} via Gson.
 *
 * <p>Items are stored as SNBT strings so the JSON file stays human-readable while
 * remaining a lossless round-trip of the full item NBT (name, lore, enchantments,
 * attributes, flags, custom model data, unbreakable, arbitrary tags...).</p>
 *
 * <p>All mutating operations are {@code synchronized} on this instance and persist
 * immediately, which together with {@link JsonIO}'s atomic writes makes the store
 * safe against concurrent access and crashes.</p>
 */
public final class KitRegistry {

    private static final KitRegistry INSTANCE = new KitRegistry();

    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private boolean loaded = false;

    private KitRegistry() {
    }

    public static KitRegistry get() {
        synchronized (INSTANCE) {
            if (!INSTANCE.loaded) {
                INSTANCE.load();
                INSTANCE.loaded = true;
            }
        }
        return INSTANCE;
    }

    public synchronized void load() {
        this.kits.clear();
        final JsonObject root = JsonIO.read(DataPaths.kits());
        if (!root.has("kits") || !root.get("kits").isJsonObject()) {
            return;
        }
        final JsonObject stored = root.getAsJsonObject("kits");
        for (final Map.Entry<String, JsonElement> entry : stored.entrySet()) {
            try {
                final Kit kit = deserialize(entry.getValue().getAsJsonObject());
                this.kits.put(kit.id.toLowerCase(), kit);
            } catch (final Exception e) {
                FantasticKits.LOGGER.error("[FantasticKits] Kit '{}' corrupto en kits.json, se omite: {}", entry.getKey(), e.toString());
            }
        }
        FantasticKits.LOGGER.info("[FantasticKits] {} kit(s) cargados.", this.kits.size());
    }

    public synchronized void save() {
        final JsonObject root = new JsonObject();
        final JsonObject stored = new JsonObject();
        for (final Kit kit : this.kits.values()) {
            stored.add(kit.id.toLowerCase(), serialize(kit));
        }
        root.add("kits", stored);
        JsonIO.write(DataPaths.kits(), root);
    }

    /** Stores (or replaces) a kit and persists immediately. The stored copy is detached. */
    public synchronized void put(final Kit kit) {
        kit.id = Kit.normalizeId(kit.id);
        this.kits.put(kit.id.toLowerCase(), kit.copy());
        save();
    }

    /** Returns a detached copy of the kit, or {@code null} if it does not exist. */
    public synchronized Kit get(final String id) {
        if (id == null) {
            return null;
        }
        final Kit kit = this.kits.get(id.toLowerCase());
        return kit == null ? null : kit.copy();
    }

    public synchronized boolean exists(final String id) {
        return id != null && this.kits.containsKey(id.toLowerCase());
    }

    public synchronized boolean remove(final String id) {
        if (id == null) {
            return false;
        }
        final boolean removed = this.kits.remove(id.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized Set<String> ids() {
        return new TreeSet<>(this.kits.keySet());
    }

    public synchronized List<Kit> all() {
        final List<Kit> out = new ArrayList<>();
        for (final Kit kit : this.kits.values()) {
            out.add(kit.copy());
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // JSON <-> Kit
    // ---------------------------------------------------------------------

    private static JsonObject serialize(final Kit kit) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("id", kit.id);
        obj.addProperty("displayName", kit.displayName == null ? "" : kit.displayName);
        obj.addProperty("group", kit.group == null ? "" : kit.group);
        final JsonArray items = new JsonArray();
        for (final ItemStack stack : kit.items) {
            if (stack != null && !stack.isEmpty()) {
                items.add(stack.save(new CompoundTag()).toString());
            }
        }
        obj.add("items", items);
        return obj;
    }

    private static Kit deserialize(final JsonObject obj) throws Exception {
        final Kit kit = new Kit(obj.get("id").getAsString());
        kit.displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : kit.id;
        kit.group = obj.has("group") ? obj.get("group").getAsString() : "";
        kit.items.clear();
        if (obj.has("items") && obj.get("items").isJsonArray()) {
            for (final JsonElement element : obj.getAsJsonArray("items")) {
                final CompoundTag tag = TagParser.parseTag(element.getAsString());
                final ItemStack stack = ItemStack.of(tag);
                if (!stack.isEmpty()) {
                    kit.items.add(stack);
                }
            }
        }
        return kit;
    }
}
