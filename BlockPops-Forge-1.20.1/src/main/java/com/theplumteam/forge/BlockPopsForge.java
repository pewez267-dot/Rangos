package com.theplumteam.forge;

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
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.common.Mod;

@Mod("blockpops")
public class BlockPopsForge {
   public BlockPopsForge() {
      BlockPopsMod.logDebug("BlockPops loading on Forge platform");
      ModBlocks.register();
      ModItems.register();
      ModBlockEntities.register();
      ModCreativeTabs.register();
      BlockPopsMod.init();
      CommandRegistrationEvent.EVENT.register(ModCommands::register);
      this.registerServerEvents();
      this.registerInteractionEvents();
      BlockPopsMod.logDebug("BlockPops Forge initialization complete");
   }

   private void registerInteractionEvents() {
      InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
         if (hand != InteractionHand.MAIN_HAND) {
            return EventResult.pass();
         } else if (!player.isShiftKeyDown()) {
            return EventResult.pass();
         } else if (player.level().isClientSide()) {
            return EventResult.pass();
         } else {
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity instanceof FigureBlockEntity figureBlockEntity) {
               if (figureBlockEntity.hasFigure()) {
                  figureBlockEntity.cyclePose();
                  player.displayClientMessage(Component.literal("Pose changed to: " + figureBlockEntity.getPoseIndex()), true);
                  return EventResult.interruptTrue();
               } else {
                  return EventResult.pass();
               }
            } else {
               if (blockEntity instanceof BoxBlockEntity boxBlockEntity && boxBlockEntity.hasFigure() && boxBlockEntity.isFigureExtracted()) {
                  boxBlockEntity.cyclePose();
                  player.displayClientMessage(Component.literal("Pose changed to: " + boxBlockEntity.getPoseIndex()), true);
                  return EventResult.interruptTrue();
               }

               return EventResult.pass();
            }
         }
      });
   }

   private void registerServerEvents() {
      LifecycleEvent.SERVER_STARTING.register(server -> {
         BlockPopsMod.logDebug("Loading static collections on server...");
         CollectionRegistry.loadCollections(server.getResourceManager());
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
            player -> {
               VersionCheckPacket.enforce(player);
               if (player.getServer() != null) {
                  if (!CollectionRegistry.isInitialized() || CollectionRegistry.getAllCollections().size() <= 1) {
                     BlockPopsMod.logDebug("Collections not loaded yet, loading now from PLAYER_JOIN...");
                     CollectionRegistry.loadCollections(player.getServer().getResourceManager());
                  }

                  FigureCollection updatedPlayerCollection = PlayerCollectionHelper.generate(player.getServer());
                  CollectionRegistry.registerDynamicCollection(updatedPlayerCollection);
                  BlockPopsMod.LOGGER.debug("Updated World Players collection after player join: {}", player.getName().getString());
                  WorldConfig wc = WorldConfig.get(player.getServer());
                  Set<String> remoteIds = new HashSet<>(wc.getEnabledRemoteCollections());
                  List<FigureCollection> allCollections = CollectionRegistry.getAllCollections()
                     .stream()
                     .filter(c -> !remoteIds.contains(c.getId()))
                     .collect(Collectors.toList());
                  SyncDynamicCollectionsPacket.sendToPlayer(player, allCollections);
                  BlockPopsMod.logDebug("Synced {} collections to joining player {}", allCollections.size(), player.getName().getString());

                  List<FigureCollection> dynamicUpdate = new ArrayList<>();
                  dynamicUpdate.add(updatedPlayerCollection);

                  for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                     if (p != player) {
                        SyncDynamicCollectionsPacket.sendToPlayer(p, dynamicUpdate);
                     }
                  }

                  IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
                  SyncDiscoveryDataPacket.sendToPlayer(
                     player, discovery.getDiscoveredSet(), discovery.getAllFigureSkins(), discovery.getAllFigureQuickSkins()
                  );
                  BlockPopsMod.logDebug(
                     "Synced {} discovered figures, {} skins, and {} quick skins to {}",
                     discovery.getDiscoveredSet().size(),
                     discovery.getAllFigureSkins().size(),
                     discovery.getAllFigureQuickSkins().size(),
                     player.getName().getString()
                  );
                  long gameTime = player.serverLevel().getGameTime();
                  long nextRegularTime = discovery.getNextRegularTokenTime();
                  long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
                  long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
                  SyncTokenDataPacket.sendToPlayer(
                     player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset
                  );
                  BlockPopsMod.logDebug(
                     "Synced token data to {}: {} regular tokens, special: {}",
                     player.getName().getString(),
                     discovery.getRegularTokens(),
                     !discovery.hasUsedTodaySpecialToken() ? "available" : "used"
                  );
                  SyncServerConfigPacket.sendToPlayer(player);
                  if (!discovery.hasChosenFavoriteColor() && ServerConfig.getInstance().isShowColorSelectionOnJoin()) {
                     BlockPopsMod.logDebug(
                        "Player {} has not chosen a favorite color. Sending packet to open selection screen.", player.getName().getString()
                     );
                     OpenFavoriteColorScreenPacket.sendToPlayer(player);
                  }
               }
            }
         );
   }
}
