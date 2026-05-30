package com.theplumteam.figure;

import com.mojang.authlib.GameProfile;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.data.PlayerDiscovery;
import com.theplumteam.data.forge.StateSaverAndLoader;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelResource;

public class PlayerCollectionHelper {
   public static final String WORLD_PLAYERS_COLLECTION_ID = "world_players";
   private static final String COLLECTION_NAME = "World Players";

   public static FigureCollection generate(MinecraftServer server) {
      try {
         Path worldPath = server.getWorldPath(LevelResource.ROOT);
         File playerdataDir = worldPath.resolve("playerdata").toFile();
         List<FigureDefinition> playerFigures = new ArrayList<>();
         Set<UUID> processedPlayers = new HashSet<>();
         GameProfileCache profileCache = server.getProfileCache();
         ResourceLocation defaultModel = new ResourceLocation("blockpops", "geo/figure/box_figure_default.geo.json");
         ResourceLocation defaultAnimation = new ResourceLocation("blockpops", "animations/figure/box_figure_default.animation.json");
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
                        Optional<GameProfile> profile = profileCache.get(playerUUID);
                        if (profile.isPresent()) {
                           playerName = profile.get().getName();
                        } else {
                           playerName = "Player " + uuidString.substring(0, 8);
                        }
                     }

                     PopBlockColor defaultColor = ServerConfig.getInstance().getDefaultPlayerColor();
                     PopBlockColor favoriteColor = defaultColor;
                     ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerUUID);
                     if (onlinePlayer != null) {
                        IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(onlinePlayer);
                        if (discovery.hasChosenFavoriteColor() && discovery.getFavoriteColor() != null) {
                           favoriteColor = discovery.getFavoriteColor();
                        }

                        BlockPopsMod.LOGGER.debug("Loaded favorite color from online player {}: {}", playerName, favoriteColor.getSerializedName());
                     } else {
                        try {
                           CompoundTag stored = StateSaverAndLoader.getExistingPlayerState(server, playerUUID);
                           if (stored.contains(PlayerDataManager.DATA_KEY, Tag.TAG_COMPOUND)) {
                              PlayerDiscovery offlineDiscovery = new PlayerDiscovery();
                              offlineDiscovery.deserializeNBT(stored.getCompound(PlayerDataManager.DATA_KEY));
                              if (offlineDiscovery.hasChosenFavoriteColor() && offlineDiscovery.getFavoriteColor() != null) {
                                 favoriteColor = offlineDiscovery.getFavoriteColor();
                              }
                           }

                           BlockPopsMod.LOGGER.debug("Loaded favorite color from saved data for offline player {}: {}", playerName, favoriteColor.getSerializedName());
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

         for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            UUID playerUUIDx = onlinePlayer.getUUID();
            if (!processedPlayers.contains(playerUUIDx)) {
               String playerNamex = onlinePlayer.getName().getString();
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
         ResourceLocation boxTexture = new ResourceLocation("blockpops", "textures/block/box/default.png");
         ResourceLocation logoTexture = new ResourceLocation("blockpops", "textures/block/box/logo/logo_worldplayers.png");
         FigureCollection.LogoConfig logoConfig = new FigureCollection.LogoConfig(logoTexture, -0.915F, -0.165F, -0.001F, 4.004F, 4.503F, 1.0F);
         return new FigureCollection("world_players", "World Players", "Minecraft", null, boxTexture, logoConfig, playerFigures, new int[]{152, 48, 167});
      } catch (Exception var28) {
         BlockPopsMod.LOGGER.error("Failed to generate World Players collection", var28);
         return createEmptyCollection();
      }
   }

   private static FigureCollection createEmptyCollection() {
      ResourceLocation boxTexture = new ResourceLocation("blockpops", "textures/block/box/default.png");
      ResourceLocation logoTexture = new ResourceLocation("blockpops", "textures/block/box/logo/logo_worldplayers.png");
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
