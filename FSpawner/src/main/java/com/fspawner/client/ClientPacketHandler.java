package com.fspawner.client;

import com.fspawner.client.screen.FSpawnerScreen;
import com.fspawner.config.SpawnerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * Client-only entry points invoked from network packet handlers. Isolated so
 * the dedicated server never class-loads client screens.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void openScreen(CompoundTag configNbt) {
        SpawnerConfig cfg = configNbt == null ? new SpawnerConfig() : SpawnerConfig.load(configNbt);
        Minecraft.getInstance().setScreen(new FSpawnerScreen(cfg));
    }
}
