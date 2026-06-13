package com.theplumteam.server;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.network.SyncServerConfigPacket;
import com.theplumteam.network.SyncTokenDataPacket;
import com.theplumteam.server.config.ServerConfig;
import dev.architectury.event.events.common.TickEvent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerTickHandler {
   private static long lastCheckTick = 0L;
   private static final int CHECK_INTERVAL = 20;

   public static void init() {
      TickEvent.SERVER_POST.register(ServerTickHandler::onServerTick);
      BlockPopsMod.logDebug("Server tick handler initialized");
   }

   private static void onServerTick(MinecraftServer server) {
      if ((long)server.getTickCount() - lastCheckTick >= 20L) {
         lastCheckTick = server.getTickCount();

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SyncServerConfigPacket.sendToPlayer(player);
            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
            boolean needsSync = false;
            needsSync |= processRegularTokens(player, discovery);
            needsSync |= processSpecialTokenReset(discovery);
            if (needsSync) {
               PlayerDataManager.markDirty(player, discovery);
               sendSyncPacket(player, discovery);
            }
         }
      }
   }

   private static boolean processRegularTokens(ServerPlayer player, IPlayerDiscovery discovery) {
      ServerLevel world = player.serverLevel();
      long gameTime = world.getGameTime();
      ServerConfig config = ServerConfig.getInstance();
      int maxTokens = config.getMaxRegularTokens();
      long cooldownTicks = (long)config.getRegularTokenCooldownHours() * 60L * 60L * 20L;
      if (discovery.getRegularTokens() < maxTokens && gameTime >= discovery.getNextRegularTokenTime()) {
         discovery.setRegularTokens(discovery.getRegularTokens() + 1);
         discovery.setNextRegularTokenTime(gameTime + cooldownTicks);
         BlockPopsMod.LOGGER
            .debug("Granted regular token to {}. Total: {}/{}", player.getName().getString(), discovery.getRegularTokens(), maxTokens);
         return true;
      } else {
         return false;
      }
   }

   private static boolean processSpecialTokenReset(IPlayerDiscovery discovery) {
      long lastUpdateMillis = discovery.getLastSpecialTokenResetTimestamp();
      if (lastUpdateMillis == 0L) {
         discovery.setLastSpecialTokenResetTimestamp(System.currentTimeMillis());
         return false;
      } else {
         ServerConfig config = ServerConfig.getInstance();
         return processSpecialTokenResetDaily(discovery, config);
      }
   }

   private static boolean processSpecialTokenResetDaily(IPlayerDiscovery discovery, ServerConfig config) {
      long lastUpdateMillis = discovery.getLastSpecialTokenResetTimestamp();
      ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
      int resetHour = config.getGuaranteedTokenResetHour();
      ZonedDateTime todayReset = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);
      ZonedDateTime mostRecentReset;
      if (now.isBefore(todayReset)) {
         mostRecentReset = todayReset.minusDays(1L);
      } else {
         mostRecentReset = todayReset;
      }

      if (lastUpdateMillis < mostRecentReset.toInstant().toEpochMilli()) {
         discovery.setLastSpecialTokenResetTimestamp(System.currentTimeMillis());
         if (discovery.hasUsedTodaySpecialToken()) {
            discovery.setUsedTodaySpecialToken(false);
            BlockPopsMod.LOGGER.debug("Daily token reset for player (Reset point was: {})", mostRecentReset);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void sendSyncPacket(ServerPlayer player, IPlayerDiscovery discovery) {
      ServerLevel world = player.serverLevel();
      long gameTime = world.getGameTime();
      long nextRegularTime = discovery.getNextRegularTokenTime();
      long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
      long millisUntilReset = calculateMillisUntilNextReset();
      SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
   }

   public static long calculateMillisUntilNextReset() {
      ServerConfig config = ServerConfig.getInstance();
      int resetHour = config.getGuaranteedTokenResetHour();
      ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
      ZonedDateTime nextReset = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);
      if (now.getHour() >= resetHour) {
         nextReset = nextReset.plusDays(1L);
      }

      return nextReset.toInstant().toEpochMilli() - now.toInstant().toEpochMilli();
   }
}
