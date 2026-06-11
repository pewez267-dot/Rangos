package com.gbaminecraft.minecraft.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only entry point. Kept separate so the common-side item code never
 * classloads client rendering classes on a dedicated server.
 */
public final class FantasticBoyClient {

    private FantasticBoyClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new FantasticBoyScreen());
    }
}
