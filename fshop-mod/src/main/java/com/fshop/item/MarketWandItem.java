package com.fshop.item;

import com.fshop.zone.PlayerSelection;
import com.fshop.zone.SelectionManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * WorldEdit-style selection wand. Right-click a block to set the second corner
 * (pos2). The first corner (pos1) is set with a left-click, handled in
 * {@link com.fshop.events.WandEvents}. The selection is stored per-player and
 * later turned into a market zone by an administrator.
 */
public final class MarketWandItem extends Item {
   public MarketWandItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      if (!level.isClientSide && ctx.getPlayer() instanceof ServerPlayer sp) {
         PlayerSelection sel = SelectionManager.get(sp);
         sel.setPos2(sp.level().dimension(), pos);
         sp.sendSystemMessage(Component.literal("[FShop] ")
               .withStyle(ChatFormatting.GOLD)
               .append(Component.literal("Esquina 2 fijada en " + fmt(pos)).withStyle(ChatFormatting.YELLOW))
               .append(sel.describeVolume()));
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return true;
   }

   @Override
   public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
      tip.add(Component.literal("Selector de zona de mercado").withStyle(ChatFormatting.GRAY));
      tip.add(Component.literal("Click izquierdo: esquina 1").withStyle(ChatFormatting.GREEN));
      tip.add(Component.literal("Click derecho: esquina 2").withStyle(ChatFormatting.GREEN));
      tip.add(Component.literal("Luego: /fshop admin zone create <nombre>").withStyle(ChatFormatting.DARK_GRAY));
   }

   public static String fmt(BlockPos p) {
      return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
   }
}
