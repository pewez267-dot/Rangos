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
import com.theplumteam.server.ServerTickHandler;
import com.theplumteam.server.config.ServerConfig;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropBoxPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(DropBoxPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "drop_box");
   private final class_2338 pos;
   private final String collectionId;
   private final TokenType tokenType;

   public DropBoxPacket(class_2338 pos, String collectionId, TokenType tokenType) {
      this.pos = pos;
      this.collectionId = collectionId;
      this.tokenType = tokenType;
   }

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_10807(this.pos);
      buffer.method_10814(this.collectionId);
      buffer.method_10817(this.tokenType);
      return buffer;
   }

   public static DropBoxPacket decode(class_2540 buffer) {
      class_2338 pos = buffer.method_10811();
      String collectionId = buffer.method_19772();
      TokenType tokenType = (TokenType)buffer.method_10818(TokenType.class);
      return new DropBoxPacket(pos, collectionId, tokenType);
   }

   public static void handleServer(class_2540 buf, PacketContext context) {
      DropBoxPacket packet = decode(buf);
      BlockPopsMod.logDebug(
         "Received drop box packet on server - Position: {}, Collection ID: {}, Token Type: {}", packet.pos, packet.collectionId, packet.tokenType
      );
      context.queue(() -> {
         if (context.getPlayer() instanceof class_3222 player) {
            BlockPopsMod.logDebug("Player: {} - Processing {} token request", player.method_5477().getString(), packet.tokenType);
            if (player.method_31548().method_7376() == -1) {
               player.method_43496(class_2561.method_43470("§cInventory is full! Cannot receive figure box."));
               BlockPopsMod.logDebug("Player {} inventory is full, token not consumed", player.method_5477().getString());
               return;
            }

            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
            if (!verifyAndConsumeToken(player, discovery, packet.tokenType)) {
               LOGGER.warn("Player {} tried to use unavailable {} token", player.method_5477().getString(), packet.tokenType);
               syncTokenDataToClient(player, discovery);
               return;
            }

            PlayerDataManager.markDirty(player, discovery);
            processBoxDrop(player, packet, discovery);
         }
      });
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
            Object skinIdObj = getSkinIdMethod.invoke(appearance);
            if (skinIdObj != null) {
               String skinId = (String)skinIdObj;
               BlockPopsMod.logDebug("Found QuickSkin ID for {}: {}", playerId, skinId);
               return skinId;
            }
         } else {
            LOGGER.debug("No QuickSkin appearance found for {}", playerId);
         }
      } catch (ClassNotFoundException var10) {
         LOGGER.debug("QuickSkin server classes not found");
      } catch (Exception var11) {
         LOGGER.warn("Error accessing QuickSkin server data: {}", var11.getMessage());
      }

      return null;
   }

   private static void processBoxDrop(class_3222 player, DropBoxPacket packet, IPlayerDiscovery discovery) {
      CollectionRegistry.getCollection(packet.collectionId).ifPresent(collection -> {
         List<FigureDefinition> figures = collection.getFigures();
         if (!figures.isEmpty()) {
            FigureDefinition selectedFigure = selectFigure(figures, packet.tokenType, discovery, packet.collectionId);
            class_1799 boxItem = null;
            if (packet.collectionId.equals("world_players")) {
               PopBlockColor color = selectedFigure.getFavoriteColor();
               if (color == null) {
                  color = PopBlockColor.ORIGINAL;
               }

               boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
            } else if (ModItems.BOX_BLOCK_ITEMS.containsKey(packet.collectionId)) {
               boxItem = new class_1799((class_1935)ModItems.BOX_BLOCK_ITEMS.get(packet.collectionId).get());
            } else {
               boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(PopBlockColor.ORIGINAL).get());
            }

            if (boxItem != null) {
               String uniqueFigureId = packet.collectionId + ":" + selectedFigure.getId();
               String skinSnapshot = null;
               String quickSkinSnapshot = null;
               if (selectedFigure.getType() == FigureType.PLAYER) {
                  GameProfile freshProfile = getFreshGameProfile(player, selectedFigure);
                  if (freshProfile != null && !freshProfile.getProperties().get("textures").isEmpty()) {
                     skinSnapshot = ((Property)freshProfile.getProperties().get("textures").iterator().next()).value();
                     discovery.saveFigureSkin(uniqueFigureId, skinSnapshot);
                     BlockPopsMod.logDebug("Saved/updated fresh skin snapshot for {}.", uniqueFigureId);
                  }

                  if (selectedFigure.getPlayerUUID() != null) {
                     String qsId = getQuickSkinIdFromServer(selectedFigure.getPlayerUUID());
                     if (qsId != null && !qsId.isEmpty()) {
                        quickSkinSnapshot = qsId;
                        discovery.saveFigureQuickSkin(uniqueFigureId, qsId);
                        BlockPopsMod.logDebug("Captured & Saved Quick Skin ID for figure {}: {}", uniqueFigureId, qsId);
                     }
                  }
               }

               if (!discovery.isDiscovered(uniqueFigureId)) {
                  discovery.discover(uniqueFigureId);
                  UnlockFigurePacket.sendToPlayer(player, uniqueFigureId, selectedFigure.getName(), skinSnapshot, quickSkinSnapshot);
                  BlockPopsMod.logDebug("Player {} discovered new figure: {} ({})", player.method_5477().getString(), selectedFigure.getName(), uniqueFigureId);
                  player.method_17356(class_3417.field_14709, class_3419.field_15248, 1.0F, 1.0F);
               } else {
                  player.method_17356(class_3417.field_14627, class_3419.field_15248, 1.0F, 1.0F);
               }

               class_2487 blockEntityTag = new class_2487();
               blockEntityTag.method_10582("FigureId", selectedFigure.getId());
               blockEntityTag.method_10582("CollectionId", packet.collectionId);
               if (packet.collectionId.equals("world_players")) {
                  PopBlockColor color = selectedFigure.getFavoriteColor();
                  if (color == null) {
                     color = PopBlockColor.ORIGINAL;
                  }

                  blockEntityTag.method_10582("Color", color.name());
               }

               if (skinSnapshot != null && !skinSnapshot.isEmpty()) {
                  blockEntityTag.method_10582("SkinSnapshot", skinSnapshot);
               } else if (selectedFigure.getType() == FigureType.PLAYER) {
                  String oldSnapshot = discovery.getFigureSkin(uniqueFigureId);
                  if (oldSnapshot != null && !oldSnapshot.isEmpty()) {
                     blockEntityTag.method_10582("SkinSnapshot", oldSnapshot);
                  }
               }

               if (quickSkinSnapshot != null) {
                  blockEntityTag.method_10582("QuickSkinId", quickSkinSnapshot);
               } else if (selectedFigure.getType() == FigureType.PLAYER) {
                  String oldQuickSkin = discovery.getFigureQuickSkin(uniqueFigureId);
                  if (oldQuickSkin != null && !oldQuickSkin.isEmpty()) {
                     blockEntityTag.method_10582("QuickSkinId", oldQuickSkin);
                     BlockPopsMod.logDebug("Used cached QuickSkin ID from discovery for {}", uniqueFigureId);
                  }
               }

               blockEntityTag.method_10582("id", "blockpops:box_block");
               boxItem.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
               player.method_31548().method_7394(boxItem);
               PlayerDataManager.markDirty(player, discovery);
            }
         }
      });
   }

   private static boolean verifyAndConsumeToken(class_3222 player, IPlayerDiscovery discovery, TokenType tokenType) {
      if (tokenType == TokenType.REGULAR) {
         if (discovery.getRegularTokens() > 0) {
            discovery.setRegularTokens(discovery.getRegularTokens() - 1);
            long var3 = player.method_51469().method_8510();
            if (discovery.getNextRegularTokenTime() <= var3) {
               long var5 = (long)ServerConfig.getInstance().getRegularTokenCooldownHours() * 72000L;
               discovery.setNextRegularTokenTime(var3 + var5);
            }

            BlockPopsMod.logDebug("Player {} used a regular token. Remaining: {}", player.method_5477().getString(), discovery.getRegularTokens());
            syncTokenDataToClient(player, discovery);
            return true;
         }
      } else if (tokenType == TokenType.GUARANTEED && !discovery.hasUsedTodaySpecialToken()) {
         discovery.setUsedTodaySpecialToken(true);
         BlockPopsMod.logDebug("Player {} used their guaranteed token", player.method_5477().getString());
         syncTokenDataToClient(player, discovery);
         return true;
      }

      return false;
   }

   private static void syncTokenDataToClient(class_3222 player, IPlayerDiscovery discovery) {
      long gameTime = player.method_51469().method_8510();
      long nextRegularTime = discovery.getNextRegularTokenTime();
      long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
      long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
      SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
   }

   private static FigureDefinition selectFigure(List<FigureDefinition> figures, TokenType tokenType, IPlayerDiscovery discovery, String collectionId) {
      Random random = new Random();
      if (tokenType == TokenType.GUARANTEED) {
         Set<String> discoveredSet = discovery.getDiscoveredSet();
         List<FigureDefinition> undiscoveredFigures = new ArrayList<>();

         for (FigureDefinition figure : figures) {
            String figureId = collectionId + ":" + figure.getId();
            if (!discoveredSet.contains(figureId)) {
               undiscoveredFigures.add(figure);
            }
         }

         if (!undiscoveredFigures.isEmpty()) {
            FigureDefinition selected = undiscoveredFigures.get(random.nextInt(undiscoveredFigures.size()));
            BlockPopsMod.logDebug("Guaranteed token logic: Selected undiscovered figure '{}'", selected.getId());
            return selected;
         } else {
            BlockPopsMod.logDebug("Guaranteed token logic: Collection complete, giving random duplicate");
            return figures.get(random.nextInt(figures.size()));
         }
      } else {
         return figures.get(random.nextInt(figures.size()));
      }
   }

   public void sendToServer() {
      NetworkManager.sendToServer(ID, this.encode());
   }
}
