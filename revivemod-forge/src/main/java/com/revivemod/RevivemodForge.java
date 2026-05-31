package com.revivemod;

import com.mojang.logging.LogUtils;
import com.revivemod.command.ReviveCommands;
import com.revivemod.config.ReviveConfig;
import com.revivemod.event.CombatEvents;
import com.revivemod.event.ConnectionEvents;
import com.revivemod.event.DownTicker;
import com.revivemod.event.InteractionEvents;
import com.revivemod.event.RestrictionEvents;
import com.revivemod.network.ReviveNetwork;
import com.revivemod.state.DownManager;
import java.nio.file.Path;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod(value="revivemod")
public final class RevivemodForge {
    public static final String MOD_ID = "revivemod";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static ReviveConfig CONFIG = new ReviveConfig();
    private static Path CONFIG_PATH;

    public RevivemodForge() {
        CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("revivemod.json");
        CONFIG = ReviveConfig.load(CONFIG_PATH);
        ReviveNetwork.register();
        MinecraftForge.EVENT_BUS.register((Object)new CombatEvents());
        MinecraftForge.EVENT_BUS.register((Object)new DownTicker());
        MinecraftForge.EVENT_BUS.register((Object)new ConnectionEvents());
        MinecraftForge.EVENT_BUS.register((Object)new InteractionEvents());
        MinecraftForge.EVENT_BUS.register((Object)new RestrictionEvents());
        MinecraftForge.EVENT_BUS.register((Object)new ReviveCommands());
        MinecraftForge.EVENT_BUS.register((Object)this);
        LOGGER.info("[{}] ReviveMod (Forge) initialised. downTime={}s, reviveDistance={} blocks, reviveTime={} ticks", new Object[]{MOD_ID, RevivemodForge.CONFIG.downTimeSeconds, RevivemodForge.CONFIG.reviveDistance, RevivemodForge.CONFIG.reviveTimeTicks});
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[{}] Saving config and clearing down state...", (Object)MOD_ID);
        RevivemodForge.saveConfig();
        DownManager.clearAll(event.getServer());
    }

    public static ReviveConfig getConfig() {
        return CONFIG;
    }

    public static void saveConfig() {
        if (CONFIG_PATH != null) {
            CONFIG.save(CONFIG_PATH);
        }
    }

    public static void reloadConfig() {
        if (CONFIG_PATH != null) {
            CONFIG = ReviveConfig.load(CONFIG_PATH);
        }
    }
}

