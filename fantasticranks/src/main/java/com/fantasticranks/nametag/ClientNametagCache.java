package com.fantasticranks.nametag;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of per-player rank nametag state, populated by the nametag update
 * packet and read by the renderer Mixin. Concurrent because it is written on the client
 * network thread and read on the client render thread.
 */
public final class ClientNametagCache {

    private static final Map<UUID, NametagData> CACHE = new ConcurrentHashMap<>();

    private ClientNametagCache() {
    }

    public static void put(UUID id, NametagData data) {
        if (id != null && data != null) {
            CACHE.put(id, data);
        }
    }

    @Nullable
    public static NametagData get(UUID id) {
        return id == null ? null : CACHE.get(id);
    }

    public static void remove(UUID id) {
        if (id != null) {
            CACHE.remove(id);
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}
