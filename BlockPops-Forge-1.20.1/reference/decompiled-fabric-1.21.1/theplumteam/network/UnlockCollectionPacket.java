package com.theplumteam.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import com.theplumteam.registry.ModItems;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.minecraft.class_1542;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2487;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockCollectionPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(UnlockCollectionPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "unlock_collection");
   private final String collectionId;

   public UnlockCollectionPacket(String collectionId) {
      this.collectionId = collectionId;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_10814(this.collectionId);
      return buffer;
   }

   public static UnlockCollectionPacket decode(class_2540 buffer) {
      String collectionId = buffer.method_19772();
      return new UnlockCollectionPacket(collectionId);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      UnlockCollectionPacket packet = decode(buf);
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            if (!player.method_5687(2)) {
               LOGGER.warn("Player {} tried to unlock collection without permission", player.method_5477().getString());
               return;
            }

            BlockPopsMod.logDebug("Player {} requested to unlock collection: {}", player.method_5477().getString(), packet.collectionId);
            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
            unlockEntireCollection(player, packet.collectionId, discovery);
            PlayerDataManager.markDirty(player, discovery);
         }
      });
   }

   private static void unlockEntireCollection(class_3222 player, String collectionId, IPlayerDiscovery discovery) {
      CollectionRegistry.getCollection(collectionId).ifPresent(collection -> {
         List<FigureDefinition> figures = collection.getFigures();
         if (figures.isEmpty()) {
            LOGGER.warn("Collection {} has no figures", collectionId);
         } else {
            BlockPopsMod.logDebug("Unlocking {} figures from collection {} for player {}", figures.size(), collectionId, player.method_5477().getString());

            for (FigureDefinition figure : figures) {
               giveBoxForFigure(player, collectionId, figure, discovery);
            }

            BlockPopsMod.logDebug("Successfully unlocked all figures from collection {} for player {}", collectionId, player.method_5477().getString());
         }
      });
   }

   @Nullable
   private static String getQuickSkinIdFromServer(UUID playerId) {
      try {
         Class<?> repoClass = Class.forName("com.quickskin.mod.server.data.ServerPlayerAppearanceRepository");
         Method getInstanceMethod = repoClass.getMethod("getInstance");
         Object repoInstance = getInstanceMethod.invoke(null);
         Method getAppearanceMethod = repoClass.getMethod("getAppearance", UUID.class);
         Object appearance = getAppearanceMethod.invoke(repoInstance, playerId);
         if (appearance != null) {
            Class<?> appearanceClass = appearance.getClass();
            Method getSkinIdMethod = appearanceClass.getMethod("getSkinId");
            return (String)getSkinIdMethod.invoke(appearance);
         }
      } catch (Exception var8) {
      }

      return null;
   }

   private static void giveBoxForFigure(class_3222 player, String collectionId, FigureDefinition figure, IPlayerDiscovery discovery) {
      class_1799 boxItem = null;
      if (collectionId.equals("world_players")) {
         PopBlockColor color = figure.getFavoriteColor();
         if (color == null) {
            color = PopBlockColor.ORIGINAL;
         }

         boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
      } else if (ModItems.BOX_BLOCK_ITEMS.containsKey(collectionId)) {
         boxItem = new class_1799((class_1935)ModItems.BOX_BLOCK_ITEMS.get(collectionId).get());
      } else {
         boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(PopBlockColor.ORIGINAL).get());
      }

      if (boxItem == null) {
         LOGGER.warn("Could not find box item for collection {}", collectionId);
      } else {
         String uniqueFigureId = collectionId + ":" + figure.getId();
         String skinSnapshot = null;
         String quickSkinSnapshot = null;
         if (figure.getType() == FigureType.PLAYER) {
            GameProfile freshProfile = getFreshGameProfile(player, figure);
            if (freshProfile != null && !freshProfile.getProperties().get("textures").isEmpty()) {
               skinSnapshot = ((Property)freshProfile.getProperties().get("textures").iterator().next()).value();
               discovery.saveFigureSkin(uniqueFigureId, skinSnapshot);
               LOGGER.debug("Saved skin snapshot for player figure: {}", uniqueFigureId);
            }

            if (figure.getPlayerUUID() != null) {
               String qsId = getQuickSkinIdFromServer(figure.getPlayerUUID());
               if (qsId != null && !qsId.isEmpty()) {
                  quickSkinSnapshot = qsId;
                  discovery.saveFigureQuickSkin(uniqueFigureId, qsId);
                  BlockPopsMod.logDebug("Captured & Saved Quick Skin ID for figure {}: {}", uniqueFigureId, qsId);
               }
            }
         }

         if (!discovery.isDiscovered(uniqueFigureId)) {
            discovery.discover(uniqueFigureId);
            UnlockFigurePacket.sendToPlayer(player, uniqueFigureId, figure.getName(), skinSnapshot, quickSkinSnapshot);
            LOGGER.debug("Unlocked new figure: {} ({})", figure.getName(), uniqueFigureId);
         }

         class_2487 blockEntityTag = new class_2487();
         blockEntityTag.method_10582("FigureId", figure.getId());
         blockEntityTag.method_10582("CollectionId", collectionId);
         if (collectionId.equals("world_players")) {
            PopBlockColor color = figure.getFavoriteColor();
            if (color == null) {
               color = PopBlockColor.ORIGINAL;
            }

            blockEntityTag.method_10582("Color", color.name());
         }

         if (skinSnapshot != null && !skinSnapshot.isEmpty()) {
            blockEntityTag.method_10582("SkinSnapshot", skinSnapshot);
         } else if (figure.getType() == FigureType.PLAYER) {
            String oldSnapshot = discovery.getFigureSkin(uniqueFigureId);
            if (oldSnapshot != null && !oldSnapshot.isEmpty()) {
               blockEntityTag.method_10582("SkinSnapshot", oldSnapshot);
            }
         }

         if (quickSkinSnapshot != null) {
            blockEntityTag.method_10582("QuickSkinId", quickSkinSnapshot);
         }

         blockEntityTag.method_10582("id", "blockpops:box_block");
         boxItem.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
         class_1542 itemEntity = new class_1542(player.method_37908(), player.method_23317(), player.method_23318() + 1.0, player.method_23321(), boxItem);
         itemEntity.method_18800(0.0, 0.2, 0.0);
         player.method_37908().method_8649(itemEntity);
      }
   }

   @Nullable
   private static GameProfile getFreshGameProfile(class_3222 player, FigureDefinition figure) {
      if (figure.getPlayerUUID() == null) {
         return null;
      } else {
         try {
            ProfileResult result = player.method_5682().method_3844().fetchProfile(figure.getPlayerUUID(), true);
            return result != null ? result.profile() : null;
         } catch (Exception var3) {
            LOGGER.error("Failed to fetch fresh GameProfile for {}: {}", figure.getName(), var3.getMessage());
            return null;
         }
      }
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
