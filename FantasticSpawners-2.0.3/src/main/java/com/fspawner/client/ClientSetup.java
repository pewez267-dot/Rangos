// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.client;

import net.minecraftforge.common.MinecraftForge;

public final class ClientSetup
{
    private ClientSetup() {
    }
    
    public static void init() {
        MinecraftForge.EVENT_BUS.register((Object)ClientEvents.class);
    }
}
