package com.fshop.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Maps {@link ShopService.Result} values to localized chat feedback. */
public final class ResultMessages {
   private ResultMessages() {
   }

   public static Component of(ShopService.Result result) {
      String key;
      ChatFormatting color = ChatFormatting.RED;
      switch (result) {
         case OK -> {
            key = "fshop.msg.ok";
            color = ChatFormatting.GREEN;
         }
         case NO_SHOP -> key = "fshop.msg.no_shop";
         case NO_OFFER -> key = "fshop.msg.no_offer";
         case NOT_OWNER -> key = "fshop.msg.not_owner";
         case OUT_OF_STOCK -> key = "fshop.msg.out_of_stock";
         case CANNOT_AFFORD -> key = "fshop.msg.cannot_afford";
         case INVENTORY_FULL -> key = "fshop.msg.inventory_full";
         case NO_CURRENCY -> key = "fshop.msg.no_currency";
         case LIMIT_REACHED -> key = "fshop.msg.limit_reached";
         case OWN_SHOP -> key = "fshop.msg.own_shop";
         default -> key = "fshop.msg.invalid";
      }
      return Component.literal("[FShop] ").withStyle(ChatFormatting.GOLD)
            .append(Component.translatable(key).withStyle(color));
   }
}
