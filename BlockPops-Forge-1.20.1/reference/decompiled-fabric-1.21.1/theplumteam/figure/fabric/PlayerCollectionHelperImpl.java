package com.theplumteam.figure.fabric;

import com.mojang.authlib.GameProfile;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.network.SyncDynamicCollectionsPacket;
import com.theplumteam.server.config.ServerConfig;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_2487;
import net.minecraft.class_2505;
import net.minecraft.class_2507;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3312;
import net.minecraft.class_5218;
import net.minecraft.server.MinecraftServer;

public class PlayerCollectionHelperImpl {
   private static final String COLLECTION_ID = "world_players";
   private static final String COLLECTION_NAME = "World Players";

   public static FigureCollection generate(MinecraftServer server) {
      try {
         Path worldPath = server.method_27050(class_5218.field_24188);
         File playerdataDir = worldPath.resolve("playerdata").toFile();
         List<FigureDefinition> playerFigures = new ArrayList<>();
         Set<UUID> processedPlayers = new HashSet<>();
         class_3312 profileCache = server.method_3793();
         class_2960 defaultModel = class_2960.method_60655("blockpops", "geo/figure/box_figure_default.geo.json");
         class_2960 defaultAnimation = class_2960.method_60655("blockpops", "animations/figure/box_figure_default.animation.json");
         if (playerdataDir.exists() && playerdataDir.isDirectory()) {
            File[] playerFiles = playerdataDir.listFiles((dir, name) -> name.endsWith(".dat"));
            if (playerFiles != null && playerFiles.length > 0) {
               for (File playerFile : playerFiles) {
                  try {
                     String filename = playerFile.getName();
                     String uuidString = filename.substring(0, filename.length() - 4);
                     UUID playerUUID = UUID.fromString(uuidString);
                     String playerName = "Unknown Player";
                     if (profileCache != null) {
                        Optional<GameProfile> profile = profileCache.method_14512(playerUUID);
                        if (profile.isPresent()) {
                           playerName = profile.get().getName();
                        } else {
                           playerName = "Player " + uuidString.substring(0, 8);
                        }
                     }

                     PopBlockColor defaultColor = ServerConfig.getInstance().getDefaultPlayerColor();
                     PopBlockColor favoriteColor = defaultColor;
                     class_3222 onlinePlayer = server.method_3760().method_14602(playerUUID);
                     if (onlinePlayer != null) {
                        IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(onlinePlayer);
                        if (discovery.hasChosenFavoriteColor() && discovery.getFavoriteColor() != null) {
                           favoriteColor = discovery.getFavoriteColor();
                        }

                        BlockPopsMod.LOGGER.debug("Loaded favorite color from online player {}: {}", playerName, favoriteColor.method_15434());
                     } else {
                        try {
                           File playerDataFile = new File(playerdataDir, uuidString + ".dat");
                           if (playerDataFile.exists()) {
                              class_2487 playerData = class_2507.method_30613(playerDataFile.toPath(), class_2505.method_53898());
                              if (playerData != null) {
                                 class_2487 cardinalComponents = playerData.method_10562("cardinal_components");
                                 if (cardinalComponents.method_10545("blockpops:player_discovery")) {
                                    class_2487 discoveryTag = cardinalComponents.method_10562("blockpops:player_discovery");
                                    if (discoveryTag.method_10573("FavoriteColor", 8)) {
                                       try {
                                          favoriteColor = PopBlockColor.valueOf(discoveryTag.method_10558("FavoriteColor").toUpperCase());
                                       } catch (IllegalArgumentException var25) {
                                          BlockPopsMod.LOGGER.warn("Invalid favorite color found for player {}, defaulting to ORIGINAL", playerUUID);
                                       }
                                    }
                                 }
                              }
                           }

                           BlockPopsMod.LOGGER.debug("Loaded favorite color from disk for offline player {}: {}", playerName, favoriteColor.method_15434());
                        } catch (Exception var26) {
                           BlockPopsMod.LOGGER.warn("Failed to load favorite color for player {}, defaulting to ORIGINAL: {}", playerUUID, var26.getMessage());
                        }
                     }

                     FigureDefinition playerFigure = new FigureDefinition(uuidString, playerName, defaultModel, defaultAnimation, playerUUID, favoriteColor);
                     playerFigures.add(playerFigure);
                     processedPlayers.add(playerUUID);
                     BlockPopsMod.LOGGER.debug("Added player figure from .dat file: {} ({})", playerName, playerUUID);
                  } catch (IllegalArgumentException var27) {
                     BlockPopsMod.LOGGER.warn("Failed to parse player UUID from file: {}", playerFile.getName());
                  }
               }
            }
         }

         for (class_3222 onlinePlayer : server.method_3760().method_14571()) {
            UUID playerUUIDx = onlinePlayer.method_5667();
            if (!processedPlayers.contains(playerUUIDx)) {
               String playerNamex = onlinePlayer.method_5477().getString();
               String uuidStringx = playerUUIDx.toString();
               PopBlockColor defaultColorOnline = ServerConfig.getInstance().getDefaultPlayerColor();
               PopBlockColor favoriteColor = defaultColorOnline;
               IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(onlinePlayer);
               if (discovery.hasChosenFavoriteColor() && discovery.getFavoriteColor() != null) {
                  favoriteColor = discovery.getFavoriteColor();
               }

               FigureDefinition playerFigure = new FigureDefinition(uuidStringx, playerNamex, defaultModel, defaultAnimation, playerUUIDx, favoriteColor);
               playerFigures.add(playerFigure);
               processedPlayers.add(playerUUIDx);
               BlockPopsMod.LOGGER.debug("Added online player figure (no .dat file yet): {} ({})", playerNamex, playerUUIDx);
            }
         }

         BlockPopsMod.logDebug("Generated World Players collection with {} figures", playerFigures.size());
         class_2960 boxTexture = class_2960.method_60655("blockpops", "textures/block/box/default.png");
         class_2960 logoTexture = class_2960.method_60655("blockpops", "textures/block/box/logo/logo_worldplayers.png");
         FigureCollection.LogoConfig logoConfig = new FigureCollection.LogoConfig(logoTexture, -0.915F, -0.165F, -0.001F, 4.004F, 4.503F, 1.0F);
         return new FigureCollection("world_players", "World Players", "Minecraft", null, boxTexture, logoConfig, playerFigures, new int[]{152, 48, 167});
      } catch (Exception var28) {
         BlockPopsMod.LOGGER.error("Failed to generate World Players collection", var28);
         return createEmptyCollection();
      }
   }

   private static FigureCollection createEmptyCollection() {
      class_2960 boxTexture = class_2960.method_60655("blockpops", "textures/block/box/default.png");
      class_2960 logoTexture = class_2960.method_60655("blockpops", "textures/block/box/logo/logo_worldplayers.png");
      FigureCollection.LogoConfig logoConfig = new FigureCollection.LogoConfig(logoTexture, -0.915F, -0.165F, -0.001F, 4.004F, 4.503F, 1.0F);
      return new FigureCollection("world_players", "World Players", "Minecraft", null, boxTexture, logoConfig, new ArrayList<>(), new int[]{152, 48, 167});
   }

   public static void regenerateAndSyncPlayerCollection(MinecraftServer server) {
      FigureCollection updatedCollection = generate(server);
      CollectionRegistry.registerDynamicCollection(updatedCollection);
      BlockPopsMod.logDebug("Regenerated World Players collection");
      List<FigureCollection> dynamicCollections = new ArrayList<>();
      dynamicCollections.add(updatedCollection);
      SyncDynamicCollectionsPacket.sendToAllPlayers(server, dynamicCollections);
   }
}
