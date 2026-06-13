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

public class ReloadTokensPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(ReloadTokensPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "reload_tokens");
   private final boolean reloadRegular;
   private final boolean reloadGuaranteed;

   public ReloadTokensPacket(boolean reloadRegular, boolean reloadGuaranteed) {
      this.reloadRegular = reloadRegular;
      this.reloadGuaranteed = reloadGuaranteed;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_52964(this.reloadRegular);
      buffer.method_52964(this.reloadGuaranteed);
      return buffer;
   }

   public static ReloadTokensPacket decode(class_2540 buffer) {
      boolean reloadRegular = buffer.readBoolean();
      boolean reloadGuaranteed = buffer.readBoolean();
      return new ReloadTokensPacket(reloadRegular, reloadGuaranteed);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      ReloadTokensPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            if (!player.method_5687(2)) {
               LOGGER.warn("Player {} tried to reload tokens without permission", player.method_5477().getString());
               return;
            }

            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
            if (packet.reloadRegular) {
               discovery.setRegularTokens(ServerConfig.getInstance().getMaxRegularTokens());
               discovery.setNextRegularTokenTime(0L);
               BlockPopsMod.logDebug("Reloaded regular tokens for player {}", player.method_5477().getString());
            }

            if (packet.reloadGuaranteed) {
               discovery.setUsedTodaySpecialToken(false);
               BlockPopsMod.logDebug("Reloaded guaranteed token for player {}", player.method_5477().getString());
            }

            PlayerDataManager.markDirty(player, discovery);
            long gameTime = player.method_51469().method_8510();
            long nextRegularTime = discovery.getNextRegularTokenTime();
            long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
            long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
            SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
