package com.theplumteam.client.token;

import com.theplumteam.network.SyncTokenDataPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTokenManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClientTokenManager.class);
   private static int regularTokens = 0;
   private static boolean hasSpecialToken = false;
   private static long ticksUntilNextRegular = 0L;
   private static long millisUntilNextSpecialReset = 0L;
   private static long lastUpdateTime = System.currentTimeMillis();

   public static void update(SyncTokenDataPacket packet) {
      regularTokens = packet.getRegularTokens();
      ticksUntilNextRegular = packet.getTicksUntilNextRegular();
      hasSpecialToken = packet.hasSpecialToken();
      millisUntilNextSpecialReset = packet.getMillisUntilNextSpecialReset();
      lastUpdateTime = System.currentTimeMillis();
      LOGGER.debug(
         "Token data updated: {} regular, special: {}, next regular in {} ticks",
         new Object[]{regularTokens, hasSpecialToken ? "available" : "used", ticksUntilNextRegular}
      );
   }

   public static int getRegularTokens() {
      return regularTokens;
   }

   public static boolean hasSpecialToken() {
      return hasSpecialToken;
   }

   public static long getTicksUntilNextRegular() {
      return Math.max(0L, ticksUntilNextRegular);
   }

   public static long getMillisUntilNextSpecialReset() {
      long timeSinceUpdate = System.currentTimeMillis() - lastUpdateTime;
      return Math.max(0L, millisUntilNextSpecialReset - timeSinceUpdate);
   }

   public static String formatNextRegularTime() {
      long ticks = getTicksUntilNextRegular();
      if (ticks <= 0L) {
         return "Ready!";
      } else {
         long totalSeconds = ticks / 20L;
         long hours = totalSeconds / 3600L;
         long minutes = totalSeconds % 3600L / 60L;
         long seconds = totalSeconds % 60L;
         if (hours > 0L) {
            return String.format("%dh %dm", hours, minutes);
         } else {
            return minutes > 0L ? String.format("%dm %ds", minutes, seconds) : String.format("%ds", seconds);
         }
      }
   }

   public static String formatNextSpecialResetTime() {
      long millis = getMillisUntilNextSpecialReset();
      if (millis <= 0L) {
         return "Resetting...";
      } else {
         long totalSeconds = millis / 1000L;
         long hours = totalSeconds / 3600L;
         long minutes = totalSeconds % 3600L / 60L;
         return hours > 0L ? String.format("%dh %dm", hours, minutes) : String.format("%dm", minutes);
      }
   }

   public static void clear() {
      regularTokens = 0;
      hasSpecialToken = false;
      ticksUntilNextRegular = 0L;
      millisUntilNextSpecialReset = 0L;
      lastUpdateTime = System.currentTimeMillis();
      LOGGER.debug("Token data cleared");
   }
}
