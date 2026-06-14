package com.pewez.fantasticessentials.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the last person each player exchanged private messages with, for /reply.
 */
public final class MessageState {

    private static final Map<UUID, UUID> LAST_CONTACT = new HashMap<>();

    private MessageState() {
    }

    public static void setLastContact(UUID player, UUID contact) {
        LAST_CONTACT.put(player, contact);
    }

    public static UUID getLastContact(UUID player) {
        return LAST_CONTACT.get(player);
    }

    public static void clear(UUID uuid) {
        LAST_CONTACT.remove(uuid);
        LAST_CONTACT.values().removeIf(uuid::equals);
    }
}
