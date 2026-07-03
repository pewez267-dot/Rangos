package com.fshop.events;

import com.fshop.FShop;
import com.fshop.item.MarketWandItem;
import com.fshop.zone.PlayerSelection;
import com.fshop.zone.SelectionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Handles the market wand's left-click (set corner 1) and cleanup on logout. */
@Mod.EventBusSubscriber(modid = FShop.MOD_ID)
public final class WandEvents {

   @SubscribeEvent
   public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
      ItemStack held = event.getItemStack();
      if (held.getItem() instanceof MarketWandItem) {
         // Prevent the wand from actually breaking blocks (e.g. in creative).
         event.setCanceled(true);
         if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer sp) {
            PlayerSelection sel = SelectionManager.get(sp);
            sel.setPos1(sp.level().dimension(), event.getPos());
            sp.sendSystemMessage(Component.literal("[FShop] ")
                  .withStyle(ChatFormatting.GOLD)
                  .append(Component.literal("Esquina 1 fijada en " + MarketWandItem.fmt(event.getPos()))
                        .withStyle(ChatFormatting.YELLOW))
                  .append(sel.describeVolume()));
         }
      }
   }

   @SubscribeEvent
   public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp) {
         SelectionManager.clear(sp);
      }
   }
}
