package com.fscrates.client;

/** Client-only setup hook. Reserved for future client registrations. */
public final class ClientSetup {

    private ClientSetup() {}

    public static void init() {
        // Currently nothing needs registering on the client beyond packet
        // handlers (which run via the network channel). Kept for symmetry and
        // future client-side keybinds / particle providers.
    }
}
