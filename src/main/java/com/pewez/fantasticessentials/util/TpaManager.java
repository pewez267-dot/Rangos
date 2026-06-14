package com.pewez.fantasticessentials.util;

import com.pewez.fantasticessentials.config.Config;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending teleport requests (/tpa and /tpahere).
 */
public final class TpaManager {

    public enum Direction {
        /** Sender teleports to the target (/tpa). */
        TO_TARGET,
        /** Target teleports to the sender (/tpahere). */
        FROM_TARGET
    }

    public static final class Request {
        public final UUID sender;
        public final UUID target;
        public final Direction direction;
        public final long expiry;

        public Request(UUID sender, UUID target, Direction direction, long expiry) {
            this.sender = sender;
            this.target = target;
            this.direction = direction;
            this.expiry = expiry;
        }
    }

    // key: target uuid -> (sender uuid -> request)
    private static final Map<UUID, Map<UUID, Request>> REQUESTS = new HashMap<>();

    private TpaManager() {
    }

    public static void add(ServerPlayer sender, ServerPlayer target, Direction direction) {
        long expiry = System.currentTimeMillis() + Config.get().tpaTimeoutSeconds * 1000L;
        REQUESTS.computeIfAbsent(target.getUUID(), u -> new HashMap<>())
                .put(sender.getUUID(), new Request(sender.getUUID(), target.getUUID(), direction, expiry));
    }

    public static boolean has(UUID target, UUID sender) {
        Map<UUID, Request> map = REQUESTS.get(target);
        return map != null && map.containsKey(sender);
    }

    /** Returns and removes a request. If sender is null, returns the most recent request. */
    public static Request take(UUID target, UUID sender) {
        Map<UUID, Request> map = REQUESTS.get(target);
        if (map == null || map.isEmpty()) {
            return null;
        }
        if (sender != null) {
            return map.remove(sender);
        }
        // most recent (highest expiry)
        UUID best = null;
        long bestExpiry = Long.MIN_VALUE;
        for (Map.Entry<UUID, Request> entry : map.entrySet()) {
            if (entry.getValue().expiry > bestExpiry) {
                bestExpiry = entry.getValue().expiry;
                best = entry.getKey();
            }
        }
        return best == null ? null : map.remove(best);
    }

    public static boolean hasAny(UUID target) {
        Map<UUID, Request> map = REQUESTS.get(target);
        return map != null && !map.isEmpty();
    }

    public static void tickExpire() {
        long now = System.currentTimeMillis();
        Iterator<Map<UUID, Request>> outer = REQUESTS.values().iterator();
        while (outer.hasNext()) {
            Map<UUID, Request> map = outer.next();
            map.values().removeIf(request -> request.expiry < now);
            if (map.isEmpty()) {
                outer.remove();
            }
        }
    }

    public static void clear(UUID uuid) {
        REQUESTS.remove(uuid);
        for (Map<UUID, Request> map : REQUESTS.values()) {
            map.remove(uuid);
        }
    }
}
