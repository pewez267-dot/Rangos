package com.fspawner.client;

import net.minecraftforge.common.MinecraftForge;

/** Registers client-only listeners. Called from FMLClientSetupEvent. */
public final class ClientSetup {

    private ClientSetup() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }
}
