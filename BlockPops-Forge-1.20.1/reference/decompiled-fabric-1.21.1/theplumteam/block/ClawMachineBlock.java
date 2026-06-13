package com.theplumteam.block;

import com.mojang.serialization.MapCodec;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import com.theplumteam.data.IPlayerDiscovery;
import com.theplumteam.data.PlayerDataManager;
import com.theplumteam.network.OpenFavoriteColorScreenPacket;
import com.theplumteam.network.SyncTokenDataPacket;
import com.theplumteam.platform.PlatformHelper;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.server.ServerTickHandler;
import com.theplumteam.server.config.ServerConfig;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1750;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2237;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2383;
import net.minecraft.class_2464;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_2753;
import net.minecraft.class_2754;
import net.minecraft.class_2756;
import net.minecraft.class_2769;
import net.minecraft.class_3222;
import net.minecraft.class_3965;
import net.minecraft.class_5558;
import net.minecraft.class_2689.class_2690;
import net.minecraft.class_4970.class_2251;
import org.jetbrains.annotations.Nullable;

public class ClawMachineBlock extends class_2237 {
   public static final MapCodec<ClawMachineBlock> CODEC = method_54094(ClawMachineBlock::new);
   public static final class_2753 FACING = class_2383.field_11177;
   public static final class_2754<class_2756> HALF = class_2741.field_12533;

   public ClawMachineBlock(class_2251 properties) {
      super(properties);
      this.method_9590(
         (class_2680)((class_2680)((class_2680)this.field_10647.method_11664()).method_11657(FACING, class_2350.field_11043))
            .method_11657(HALF, class_2756.field_12607)
      );
   }

   protected MapCodec<? extends class_2237> method_53969() {
      return CODEC;
   }

   @Nullable
   public class_2586 method_10123(class_2338 pos, class_2680 state) {
      return state.method_11654(HALF) == class_2756.field_12607 ? new ClawMachineBlockEntity(pos, state) : null;
   }

   @Nullable
   public <T extends class_2586> class_5558<T> method_31645(class_1937 level, class_2680 state, class_2591<T> blockEntityType) {
      if (state.method_11654(HALF) == class_2756.field_12607) {
         return level.field_9236 ? method_31618(blockEntityType, (class_2591)ModBlockEntities.CLAW_MACHINE_BLOCK.get(), ClawMachineBlockEntity::tick) : null;
      } else {
         return null;
      }
   }

   public class_2464 method_9604(class_2680 state) {
      return class_2464.field_11456;
   }

   protected class_1269 method_55766(class_2680 state, class_1937 level, class_2338 pos, class_1657 player, class_3965 hit) {
      class_2338 lowerPos = state.method_11654(HALF) == class_2756.field_12607 ? pos : pos.method_10074();
      if (level.method_8321(lowerPos) instanceof ClawMachineBlockEntity clawMachineBlockEntity) {
         if (level.field_9236) {
            PlatformHelper.openClawMachineScreen(lowerPos, clawMachineBlockEntity);
         } else if (player instanceof class_3222 serverPlayer) {
            if (!ServerConfig.getInstance().isShowColorSelectionOnJoin()) {
               IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(serverPlayer);
               if (!discovery.hasChosenFavoriteColor()) {
                  OpenFavoriteColorScreenPacket.sendToPlayer(serverPlayer);
                  return class_1269.method_29236(level.field_9236);
               }
            }

            syncTokenDataToClient(serverPlayer);
         }

         return class_1269.method_29236(level.field_9236);
      } else {
         return class_1269.field_5811;
      }
   }

   private static void syncTokenDataToClient(class_3222 player) {
      IPlayerDiscovery discovery = PlayerDataManager.getDiscovery(player);
      long gameTime = player.method_51469().method_8510();
      long nextRegularTime = discovery.getNextRegularTokenTime();
      long ticksUntilNext = Math.max(0L, nextRegularTime - gameTime);
      long millisUntilReset = ServerTickHandler.calculateMillisUntilNextReset();
      SyncTokenDataPacket.sendToPlayer(player, discovery.getRegularTokens(), ticksUntilNext, !discovery.hasUsedTodaySpecialToken(), millisUntilReset);
   }

   @Nullable
   public class_2680 method_9605(class_1750 context) {
      class_2338 pos = context.method_8037();
      class_1937 level = context.method_8045();
      if (pos.method_10264() < level.method_31600() - 1 && level.method_8320(pos.method_10084()).method_26166(context)) {
         class_2350 playerFacing = context.method_8042();
         class_2350 blockFacing = playerFacing.method_10153();
         return (class_2680)((class_2680)this.method_9564().method_11657(FACING, blockFacing)).method_11657(HALF, class_2756.field_12607);
      } else {
         return null;
      }
   }

   public void method_9567(class_1937 level, class_2338 pos, class_2680 state, @Nullable class_1309 placer, class_1799 stack) {
      level.method_8652(pos.method_10084(), (class_2680)state.method_11657(HALF, class_2756.field_12609), 3);
   }

   public class_2680 method_9576(class_1937 level, class_2338 pos, class_2680 state, class_1657 player) {
      if (!level.field_9236) {
         if (player.method_7337()) {
            preventCreativeDropFromBottomPart(level, pos, state, player);
         } else if (player.method_7305(state)) {
            method_9577(level, pos, new class_1799(this.method_8389()));
         }
      }

      return super.method_9576(level, pos, state, player);
   }

   protected void method_9536(class_2680 state, class_1937 level, class_2338 pos, class_2680 newState, boolean isMoving) {
      if (!state.method_27852(newState.method_26204())) {
         class_2756 half = (class_2756)state.method_11654(HALF);
         class_2338 otherPos = half == class_2756.field_12607 ? pos.method_10084() : pos.method_10074();
         class_2680 otherState = level.method_8320(otherPos);
         if (otherState.method_27852(this) && otherState.method_11654(HALF) != half) {
            level.method_8652(otherPos, class_2246.field_10124.method_9564(), 35);
            level.method_8444(null, 2001, otherPos, class_2248.method_9507(otherState));
         }
      }

      super.method_9536(state, level, pos, newState, isMoving);
   }

   protected static void preventCreativeDropFromBottomPart(class_1937 level, class_2338 pos, class_2680 state, class_1657 player) {
      class_2756 half = (class_2756)state.method_11654(HALF);
      if (half == class_2756.field_12609) {
         class_2338 lowerPos = pos.method_10074();
         class_2680 lowerState = level.method_8320(lowerPos);
         if (lowerState.method_27852(state.method_26204()) && lowerState.method_11654(HALF) == class_2756.field_12607) {
            class_2680 airState = lowerState.method_28498(class_2741.field_12508) && lowerState.method_11654(class_2741.field_12508)
               ? class_2246.field_10382.method_9564()
               : class_2246.field_10124.method_9564();
            level.method_8652(lowerPos, airState, 35);
            level.method_8444(player, 2001, lowerPos, class_2248.method_9507(lowerState));
         }
      }
   }

   protected void method_9515(class_2690<class_2248, class_2680> builder) {
      builder.method_11667(new class_2769[]{FACING, HALF});
   }
}
