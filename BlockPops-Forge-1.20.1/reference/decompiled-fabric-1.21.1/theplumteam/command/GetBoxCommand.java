package com.theplumteam.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import com.theplumteam.network.TokenType;
import com.theplumteam.network.UnlockFigurePacket;
import com.theplumteam.registry.ModItems;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_1542;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2172;
import net.minecraft.class_2487;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetBoxCommand {
   private static final Logger LOGGER = LoggerFactory.getLogger(GetBoxCommand.class);
   private static final SuggestionProvider<class_2168> COLLECTION_SUGGESTIONS = (context, builder) -> {
      Set<String> collectionIds = CollectionRegistry.getCollectionIds();
      return class_2172.method_9264(collectionIds.stream().filter(id -> !id.equals("default")), builder);
   };
   private static final SuggestionProvider<class_2168> TOKEN_TYPE_SUGGESTIONS = (context, builder) -> class_2172.method_9253(
         new String[]{"regular", "guaranteed"}, builder
      );

   public static void register(CommandDispatcher<class_2168> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)class_2170.method_9247("blockpops")
            .then(
               ((LiteralArgumentBuilder)class_2170.method_9247("getbox").requires(source -> source.method_9259(2)))
                  .then(
                     ((RequiredArgumentBuilder)class_2170.method_9244("collection_id", StringArgumentType.string())
                           .suggests(COLLECTION_SUGGESTIONS)
                           .executes(context -> executeCommand(context, TokenType.REGULAR)))
                        .then(class_2170.method_9244("token_type", StringArgumentType.string()).suggests(TOKEN_TYPE_SUGGESTIONS).executes(context -> {
                           String tokenTypeStr = StringArgumentType.getString(context, "token_type");
                           TokenType tokenType = parseTokenType(tokenTypeStr);
                           if (tokenType == null) {
                              ((class_2168)context.getSource()).method_9213(class_2561.method_43470("Invalid token type. Use 'regular' or 'guaranteed'"));
                              return 0;
                           } else {
                              return executeCommand(context, tokenType);
                           }
                        }))
                  )
            )
      );
   }

   private static TokenType parseTokenType(String tokenTypeStr) {
      if (tokenTypeStr.equalsIgnoreCase("regular")) {
         return TokenType.REGULAR;
      } else {
         return tokenTypeStr.equalsIgnoreCase("guaranteed") ? TokenType.GUARANTEED : null;
      }
   }

   private static int executeCommand(CommandContext<class_2168> context, TokenType tokenType) {
      String collectionId = StringArgumentType.getString(context, "collection_id");
      class_2168 source = (class_2168)context.getSource();

      try {
         class_3222 player = source.method_9207();
         if (!CollectionRegistry.getCollection(collectionId).isPresent()) {
            source.method_9213(class_2561.method_43470("Collection '" + collectionId + "' does not exist"));
            return 0;
         } else {
            IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
            processBoxDrop(player, collectionId, tokenType, discovery);
            PlayerDataManager.saveDiscovery(player);
            source.method_9226(
               () -> class_2561.method_43470("Gave box from collection '" + collectionId + "' using " + tokenType.name().toLowerCase() + " token logic"), true
            );
            return 1;
         }
      } catch (Exception var6) {
         source.method_9213(class_2561.method_43470("This command can only be executed by a player"));
         LOGGER.error("Error executing getbox command", var6);
         return 0;
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

   private static void processBoxDrop(class_3222 player, String collectionId, TokenType tokenType, IPlayerDiscovery discovery) {
      CollectionRegistry.getCollection(collectionId)
         .ifPresent(
            collection -> {
               List<FigureDefinition> figures = collection.getFigures();
               if (!figures.isEmpty()) {
                  FigureDefinition selectedFigure = selectFigure(figures, tokenType, discovery, collectionId);
                  class_1799 boxItem = null;
                  if (collectionId.equals("world_players")) {
                     PopBlockColor color = selectedFigure.getFavoriteColor();
                     if (color == null) {
                        color = PopBlockColor.ORIGINAL;
                     }

                     boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color).get());
                  } else if (ModItems.BOX_BLOCK_ITEMS.containsKey(collectionId)) {
                     boxItem = new class_1799((class_1935)ModItems.BOX_BLOCK_ITEMS.get(collectionId).get());
                  } else {
                     boxItem = new class_1799((class_1935)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(PopBlockColor.ORIGINAL).get());
                  }

                  if (boxItem != null) {
                     String uniqueFigureId = collectionId + ":" + selectedFigure.getId();
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
                     }

                     class_2487 blockEntityTag = new class_2487();
                     blockEntityTag.method_10582("FigureId", selectedFigure.getId());
                     blockEntityTag.method_10582("CollectionId", collectionId);
                     if (collectionId.equals("world_players")) {
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
                     }

                     blockEntityTag.method_10582("id", "blockpops:box_block");
                     boxItem.method_57379(class_9334.field_49611, class_9279.method_57456(blockEntityTag));
                     class_1542 itemEntity = new class_1542(
                        player.method_37908(), player.method_23317(), player.method_23318() + 1.0, player.method_23321(), boxItem
                     );
                     itemEntity.method_18800(0.0, 0.2, 0.0);
                     player.method_37908().method_8649(itemEntity);
                  }
               }
            }
         );
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

         return !undiscoveredFigures.isEmpty()
            ? undiscoveredFigures.get(random.nextInt(undiscoveredFigures.size()))
            : figures.get(random.nextInt(figures.size()));
      } else {
         return figures.get(random.nextInt(figures.size()));
      }
   }
}
