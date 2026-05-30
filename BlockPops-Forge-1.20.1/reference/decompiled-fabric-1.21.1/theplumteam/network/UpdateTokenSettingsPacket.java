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

public class UpdateTokenSettingsPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTokenSettingsPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "update_token_settings");
   private final int regularTokenCooldownHours;
   private final int maxRegularTokens;
   private final int guaranteedTokenResetHour;

   public UpdateTokenSettingsPacket(int regularTokenCooldownHours, int maxRegularTokens, int guaranteedTokenResetHour) {
      this.regularTokenCooldownHours = regularTokenCooldownHours;
      this.maxRegularTokens = maxRegularTokens;
      this.guaranteedTokenResetHour = guaranteedTokenResetHour;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.regularTokenCooldownHours);
      buffer.method_53002(this.maxRegularTokens);
      buffer.method_53002(this.guaranteedTokenResetHour);
      return buffer;
   }

   public static UpdateTokenSettingsPacket decode(class_2540 buffer) {
      int regularTokenCooldownHours = buffer.readInt();
      int maxRegularTokens = buffer.readInt();
      int guaranteedTokenResetHour = buffer.readInt();
      return new UpdateTokenSettingsPacket(regularTokenCooldownHours, maxRegularTokens, guaranteedTokenResetHour);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      UpdateTokenSettingsPacket packet = decode(buf);
      context.queue(
         () -> {
            if (context.getPlayer() instanceof class_3222 player) {
               if (!player.method_5687(2)) {
                  LOGGER.warn("Player {} tried to update token settings without permission", player.method_5477().getString());
                  return;
               }

               ServerConfig config = ServerConfig.getInstance();
               config.regularTokenCooldownHours = Math.max(1, Math.min(168, packet.regularTokenCooldownHours));
               config.maxRegularTokens = Math.max(1, Math.min(99, packet.maxRegularTokens));
               config.guaranteedTokenResetHour = Math.max(0, Math.min(23, packet.guaranteedTokenResetHour));
               config.save();
               BlockPopsMod.logDebug(
                  "Player {} updated token settings: cooldown={}h, maxTokens={}, resetHour={}",
                  player.method_5477().getString(),
                  config.getRegularTokenCooldownHours(),
                  config.getMaxRegularTokens(),
                  config.getGuaranteedTokenResetHour()
               );
               SyncServerConfigPacket.broadcastToAll(player.method_5682());

               for (class_3222 p : player.method_5682().method_3760().method_14571()) {
                  IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(p);
                  long gameTime = p.method_51469().method_8510();
                  long nextRegularTime = discovery.getNextRegularTokenTime();
                  long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
                  long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
                  SyncTokenDataPacket.sendToPlayer(p, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
               }
            }
         }
      );
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
