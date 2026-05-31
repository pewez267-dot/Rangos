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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Forge 1.20.1 port of the Fabric "Revive Mod".
 *
 * Every Fabric feature is reproduced here using pure Forge events + a SimpleChannel network:
 *  - Bleed-out (downed) state with a boss-bar countdown.
 *  - Right-click revive (faster the more players revive at once); reviver is invincible while reviving.
 *  - Self-revive paying XP levels and surrender to die.
 *  - Slowness + glowing + interaction restrictions while downed.
 *  - Mobs ignore downed players (LivingChangeTargetEvent) and existing aggro is cleared on knock-down.
 *  - Action-bar revive progress, client HUD overlay, hold E (surrender) / F (self) prompts.
 *  - Inventory screen blocked while downed; forced crawl pose.
 *  - JSON config + /revive command tree.
 */
@Mod(RevivemodForge.MOD_ID)
public final class RevivemodForge {
    public static final String MOD_ID = "revivemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ReviveConfig CONFIG = new ReviveConfig();
    private static Path CONFIG_PATH;

    public RevivemodForge() {
        CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("revivemod.json");
        CONFIG = ReviveConfig.load(CONFIG_PATH);

        ReviveNetwork.register();

        MinecraftForge.EVENT_BUS.register(new CombatEvents());
        MinecraftForge.EVENT_BUS.register(new DownTicker());
        MinecraftForge.EVENT_BUS.register(new ConnectionEvents());
        MinecraftForge.EVENT_BUS.register(new InteractionEvents());
        MinecraftForge.EVENT_BUS.register(new RestrictionEvents());
        MinecraftForge.EVENT_BUS.register(new ReviveCommands());
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[{}] ReviveMod (Forge) initialised. downTime={}s, reviveDistance={} blocks, reviveTime={} ticks",
                MOD_ID, CONFIG.downTimeSeconds, CONFIG.reviveDistance, CONFIG.reviveTimeTicks);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[{}] Saving config and clearing down state...", MOD_ID);
        saveConfig();
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
