package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.figure.PlayerCollectionHelper;
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

public class SetFavoriteColorPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SetFavoriteColorPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "set_favorite_color");
   private final String colorName;

   public SetFavoriteColorPacket(String colorName) {
      this.colorName = colorName;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_10814(this.colorName);
      return buffer;
   }

   public static SetFavoriteColorPacket decode(class_2540 buffer) {
      String colorName = buffer.method_19772();
      return new SetFavoriteColorPacket(colorName);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      SetFavoriteColorPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);

            try {
               PopBlockColor color = PopBlockColor.valueOf(packet.colorName.toUpperCase());
               discovery.setFavoriteColor(color);
               discovery.setHasChosenFavoriteColor(true);
               PlayerDataManager.markDirty(player, discovery);
               BlockPopsMod.logDebug("Player {} chose favorite color: {}", player.method_5477().getString(), color.method_15434());
               if (player.method_5682() != null) {
                  PlayerCollectionHelper.regenerateAndSyncPlayerCollection(player.method_5682());
                  BlockPopsMod.logDebug("Regenerated World Players collection after {} changed their favorite color", player.method_5477().getString());
               }
            } catch (IllegalArgumentException var5) {
               LOGGER.warn("Player {} sent invalid color name: {}", player.method_5477().getString(), packet.colorName);
            }
         }
      });
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
