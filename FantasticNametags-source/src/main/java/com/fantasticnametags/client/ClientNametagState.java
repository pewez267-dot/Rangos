package com.fantasticnametags.client;

/** Valores vigentes en el cliente (recibidos del server). El render los lee de aqui. */
public final class ClientNametagState {
    private static volatile double heightOffset = 0.35;
    private static volatile boolean playersOnly = true;

    public static void apply(double height, boolean onlyPlayers) {
        heightOffset = height;
        playersOnly = onlyPlayers;
    }

    public static double heightOffset() {
        return heightOffset;
    }

    public static boolean playersOnly() {
        return playersOnly;
    }

    private ClientNametagState() {
    }
}
