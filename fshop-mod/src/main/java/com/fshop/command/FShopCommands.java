package com.fshop.command;

import com.fshop.config.FShopConfig;
import com.fshop.data.FShopSavedData;
import com.fshop.economy.CoinEconomy;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopNet;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class FShopCommands {
   private FShopCommands() {
   }

   static int createShop(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      if (FShopConfig.REQUIRE_ZONE_FOR_CREATE.get()
            && !FShopSavedData.get(player.serverLevel()).isInsideAnyZone(player)) {
         msg(player, "Debes estar dentro de una zona de mercado para crear tu tienda.", ChatFormatting.RED);
         return 0;
      }
      String name = StringArgumentType.getString(ctx, "name").trim();
      if (name.isEmpty() || name.length() > 32) {
         msg(player, "El nombre debe tener entre 1 y 32 caracteres.", ChatFormatting.RED);
         return 0;
      }
      FShopSavedData data = FShopSavedData.get(player.serverLevel());
      List<PlayerShop> owned = data.getShopsByOwner(player.getUUID());
      if (owned.size() >= FShopConfig.MAX_SHOPS_PER_PLAYER.get()) {
         msg(player, "Ya alcanzaste el maximo de tiendas permitidas (" + FShopConfig.MAX_SHOPS_PER_PLAYER.get() + ").",
               ChatFormatting.RED);
         return 0;
      }
      PlayerShop shop = new PlayerShop(UUID.randomUUID(), player.getUUID(),
            player.getGameProfile().getName(), name);
      data.putShop(shop);
      msg(player, "Tienda \"" + name + "\" creada. Anade items con el boton de venta.", ChatFormatting.GREEN);
      ShopNet.openManage(player, shop);
      return 1;
   }

   static int buy(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      if (FShopConfig.REQUIRE_ZONE_FOR_BUY.get()
            && !FShopSavedData.get(player.serverLevel()).isInsideAnyZone(player)) {
         msg(player, "Debes estar dentro de una zona de mercado para comprar.", ChatFormatting.RED);
         return 0;
      }
      ShopNet.openBrowse(player);
      return 1;
   }

   static int sell(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      if (FShopConfig.REQUIRE_ZONE_FOR_SELL.get()
            && !FShopSavedData.get(player.serverLevel()).isInsideAnyZone(player)) {
         msg(player, "Debes estar dentro de una zona de mercado para gestionar tu tienda.", ChatFormatting.RED);
         return 0;
      }
      List<PlayerShop> owned = FShopSavedData.get(player.serverLevel()).getShopsByOwner(player.getUUID());
      if (owned.isEmpty()) {
         msg(player, "No tienes ninguna tienda. Usa /fshop create <nombre> primero.", ChatFormatting.YELLOW);
         return 0;
      }
      ShopNet.openManage(player, owned.get(0));
      return 1;
   }

   static int collect(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      if (!CoinEconomy.available()) {
         msg(player, "La moneda del servidor (FantasticCoins) no esta instalada.", ChatFormatting.RED);
         return 0;
      }
      FShopSavedData data = FShopSavedData.get(player.serverLevel());
      long total = 0L;
      for (PlayerShop shop : data.getShopsByOwner(player.getUUID())) {
         total += shop.getPendingEarnings();
         shop.clearEarnings();
      }
      if (total <= 0L) {
         msg(player, "No tienes ganancias pendientes por cobrar.", ChatFormatting.YELLOW);
         return 0;
      }
      CoinEconomy.deposit(player, total);
      data.setDirty();
      msg(player, "Cobraste " + CoinEconomy.format(total) + ".", ChatFormatting.GREEN);
      return 1;
   }

   static int balance(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      long bal = CoinEconomy.balance(player);
      msg(player, "Tu saldo: " + CoinEconomy.format(bal) + " (" + bal + " en bronce).", ChatFormatting.GOLD);
      return 1;
   }

   static int help(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack src = ctx.getSource();
      line(src, "===== FShop =====", ChatFormatting.GOLD);
      line(src, "/fshop create <nombre> - crea y abre tu tienda", ChatFormatting.YELLOW);
      line(src, "/fshop buy - explora las tiendas del servidor", ChatFormatting.YELLOW);
      line(src, "/fshop sell - gestiona tu tienda y su stock", ChatFormatting.YELLOW);
      line(src, "/fshop collect - cobra tus ganancias", ChatFormatting.YELLOW);
      line(src, "/fshop balance - muestra tu saldo en monedas", ChatFormatting.YELLOW);
      if (src.hasPermission(2)) {
         line(src, "/fshop admin ... - herramientas de administracion", ChatFormatting.AQUA);
      }
      return 1;
   }

   // Helpers ---------------------------------------------------------------
   static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
      try {
         return ctx.getSource().getPlayerOrException();
      } catch (Exception e) {
         ctx.getSource().sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
         return null;
      }
   }

   static void msg(ServerPlayer player, String text, ChatFormatting color) {
      player.sendSystemMessage(Component.literal("[FShop] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(text).withStyle(color)));
   }

   static void line(CommandSourceStack src, String text, ChatFormatting color) {
      src.sendSystemMessage(Component.literal(text).withStyle(color));
   }
}
