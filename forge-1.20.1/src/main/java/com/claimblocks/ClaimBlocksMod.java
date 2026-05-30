package com.claimblocks;

import com.claimblocks.command.ClaimAdminCommands;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.EntityProtectionEvents;
import com.claimblocks.event.PassiveEffectsManager;
import com.claimblocks.event.PlayerTracker;
import com.claimblocks.gui.ClaimMenuHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ClaimBlocksMod.MOD_ID)
public class ClaimBlocksMod {
    public static final String MOD_ID = "claimblocks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ClaimBlocksMod() {
        LOGGER.info("[ClaimBlocks] Inicializando v6.0.0 (Forge 1.20.1)...");
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new BlockProtectionEvents());
        MinecraftForge.EVENT_BUS.register(new EntityProtectionEvents());
        MinecraftForge.EVENT_BUS.register(new PlayerTracker());
        LOGGER.info("[ClaimBlocks] Eventos registrados.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClaimCommands.register(event.getDispatcher());
        ClaimAdminCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ClaimManager.getInstance().load(event.getServer());
        GlobalFlags.getInstance().load(event.getServer());
        LOGGER.info("[ClaimBlocks] Datos cargados.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ClaimManager.getInstance().save();
        GlobalFlags.getInstance().save(event.getServer());
        LOGGER.info("[ClaimBlocks] Datos guardados al apagar.");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ClaimManager.getInstance().flushPendingTo(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerTracker.onDisconnect(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        PlayerTracker.tick(server);
        BlockProtectionEvents.tickFireSweep(server);
        PassiveEffectsManager.tick(server);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ClaimMenuHandler.handleChat(event);
    }
}
