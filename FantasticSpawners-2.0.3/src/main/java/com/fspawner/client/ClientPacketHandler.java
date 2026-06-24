// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.client;

import net.minecraft.client.gui.screens.Screen;
import com.fspawner.client.screen.FSpawnerScreen;
import net.minecraft.client.Minecraft;
import com.fspawner.config.SpawnerConfig;
import com.fspawner.network.EditContext;
import net.minecraft.nbt.CompoundTag;

public final class ClientPacketHandler
{
    private ClientPacketHandler() {
    }
    
    public static void openScreen(final CompoundTag configNbt, final EditContext context) {
        final SpawnerConfig cfg = (configNbt == null) ? new SpawnerConfig() : SpawnerConfig.load(configNbt);
        Minecraft.getInstance().setScreen((Screen)new FSpawnerScreen(cfg, (context == null) ? EditContext.newSession() : context));
    }
}
