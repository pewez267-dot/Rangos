package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.server.ServerTickHandler;
import com.theplumteam.server.config.ServerConfig;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateGuaranteedResetHourPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UpdateGuaranteedResetHourPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "update_guaranteed_reset_hour");
   private final int resetHour;

   public UpdateGuaranteedResetHourPacket(int resetHour) {
      this.resetHour = resetHour;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.resetHour);
      return buffer;
   }

   public static UpdateGuaranteedResetHourPacket decode(class_2540 buffer) {
      return new UpdateGuaranteedResetHourPacket(buffer.readInt());
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      UpdateGuaranteedResetHourPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            if (player.method_5687(2)) {
               ServerConfig.getInstance().setGuaranteedTokenResetHour(packet.resetHour);
               BlockPopsMod.logDebug("Player {} updated guaranteed token reset hour to {} UTC", player.method_5477().getString(), packet.resetHour);
               syncTokenData(player);
            } else {
               LOGGER.warn("Player {} tried to update reset hour without permission", player.method_5477().getString());
            }
         }
      });
   }

   private static void syncTokenData(class_3222 player) {
      IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
      long gameTime = player.method_51469().method_8510();
      long nextRegularTime = discovery.getNextRegularTokenTime();
      long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
      long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
      SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
