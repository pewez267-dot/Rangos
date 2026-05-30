package com.arthurleray.invhistory.forge;

import com.arthurleray.invhistory.InvHistory;
import com.arthurleray.invhistory.InvHistoryPlatform;
import com.arthurleray.invhistory.command.InvHistoryCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Forge 1.20.1 entry point. Replaces Fabric's DedicatedServerModInitializer and wires every
 * Fabric event to its Forge equivalent.
 */
@Mod(InvHistory.MOD_ID)
public class InvHistoryForge {

    public InvHistoryForge() {
        InvHistoryPlatform.INSTANCE[0] = new ForgePlatform();
        InvHistory.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    // CommandRegistrationCallback -> RegisterCommandsEvent
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        InvHistoryCommand.register(event.getDispatcher());
    }

    // ServerLifecycleEvents.SERVER_STARTED -> ServerStartedEvent
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        InvHistory.storage().init(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    // ServerTickEvents.END_SERVER_TICK -> TickEvent.ServerTickEvent (END)
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            InvHistory.tracker().onServerTick(event.getServer());
        }
    }

    // ServerPlayConnectionEvents.JOIN -> PlayerLoggedInEvent
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InvHistory.tracker().onPlayerJoin(player);
        }
    }

    // ServerPlayConnectionEvents.DISCONNECT -> PlayerLoggedOutEvent
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InvHistory.tracker().onPlayerLeave(player);
        }
    }

    // ServerLivingEntityEvents.AFTER_DEATH -> LivingDeathEvent (fires before inventory is dropped)
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InvHistory.tracker().onPlayerDeath(player);
        }
    }

    private static final class ForgePlatform implements InvHistoryPlatform {
        @Override
        public Path getConfigDir() {
            return FMLPaths.CONFIGDIR.get();
        }
    }
}
