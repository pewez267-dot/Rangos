package com.fsmobs;

import com.fsmobs.command.FSMobsCommand;
import com.fsmobs.network.Net;
import com.fsmobs.stats.StatsManager;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod(FSMobs.MODID)
public final class FSMobs {

    public static final String MODID = "fsmobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FSMobs() {
        Net.register();
    }

    /** Handlers del bus de FORGE (servidor). */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {

        private ForgeEvents() {}

        @SubscribeEvent
        public static void onServerAboutToStart(ServerAboutToStartEvent event) {
            MobControl.load();
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            FSMobsCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                StatsManager.serverTick(server);
            }
        }

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            StatsManager.clear(event.getEntity().getUUID());
        }
    }
}
