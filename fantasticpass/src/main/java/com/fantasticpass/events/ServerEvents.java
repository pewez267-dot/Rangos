package com.fantasticpass.events;

import com.fantasticpass.afk.AfkTracker;
import com.fantasticpass.commands.FsPassCommand;
import com.fantasticpass.nametag.NametagData;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.network.NametagUpdatePacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.progression.TierProgressionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Forge-bus gameplay handlers: server tick (AFK + progression), interaction events that
 * reset the AFK timer, command registration, and nametag synchronization on login and
 * when players come into view.
 */
public final class ServerEvents {

    private static final AfkTracker AFK = new AfkTracker();
    private static final TierProgressionManager PROGRESSION = new TierProgressionManager(AFK);

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        AFK.serverTick(server);
        PROGRESSION.serverTick(server);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FsPassCommand.register(event.getDispatcher());
    }

    // ---- Anti-AFK interaction hooks ----

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        mark(event.getEntity());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        mark(event.getEntity());
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        mark(event.getEntity());
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        mark(event.getEntity());
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        mark(event.getEntity());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        mark(event.getPlayer());
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        AFK.registerInteraction(event.getPlayer());
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer serverPlayer) {
            AFK.registerInteraction(serverPlayer);
        }
    }

    private void mark(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            AFK.registerInteraction(serverPlayer);
        }
    }

    // ---- Nametag synchronization ----

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) {
            return;
        }
        MinecraftServer server = joining.getServer();
        if (server == null) {
            return;
        }
        // Broadcast the joining player's line to everyone who can see them (and self).
        NametagSync.syncPlayer(joining);
        // Populate the joining client's cache with every currently-online player.
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            NametagData data = NametagSync.compute(other);
            PacketHandler.sendToPlayer(joining, new NametagUpdatePacket(other.getUUID(), data));
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target
                && event.getEntity() instanceof ServerPlayer viewer) {
            PacketHandler.sendToPlayer(viewer, new NametagUpdatePacket(target.getUUID(), NametagSync.compute(target)));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AFK.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        AFK.clear();
    }
}
