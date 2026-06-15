package com.fantastic.kits.kits;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.audit.AuditEventType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds every {@link Kit} loaded in memory and is the single API the rest of
 * the mod uses to mutate the catalogue. Every change is persisted immediately
 * (eager save) and replayed to LuckPerms through the integration hook.
 */
public final class KitManager {

    private final Path kitsDir;
    private final Map<String, Kit> byId = new ConcurrentHashMap<>();
    private final Map<UUID, Kit> byUuid = new ConcurrentHashMap<>();

    public KitManager(Path kitsDir) {
        this.kitsDir = kitsDir;
        try {
            Files.createDirectories(kitsDir);
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Cannot create kits directory {}", kitsDir, e);
        }
    }

    public int size() { return byId.size(); }
    public Path directory() { return kitsDir; }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    public Optional<Kit> byId(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<Kit> byUuid(UUID uuid) {
        return Optional.ofNullable(byUuid.get(uuid));
    }

    public List<Kit> all() {
        List<Kit> list = new ArrayList<>(byId.values());
        list.sort(Comparator.comparing(Kit::displayName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public List<Kit> byGroup(String groupName) {
        if (groupName == null) return List.of();
        return all().stream()
                .filter(k -> groupName.equalsIgnoreCase(k.ownerGroup()))
                .collect(Collectors.toList());
    }

    /**
     * The full set of commands that {@code groupName} is entitled to run via any
     * of its kits. Used by the runtime command gate.
     */
    public java.util.Set<String> commandsForGroup(String groupName) {
        return byGroup(groupName).stream()
                .flatMap(k -> k.commands().stream())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    public Kit createEmpty(String id, String displayName, String ownerGroup) {
        String safeId = Kit.sanitizeId(id);
        if (byId.containsKey(safeId)) {
            throw new IllegalStateException("A kit with id '" + safeId + "' already exists.");
        }
        Kit kit = new Kit(safeId, displayName, ownerGroup);
        byId.put(safeId, kit);
        byUuid.put(kit.uuid(), kit);
        save(kit);
        FantasticKits.luckPerms().syncKitToGroup(ownerGroup, kit.id(), kit.commands());
        FantasticKits.audit().log(AuditEventType.CREATE_KIT, null, kit, "SUCCESS",
                "Empty kit registered for group '" + ownerGroup + "'.");
        return kit;
    }

    /**
     * Insert a brand-new kit that the editor produced. Generates a unique id if
     * the incoming one collides with an existing entry, and persists immediately.
     */
    public synchronized Kit registerNew(Kit kit) {
        String desiredId = Kit.sanitizeId(kit.id());
        String finalId = desiredId;
        int suffix = 2;
        while (byId.containsKey(finalId)) {
            finalId = desiredId + "_" + suffix;
            suffix++;
        }
        if (!finalId.equals(kit.id())) {
            // Replace the kit's id by re-loading it through NBT with the new id.
            net.minecraft.nbt.CompoundTag tag = kit.save();
            tag.putString("id", finalId);
            kit = Kit.load(tag);
        }
        byId.put(kit.id(), kit);
        byUuid.put(kit.uuid(), kit);
        save(kit);
        return kit;
    }

    /**
     * Replace an existing kit's data with a freshly-deserialised instance.
     * Preserves the in-memory map slot so external references stay consistent.
     */
    public synchronized void replace(Kit existing, Kit incoming) {
        if (existing == null || incoming == null) return;
        // Move id key if it changed.
        if (!existing.id().equalsIgnoreCase(incoming.id())) {
            byId.remove(existing.id());
            try { java.nio.file.Files.deleteIfExists(fileFor(existing.id())); }
            catch (java.io.IOException ignored) {}
        }
        byId.put(incoming.id(), incoming);
        byUuid.put(incoming.uuid(), incoming);
        save(incoming);
    }

    /**
     * Replaces an existing kit's data atomically. The kit reference itself stays
     * the same so live menus see the updated values immediately.
     */
    public void update(Kit kit, Runnable mutation) {
        synchronized (kit) {
            mutation.run();
            save(kit);
            FantasticKits.luckPerms().syncKitToGroup(kit.ownerGroup(), kit.id(), kit.commands());
        }
    }

    public boolean delete(Kit kit, String reason) {
        if (kit == null) return false;
        Kit removed = byId.remove(kit.id());
        if (removed == null) return false;
        byUuid.remove(kit.uuid());
        try {
            Files.deleteIfExists(fileFor(kit.id()));
        } catch (IOException e) {
            FantasticKits.LOGGER.warn("Failed to delete kit file for '{}'", kit.id(), e);
        }
        FantasticKits.luckPerms().revokeKit(kit.id());
        FantasticKits.audit().log(AuditEventType.DELETE_KIT, null, kit, "SUCCESS",
                reason == null ? "Kit deleted." : reason);
        return true;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public void loadAll() {
        byId.clear();
        byUuid.clear();
        if (!Files.isDirectory(kitsDir)) return;
        try (Stream<Path> stream = Files.list(kitsDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".dat"))
                  .forEach(p -> {
                      try {
                          CompoundTag tag = NbtIo.readCompressed(p.toFile());
                          Kit kit = Kit.load(tag);
                          byId.put(kit.id(), kit);
                          byUuid.put(kit.uuid(), kit);
                      } catch (Exception e) {
                          FantasticKits.LOGGER.error("Failed to load kit file {}", p, e);
                      }
                  });
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Cannot list kits directory {}", kitsDir, e);
        }
        FantasticKits.LOGGER.info("Loaded {} kit(s) from disk.", byId.size());
    }

    public void save(Kit kit) {
        try {
            Files.createDirectories(kitsDir);
            NbtIo.writeCompressed(kit.save(), fileFor(kit.id()).toFile());
        } catch (IOException e) {
            FantasticKits.LOGGER.error("Failed to save kit '{}'", kit.id(), e);
        }
    }

    public void flush() {
        for (Kit k : byId.values()) save(k);
    }

    private Path fileFor(String id) {
        return kitsDir.resolve(id + ".dat");
    }
}
