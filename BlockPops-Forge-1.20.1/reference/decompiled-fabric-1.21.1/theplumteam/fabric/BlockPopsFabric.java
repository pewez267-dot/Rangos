package com.theplumteam.fabric;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.FigureBlockEntity;
import com.theplumteam.command.ModCommands;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.PlayerCollectionHelper;
import com.theplumteam.network.OpenFavoriteColorScreenPacket;
import com.theplumteam.network.SyncDiscoveryDataPacket;
import com.theplumteam.network.SyncDynamicCollectionsPacket;
import com.theplumteam.network.SyncServerConfigPacket;
import com.theplumteam.network.SyncTokenDataPacket;
import com.theplumteam.network.VersionCheckPacket;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.registry.ModBlocks;
import com.theplumteam.registry.ModCreativeTabs;
import com.theplumteam.registry.ModItems;
import com.theplumteam.server.ServerCollectionLoader;
import com.theplumteam.server.ServerTickHandler;
import com.theplumteam.server.config.ServerConfig;
import com.theplumteam.server.config.WorldConfig;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.LifecycleEvent.ServerState;
import dev.architectury.event.events.common.PlayerEvent.PlayerJoin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_3222;

public class BlockPopsFabric implements ModInitializer {
   public void onInitialize() {
      BlockPopsMod.logDebug("BlockPops loading on Fabric platform");
      ModBlocks.register();
      ModItems.register();
      ModBlockEntities.register();
      ModCreativeTabs.register();
      BlockPopsMod.init();
      CommandRegistrationEvent.EVENT.register(ModCommands::register);
      this.registerServerEvents();
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, world, hand, hitResult) -> {
         if (hand != class_1268.field_5808) {
            return class_1269.field_5811;
         } else if (!player.method_5715()) {
            return class_1269.field_5811;
         } else if (world.method_8608()) {
            return class_1269.field_5811;
         } else {
            class_2586 blockEntity = world.method_8321(hitResult.method_17777());
            if (blockEntity instanceof FigureBlockEntity figureBlockEntity) {
               if (figureBlockEntity.hasFigure()) {
                  figureBlockEntity.cyclePose();
                  player.method_7353(class_2561.method_43470("Pose changed to: " + figureBlockEntity.getPoseIndex()), true);
                  return class_1269.field_5812;
               } else {
                  return class_1269.field_5811;
               }
            } else {
               if (blockEntity instanceof BoxBlockEntity boxBlockEntity && boxBlockEntity.hasFigure() && boxBlockEntity.isFigureExtracted()) {
                  boxBlockEntity.cyclePose();
                  player.method_7353(class_2561.method_43470("Pose changed to: " + boxBlockEntity.getPoseIndex()), true);
                  return class_1269.field_5812;
               }

               return class_1269.field_5811;
            }
         }
      });
      BlockPopsMod.logDebug("BlockPops Fabric initialization complete");
   }

   private void registerServerEvents() {
      LifecycleEvent.SERVER_STARTING.register((ServerState)server -> {
         BlockPopsMod.logDebug("Loading static collections on server...");
         CollectionRegistry.loadCollections(server.method_34864());
         BlockPopsMod.logDebug("Generating World Players collection...");
         FigureCollection playerCollection = PlayerCollectionHelper.generate(server);
         CollectionRegistry.registerDynamicCollection(playerCollection);
         WorldConfig worldConfig = WorldConfig.get(server);
         Set<String> enabledRemote = new HashSet<>(worldConfig.getEnabledRemoteCollections());
         if (!enabledRemote.isEmpty()) {
            ServerCollectionLoader.loadCollections(enabledRemote);
         }
      });
      PlayerEvent.PLAYER_JOIN
         .register(
            (PlayerJoin)player -> {
               VersionCheckPacket.enforce(player);
               if (player.method_5682() != null) {
                  if (!CollectionRegistry.isInitialized() || CollectionRegistry.getAllCollections().size() <= 1) {
                     BlockPopsMod.logDebug("Collections not loaded yet, loading now from PLAYER_JOIN...");
                     CollectionRegistry.loadCollections(player.method_5682().method_34864());
                  }

                  FigureCollection updatedPlayerCollection = PlayerCollectionHelper.generate(player.method_5682());
                  CollectionRegistry.registerDynamicCollection(updatedPlayerCollection);
                  BlockPopsMod.LOGGER.debug("Updated World Players collection after player join: {}", player.method_5477().getString());
                  if (player instanceof class_3222) {
                     WorldConfig wc = WorldConfig.get(player.method_5682());
                     Set<String> remoteIds = new HashSet<>(wc.getEnabledRemoteCollections());
                     List<FigureCollection> allCollections = CollectionRegistry.getAllCollections()
                        .stream()
                        .filter(c -> !remoteIds.contains(c.getId()))
                        .collect(Collectors.toList());
                     SyncDynamicCollectionsPacket.sendToPlayer(player, allCollections);
                     BlockPopsMod.logDebug("Synced {} collections to joining player {}", allCollections.size(), player.method_5477().getString());
                  }

                  List<FigureCollection> dynamicUpdate = new ArrayList<>();
                  dynamicUpdate.add(updatedPlayerCollection);

                  for (class_3222 p : player.method_5682().method_3760().method_14571()) {
                     if (p != player) {
                        SyncDynamicCollectionsPacket.sendToPlayer(p, dynamicUpdate);
                     }
                  }

                  if (player instanceof class_3222) {
                     IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
                     SyncDiscoveryDataPacket.sendToPlayer(
                        player, discovery.getDiscoveredSet(), discovery.getAllFigureSkins(), discovery.getAllFigureQuickSkins()
                     );
                     BlockPopsMod.logDebug(
                        "Synced {} discovered figures, {} skins, and {} quick skins to {}",
                        discovery.getDiscoveredSet().size(),
                        discovery.getAllFigureSkins().size(),
                        discovery.getAllFigureQuickSkins().size(),
                        player.method_5477().getString()
                     );
                     long gameTime = player.method_51469().method_8510();
                     long nextRegularTime = discovery.getNextRegularTokenTime();
                     long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
                     long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
                     SyncTokenDataPacket.sendToPlayer(
                        player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset
                     );
                     BlockPopsMod.logDebug(
                        "Synced token data to {}: {} regular tokens, special: {}",
                        player.method_5477().getString(),
                        discovery.getRegularTokens(),
                        !discovery.hasUsedTodaySpecialToken() ? "available" : "used"
                     );
                     SyncServerConfigPacket.sendToPlayer(player);
                     if (!discovery.hasChosenFavoriteColor() && ServerConfig.getInstance().isShowColorSelectionOnJoin()) {
                        BlockPopsMod.logDebug(
                           "Player {} has not chosen a favorite color. Sending packet to open selection screen.", player.method_5477().getString()
                        );
                        OpenFavoriteColorScreenPacket.sendToPlayer(player);
                     }
                  }
               }
            }
         );
   }
}
