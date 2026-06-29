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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class ServerEvents {
   private static final AfkTracker AFK = new AfkTracker();
   private static final TierProgressionManager PROGRESSION = new TierProgressionManager(AFK);

   @SubscribeEvent
   public void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) {
            AFK.serverTick(server);
            PROGRESSION.serverTick(server);
         }
      }
   }

   @SubscribeEvent
   public void onRegisterCommands(RegisterCommandsEvent event) {
      FsPassCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public void onRightClickBlock(RightClickBlock event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onRightClickItem(RightClickItem event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onLeftClickBlock(LeftClickBlock event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onEntityInteract(EntityInteract event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onAttackEntity(AttackEntityEvent event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onBlockBreak(BreakEvent event) {
      this.mark(event.getPlayer());
   }

   @SubscribeEvent
   public void onChat(ServerChatEvent event) {
      AFK.registerInteraction(event.getPlayer());
   }

   @SubscribeEvent
   public void onCommand(CommandEvent event) {
      CommandSourceStack source = (CommandSourceStack)event.getParseResults().getContext().getSource();
      if (source.getEntity() instanceof ServerPlayer serverPlayer) {
         AFK.registerInteraction(serverPlayer);
      }
   }

   private void mark(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         AFK.registerInteraction(serverPlayer);
      }
   }

   @SubscribeEvent
   public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer joining) {
         MinecraftServer server = joining.getServer();
         if (server != null) {
            NametagSync.syncPlayer(joining);

            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
               NametagData data = NametagSync.compute(other);
               PacketHandler.sendToPlayer(joining, new NametagUpdatePacket(other.getUUID(), data));
            }
         }
      }
   }

   @SubscribeEvent
   public void onStartTracking(StartTracking event) {
      if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
         PacketHandler.sendToPlayer(viewer, new NametagUpdatePacket(target.getUUID(), NametagSync.compute(target)));
      }
   }

   @SubscribeEvent
   public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         AFK.remove(serverPlayer.getUUID());
      }
   }

   @SubscribeEvent
   public void onServerStopping(ServerStoppingEvent event) {
      AFK.clear();
   }
}
