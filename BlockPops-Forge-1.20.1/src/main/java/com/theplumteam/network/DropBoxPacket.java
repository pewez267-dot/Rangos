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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropBoxPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(DropBoxPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "drop_box");
   private final BlockPos pos;
   private final String collectionId;
   private final TokenType tokenType;

   public DropBoxPacket(BlockPos pos, String collectionId, TokenType tokenType) {
      this.pos = pos;
      this.collectionId = collectionId;
      this.tokenType = tokenType;
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeBlockPos(this.pos);
      buffer.writeUtf(this.collectionId);
      buffer.writeEnum(this.tokenType);
      return buffer;
   }

   public static DropBoxPacket decode(FriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      String collectionId = buffer.readUtf();
      TokenType tokenType = buffer.readEnum(TokenType.class);
      return new DropBoxPacket(pos, collectionId, tokenType);
   }

   public static void handleServer(FriendlyByteBuf buf, PacketContext context) {
      DropBoxPacket packet = decode(buf);
      BlockPopsMod.logDebug(
         "Received drop box packet on server - Position: {}, Collection ID: {}, Token Type: {}", packet.pos, packet.collectionId, packet.tokenType
      );
      context.queue(() -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            BlockPopsMod.logDebug("Player: {} - Processing {} token request", player.getName().getString(), packet.tokenType);
            if (player.getInventory().getFreeSlot() == -1) {
               player.sendSystemMessage(Component.literal("§cInventory is full! Cannot receive figure box."));
               BlockPopsMod.logDebug("Player {} inventory is full, token not consumed", player.getName().getString());
               return;
            }

            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
            if (!verifyAndConsumeToken(player, discovery, packet.tokenType)) {
               LOGGER.warn("Player {} tried to use unavailable {} token", player.getName().getString(), packet.tokenType);
               syncTokenDataToClient(player, discovery);
               return;
            }

            PlayerDataManager.markDirty(player, discovery);
            processBoxDrop(player, packet, discovery);
         }
      });
   }

   @Nullable
   private static GameProfile getFreshGameProfile(ServerPlayer player, FigureDefinition figure) {
      if (figure.getPlayerUUID() == null) {
         return null;
      } else {
         try {
            ProfileResult result = player.getServer().getSessionService().fetchProfile(figure.getPlayerUUID(), true);
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

   private static void processBoxDrop(ServerPlayer player, DropBoxPacket packet, IPlayerDiscovery discovery) {
      CollectionRegistry.getCollection(packet.collectionId).ifPresent(collection -> {
         List<FigureDefinition> figures = collection.getFigures();
         if (!figures.isEmpty()) {
            FigureDefinition selectedFigure = selectFigure(figures, packet.tokenType, discovery, packet.collectionId);
            ItemStack boxItem;
            if (packet.collectionId.equals("world_players")) {
               PopBlockColor color = selectedFigure.getFavoriteColor();
               if (color == null) {
                  color = PopBlockColor.ORIGINAL;
               }

               boxItem = new ItemStack(ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
            } else if (ModItems.BOX_BLOCK_ITEMS.containsKey(packet.collectionId)) {
               boxItem = new ItemStack(ModItems.BOX_BLOCK_ITEMS.get(packet.collectionId).get());
            } else {
               boxItem = new ItemStack(ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(PopBlockColor.ORIGINAL).get());
            }

            String uniqueFigureId = packet.collectionId + ":" + selectedFigure.getId();
            String skinSnapshot = null;
            String quickSkinSnapshot = null;
            if (selectedFigure.getType() == FigureType.PLAYER) {
               GameProfile freshProfile = getFreshGameProfile(player, selectedFigure);
               if (freshProfile != null && !freshProfile.getProperties().get("textures").isEmpty()) {
                  skinSnapshot = freshProfile.getProperties().get("textures").iterator().next().value();
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
               BlockPopsMod.logDebug("Player {} discovered new figure: {} ({})", player.getName().getString(), selectedFigure.getName(), uniqueFigureId);
               player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
               player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.putString("FigureId", selectedFigure.getId());
            blockEntityTag.putString("CollectionId", packet.collectionId);
            if (packet.collectionId.equals("world_players")) {
               PopBlockColor color = selectedFigure.getFavoriteColor();
               if (color == null) {
                  color = PopBlockColor.ORIGINAL;
               }

               blockEntityTag.putString("Color", color.name());
            }

            if (skinSnapshot != null && !skinSnapshot.isEmpty()) {
               blockEntityTag.putString("SkinSnapshot", skinSnapshot);
            } else if (selectedFigure.getType() == FigureType.PLAYER) {
               String oldSnapshot = discovery.getFigureSkin(uniqueFigureId);
               if (oldSnapshot != null && !oldSnapshot.isEmpty()) {
                  blockEntityTag.putString("SkinSnapshot", oldSnapshot);
               }
            }

            if (quickSkinSnapshot != null) {
               blockEntityTag.putString("QuickSkinId", quickSkinSnapshot);
            } else if (selectedFigure.getType() == FigureType.PLAYER) {
               String oldQuickSkin = discovery.getFigureQuickSkin(uniqueFigureId);
               if (oldQuickSkin != null && !oldQuickSkin.isEmpty()) {
                  blockEntityTag.putString("QuickSkinId", oldQuickSkin);
                  BlockPopsMod.logDebug("Used cached QuickSkin ID from discovery for {}", uniqueFigureId);
               }
            }

            blockEntityTag.putString("id", "blockpops:box_block");
            boxItem.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
            player.getInventory().add(boxItem);
            PlayerDataManager.markDirty(player, discovery);
         }
      });
   }

   private static boolean verifyAndConsumeToken(ServerPlayer player, IPlayerDiscovery discovery, TokenType tokenType) {
      if (tokenType == TokenType.REGULAR) {
         if (discovery.getRegularTokens() > 0) {
            discovery.setRegularTokens(discovery.getRegularTokens() - 1);
            long var3 = player.serverLevel().getGameTime();
            if (discovery.getNextRegularTokenTime() <= var3) {
               long var5 = (long)ServerConfig.getInstance().getRegularTokenCooldownHours() * 72000L;
               discovery.setNextRegularTokenTime(var3 + var5);
            }

            BlockPopsMod.logDebug("Player {} used a regular token. Remaining: {}", player.getName().getString(), discovery.getRegularTokens());
            syncTokenDataToClient(player, discovery);
            return true;
         }
      } else if (tokenType == TokenType.GUARANTEED && !discovery.hasUsedTodaySpecialToken()) {
         discovery.setUsedTodaySpecialToken(true);
         BlockPopsMod.logDebug("Player {} used their guaranteed token", player.getName().getString());
         syncTokenDataToClient(player, discovery);
         return true;
      }

      return false;
   }

   private static void syncTokenDataToClient(ServerPlayer player, IPlayerDiscovery discovery) {
      long gameTime = player.serverLevel().getGameTime();
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
