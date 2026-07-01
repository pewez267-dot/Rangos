package com.fantasticpass.events;

import com.fantasticpass.afk.AfkTracker;
import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.commands.FsPassCommand;
import com.fantasticpass.data.DefaultPass;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.nametag.NametagData;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.network.NametagUpdatePacket;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.progression.TierProgressionManager;
import com.fantasticpass.quest.QuestManager;
import com.fantasticpass.quest.QuestType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class ServerEvents {
   private static final AfkTracker AFK = new AfkTracker();
   private static final TierProgressionManager PROGRESSION = new TierProgressionManager(AFK);

   @SubscribeEvent
   public void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) {
            AFK.serverTick(server);
            PROGRESSION.serverTick(server);
         }
      }
   }

   @SubscribeEvent
   public void onRegisterCommands(RegisterCommandsEvent event) {
      FsPassCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public void onRightClickBlock(RightClickBlock event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onRightClickItem(RightClickItem event) {
      this.mark(event.getEntity());
      if (event.getEntity() instanceof ServerPlayer serverPlayer && !event.getItemStack().isEmpty()) {
         this.questParam(serverPlayer, QuestType.USE_ITEM,
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()), 1);
      }
   }

   @SubscribeEvent
   public void onLeftClickBlock(LeftClickBlock event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onEntityInteract(EntityInteract event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onAttackEntity(AttackEntityEvent event) {
      this.mark(event.getEntity());
   }

   @SubscribeEvent
   public void onBlockBreak(BreakEvent event) {
      this.mark(event.getPlayer());
      if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
         BlockState state = event.getState();
         this.quest(serverPlayer, QuestType.BREAK_BLOCKS, 1);
         // Generic per-block objective (any vanilla or modded block).
         this.questParam(serverPlayer, QuestType.MINE_BLOCK, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), 1);

         if (state.is(Tags.Blocks.ORES)) {
            this.quest(serverPlayer, QuestType.MINE_ORES, 1);
            if (state.is(Tags.Blocks.ORES_COAL)) {
               this.quest(serverPlayer, QuestType.MINE_COAL, 1);
            } else if (state.is(Tags.Blocks.ORES_IRON)) {
               this.quest(serverPlayer, QuestType.MINE_IRON, 1);
            } else if (state.is(Tags.Blocks.ORES_GOLD)) {
               this.quest(serverPlayer, QuestType.MINE_GOLD, 1);
            } else if (state.is(Tags.Blocks.ORES_DIAMOND)) {
               this.quest(serverPlayer, QuestType.MINE_DIAMOND, 1);
            } else if (state.is(Tags.Blocks.ORES_REDSTONE)) {
               this.quest(serverPlayer, QuestType.MINE_REDSTONE, 1);
            } else if (state.is(Tags.Blocks.ORES_LAPIS)) {
               this.quest(serverPlayer, QuestType.MINE_LAPIS, 1);
            } else if (state.is(Tags.Blocks.ORES_EMERALD)) {
               this.quest(serverPlayer, QuestType.MINE_EMERALD, 1);
            } else if (state.is(Blocks.ANCIENT_DEBRIS)) {
               this.quest(serverPlayer, QuestType.MINE_NETHERITE, 1);
            } else if (state.is(Blocks.NETHER_QUARTZ_ORE)) {
               this.quest(serverPlayer, QuestType.MINE_QUARTZ, 1);
            } else if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
               this.quest(serverPlayer, QuestType.MINE_COPPER, 1);
            }
         } else if (state.is(BlockTags.LOGS)) {
            this.quest(serverPlayer, QuestType.CHOP_WOOD, 1);
         } else if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            this.quest(serverPlayer, QuestType.HARVEST_CROPS, 1);
         } else if (state.is(Tags.Blocks.STONE) || state.is(Tags.Blocks.COBBLESTONE)) {
            this.quest(serverPlayer, QuestType.MINE_STONE, 1);
         }
      }
   }

   @SubscribeEvent
   public void onBlockPlace(EntityPlaceEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         this.mark(serverPlayer);
         this.quest(serverPlayer, QuestType.PLACE_BLOCKS, 1);
         this.questParam(serverPlayer, QuestType.PLACE_BLOCK,
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()), 1);
      }
   }

   @SubscribeEvent
   public void onLivingDeath(LivingDeathEvent event) {
      if (event.getSource().getEntity() instanceof ServerPlayer killer) {
         LivingEntity dead = event.getEntity();
         if (dead instanceof Player) {
            return; // PvP is intentionally not tracked.
         }

         if (dead instanceof Monster) {
            this.quest(killer, QuestType.KILL_MONSTERS, 1);
            if (dead instanceof Zombie) {
               this.quest(killer, QuestType.KILL_ZOMBIES, 1);
            } else if (dead instanceof AbstractSkeleton) {
               this.quest(killer, QuestType.KILL_SKELETONS, 1);
            } else if (dead instanceof Creeper) {
               this.quest(killer, QuestType.KILL_CREEPERS, 1);
            } else if (dead instanceof Spider) {
               this.quest(killer, QuestType.KILL_SPIDERS, 1);
            } else if (dead instanceof EnderMan) {
               this.quest(killer, QuestType.KILL_ENDERMEN, 1);
            }
         } else if (dead instanceof Animal) {
            this.quest(killer, QuestType.KILL_ANIMALS, 1);
         }

         // Specific mob objectives (work whether or not the mob is a Monster subclass).
         EntityType<?> et = dead.getType();
         if (et == EntityType.BLAZE) {
            this.quest(killer, QuestType.KILL_BLAZE, 1);
         } else if (et == EntityType.WITHER_SKELETON) {
            this.quest(killer, QuestType.KILL_WITHER_SKELETONS, 1);
         } else if (et == EntityType.PIGLIN || et == EntityType.PIGLIN_BRUTE) {
            this.quest(killer, QuestType.KILL_PIGLINS, 1);
         } else if (et == EntityType.SLIME) {
            this.quest(killer, QuestType.KILL_SLIMES, 1);
         } else if (et == EntityType.MAGMA_CUBE) {
            this.quest(killer, QuestType.KILL_MAGMA_CUBES, 1);
         } else if (et == EntityType.GUARDIAN || et == EntityType.ELDER_GUARDIAN) {
            this.quest(killer, QuestType.KILL_GUARDIANS, 1);
         } else if (et == EntityType.PHANTOM) {
            this.quest(killer, QuestType.KILL_PHANTOMS, 1);
         } else if (et == EntityType.DROWNED) {
            this.quest(killer, QuestType.KILL_DROWNED, 1);
         } else if (et == EntityType.WITCH) {
            this.quest(killer, QuestType.KILL_WITCHES, 1);
         } else if (et == EntityType.PILLAGER) {
            this.quest(killer, QuestType.KILL_PILLAGERS, 1);
         } else if (et == EntityType.GHAST) {
            this.quest(killer, QuestType.KILL_GHASTS, 1);
         } else if (et == EntityType.HOGLIN || et == EntityType.ZOGLIN) {
            this.quest(killer, QuestType.KILL_HOGLINS, 1);
         } else if (et == EntityType.VINDICATOR) {
            this.quest(killer, QuestType.KILL_VINDICATORS, 1);
         } else if (et == EntityType.RAVAGER) {
            this.quest(killer, QuestType.KILL_RAVAGERS, 1);
         } else if (et == EntityType.EVOKER) {
            this.quest(killer, QuestType.KILL_EVOKERS, 1);
         } else if (et == EntityType.VEX) {
            this.quest(killer, QuestType.KILL_VEXES, 1);
         } else if (et == EntityType.ILLUSIONER) {
            this.quest(killer, QuestType.KILL_ILLUSIONERS, 1);
         } else if (et == EntityType.SHULKER) {
            this.quest(killer, QuestType.KILL_SHULKERS, 1);
         } else if (et == EntityType.WARDEN) {
            this.quest(killer, QuestType.KILL_WARDENS, 1);
         } else if (et == EntityType.SILVERFISH) {
            this.quest(killer, QuestType.KILL_SILVERFISH, 1);
         } else if (et == EntityType.ENDERMITE) {
            this.quest(killer, QuestType.KILL_ENDERMITES, 1);
         } else if (et == EntityType.STRAY) {
            this.quest(killer, QuestType.KILL_STRAYS, 1);
         } else if (et == EntityType.HUSK) {
            this.quest(killer, QuestType.KILL_HUSKS, 1);
         } else if (et == EntityType.ZOMBIE_VILLAGER) {
            this.quest(killer, QuestType.KILL_ZOMBIE_VILLAGERS, 1);
         } else if (et == EntityType.CAVE_SPIDER) {
            this.quest(killer, QuestType.KILL_CAVE_SPIDERS, 1);
         } else if (et == EntityType.ZOMBIFIED_PIGLIN) {
            this.quest(killer, QuestType.KILL_ZOMBIFIED_PIGLINS, 1);
         } else if (et == EntityType.WITHER) {
            this.quest(killer, QuestType.KILL_WITHER, 1);
            this.quest(killer, QuestType.KILL_BOSSES, 1);
         } else if (et == EntityType.ENDER_DRAGON) {
            this.quest(killer, QuestType.KILL_ENDER_DRAGON, 1);
            this.quest(killer, QuestType.KILL_BOSSES, 1);
         }

         // Generic per-entity objective (any vanilla or modded mob).
         net.minecraft.resources.ResourceLocation entId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(et);
         this.questParam(killer, QuestType.KILL_ENTITY, entId, 1);
      }
   }

   @SubscribeEvent
   public void onItemFished(ItemFishedEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         this.quest(serverPlayer, QuestType.CATCH_FISH, 1);
      }
   }

   @SubscribeEvent
   public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getItem().isEdible()) {
         this.quest(serverPlayer, QuestType.EAT_FOOD, 1);
         this.questParam(serverPlayer, QuestType.EAT_ITEM,
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItem().getItem()), 1);
      }
   }

   @SubscribeEvent
   public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer && !event.getCrafting().isEmpty()) {
         this.quest(serverPlayer, QuestType.CRAFT_ITEMS, event.getCrafting().getCount());
         this.questParam(serverPlayer, QuestType.CRAFT_ITEM,
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()), event.getCrafting().getCount());
      }
   }

   @SubscribeEvent
   public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer && !event.getSmelting().isEmpty()) {
         this.quest(serverPlayer, QuestType.SMELT_ITEMS, event.getSmelting().getCount());
         this.questParam(serverPlayer, QuestType.SMELT_ITEM,
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getSmelting().getItem()), event.getSmelting().getCount());
      }
   }

   @SubscribeEvent
   public void onBabySpawn(BabyEntitySpawnEvent event) {
      if (event.getCausedByPlayer() instanceof ServerPlayer serverPlayer) {
         this.quest(serverPlayer, QuestType.BREED_ANIMALS, 1);
         if (event.getParentA() != null) {
            this.questParam(serverPlayer, QuestType.BREED_ENTITY,
               net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getParentA().getType()), 1);
         }
      }
   }

   @SubscribeEvent
   public void onAnimalTame(AnimalTameEvent event) {
      if (event.getTamer() instanceof ServerPlayer serverPlayer) {
         this.quest(serverPlayer, QuestType.TAME_ANIMALS, 1);
         if (event.getAnimal() != null) {
            this.questParam(serverPlayer, QuestType.TAME_ENTITY,
               net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getAnimal().getType()), 1);
         }
      }
   }

   @SubscribeEvent
   public void onLivingHurt(LivingHurtEvent event) {
      // Damage DEALT by a player to a non-player living entity.
      if (event.getSource().getEntity() instanceof ServerPlayer attacker && !(event.getEntity() instanceof Player)) {
         int amount = Math.max(1, (int)Math.ceil(event.getAmount()));
         this.quest(attacker, QuestType.DEAL_DAMAGE, amount);
      }
   }

   @SubscribeEvent
   public void onXpChange(PlayerXpEvent.XpChange event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getAmount() > 0) {
         this.quest(serverPlayer, QuestType.GAIN_XP, event.getAmount());
      }
   }

   @SubscribeEvent
   public void onItemPickup(EntityItemPickupEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getItem() != null) {
         net.minecraft.world.item.ItemStack stack = event.getItem().getItem();
         if (!stack.isEmpty()) {
            this.questParam(serverPlayer, QuestType.PICKUP_ITEM,
               net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount());
         }
      }
   }

   private void quest(ServerPlayer player, QuestType type, int amount) {
      PlayerPassData data = PassCapability.getData(player);
      if (data != null && QuestManager.track(player, data, type, amount)) {
         NametagSync.syncPlayer(player);
      }
   }

   /** Track a parameterized objective (entity/block/item id) for mod compatibility. */
   private void questParam(ServerPlayer player, QuestType type, net.minecraft.resources.ResourceLocation id, int amount) {
      if (id == null) {
         return;
      }
      PlayerPassData data = PassCapability.getData(player);
      if (data != null && QuestManager.track(player, data, type, id.toString(), amount)) {
         NametagSync.syncPlayer(player);
      }
   }

   @SubscribeEvent
   public void onChat(ServerChatEvent event) {
      AFK.registerInteraction(event.getPlayer());
   }

   @SubscribeEvent
   public void onCommand(CommandEvent event) {
      CommandSourceStack source = (CommandSourceStack)event.getParseResults().getContext().getSource();
      if (source.getEntity() instanceof ServerPlayer serverPlayer) {
         AFK.registerInteraction(serverPlayer);
      }
   }

   private void mark(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         AFK.registerInteraction(serverPlayer);
      }
   }

   @SubscribeEvent
   public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer joining) {
         MinecraftServer server = joining.getServer();
         if (server != null) {
            PlayerPassData data = PassCapability.getData(joining);
            if (data != null) {
               QuestManager.ensureDaily(joining.getUUID(), data);
            }

            NametagSync.syncPlayer(joining);

            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
               NametagData ntData = NametagSync.compute(other);
               PacketHandler.sendToPlayer(joining, new NametagUpdatePacket(other.getUUID(), ntData));
            }
         }
      }
   }

   @SubscribeEvent
   public void onStartTracking(StartTracking event) {
      if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
         PacketHandler.sendToPlayer(viewer, new NametagUpdatePacket(target.getUUID(), NametagSync.compute(target)));
      }
   }

   @SubscribeEvent
   public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         AFK.remove(serverPlayer.getUUID());
         // Belt-and-suspenders: drop any test overlay so nothing test-related persists.
         PlayerPassData data = PassCapability.getData(serverPlayer);
         if (data != null && data.isTestMode()) {
            data.exitTestMode();
         }
      }
   }

   @SubscribeEvent
   public void onServerStarted(ServerStartedEvent event) {
      MinecraftServer server = event.getServer();
      if (server != null) {
         PassSavedData saved = PassSavedData.get(server);
         if (saved.getPasses().isEmpty()) {
            PassDefinition def = DefaultPass.build();
            saved.putPass(def);
            saved.setActivePassId(def.getId());
         } else if (saved.getActivePass() == null && !saved.getPasses().isEmpty()) {
            saved.setActivePassId(saved.getPasses().keySet().iterator().next());
         }
      }
   }

   @SubscribeEvent
   public void onServerStopping(ServerStoppingEvent event) {
      AFK.clear();
   }
}
