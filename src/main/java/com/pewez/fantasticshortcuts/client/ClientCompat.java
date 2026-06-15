package com.pewez.fantasticshortcuts.client;

/**
 * Client compatibility notes.
 *
 * Fantastic Shortcuts is server-driven: the management GUI uses a vanilla 9x6 container, so it
 * renders on any client with no client-side code required. Command suggestions/tab completion are
 * synchronised through the standard server command tree. This class is intentionally a no-op marker
 * documenting that the mod requires nothing special on the client.
 */
public final class ClientCompat {

    public static final boolean REQUIRES_CLIENT_MOD = false;

    private ClientCompat() {
    }
}
