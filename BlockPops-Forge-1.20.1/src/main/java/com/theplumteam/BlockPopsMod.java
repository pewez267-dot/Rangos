package com.theplumteam;

import com.theplumteam.network.ModNetworking;
import com.theplumteam.server.ServerTickHandler;
import com.theplumteam.server.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (loader-agnostic) entry point for BlockPops.
 * Ported from Fabric 1.21.1 to Forge 1.20.1. Original mod by ThePlumTeam.
 */
public final class BlockPopsMod {
   public static final String MOD_ID = "blockpops";
   public static final Logger LOGGER = LoggerFactory.getLogger("blockpops");
   public static volatile boolean LOCAL_ADMIN;

   public static boolean isDebugLogging() {
      try {
         return ServerConfig.getInstance().isDebugLogging();
      } catch (Exception var1) {
         return false;
      }
   }

   public static void logDebug(String message) {
      if (isDebugLogging()) {
         LOGGER.info(message);
      }
   }

   public static void logDebug(String message, Object... args) {
      if (isDebugLogging()) {
         LOGGER.info(message, args);
      }
   }

   public static void init() {
      LOGGER.info("Initializing BlockPops mod");
      ModNetworking.init();
      ServerTickHandler.init();
      LOGGER.info("BlockPops mod initialization complete");
   }
}
