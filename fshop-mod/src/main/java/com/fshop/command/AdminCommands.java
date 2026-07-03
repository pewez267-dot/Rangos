package com.fshop.command;

import com.fshop.data.FShopSavedData;
import com.fshop.economy.CoinEconomy;
import com.fshop.registry.ModItems;
import com.fshop.zone.MarketZone;
import com.fshop.zone.PlayerSelection;
import com.fshop.zone.SelectionManager;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class AdminCommands {
   private AdminCommands() {
   }

   static int giveWand(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = FShopCommands.playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      ItemStack wand = new ItemStack(ModItems.MARKET_WAND.get());
      if (!player.getInventory().add(wand)) {
         player.drop(wand, false);
      }
      FShopCommands.msg(player, "Recibiste el selector de zona de mercado.", ChatFormatting.GREEN);
      return 1;
   }

   static int reload(CommandContext<CommandSourceStack> ctx) {
      FShopCommands.line(ctx.getSource(),
            "[FShop] La configuracion se recarga automaticamente al editar el archivo fshop-common.toml.",
            ChatFormatting.AQUA);
      return 1;
   }

   static int zoneCreate(CommandContext<CommandSourceStack> ctx) {
      ServerPlayer player = FShopCommands.playerOrNull(ctx);
      if (player == null) {
         return 0;
      }
      PlayerSelection sel = SelectionManager.get(player);
      if (!sel.isComplete()) {
         FShopCommands.msg(player, "Seleccion incompleta. Usa el selector: click izq (esq 1) y click der (esq 2).",
               ChatFormatting.RED);
         return 0;
      }
      String name = StringArgumentType.getString(ctx, "name");
      FShopSavedData data = FShopSavedData.get(player.serverLevel());
      if (data.getZones().containsKey(name.toLowerCase())) {
         FShopCommands.msg(player, "Ya existe una zona con ese nombre.", ChatFormatting.RED);
         return 0;
      }
      BlockPos min = sel.min();
      BlockPos max = sel.max();
      MarketZone zone = new MarketZone(name, sel.getDimension(), min, max);
      data.putZone(zone);
      FShopCommands.msg(player, "Zona de mercado \"" + name + "\" creada (" + zone.volume() + " bloques).",
            ChatFormatting.GREEN);
      return 1;
   }

   static int zoneRemove(CommandContext<CommandSourceStack> ctx) {
      String name = StringArgumentType.getString(ctx, "name");
      FShopSavedData data = FShopSavedData.get(ctx.getSource().getLevel().getServer());
      boolean removed = data.removeZone(name);
      FShopCommands.line(ctx.getSource(),
            removed ? "[FShop] Zona \"" + name + "\" eliminada." : "[FShop] No existe esa zona.",
            removed ? ChatFormatting.GREEN : ChatFormatting.RED);
      return removed ? 1 : 0;
   }

   static int zoneList(CommandContext<CommandSourceStack> ctx) {
      FShopSavedData data = FShopSavedData.get(ctx.getSource().getLevel().getServer());
      if (data.getZones().isEmpty()) {
         FShopCommands.line(ctx.getSource(), "[FShop] No hay zonas de mercado definidas.", ChatFormatting.YELLOW);
         return 1;
      }
      FShopCommands.line(ctx.getSource(), "===== Zonas de mercado =====", ChatFormatting.GOLD);
      for (MarketZone z : data.getZones().values()) {
         FShopCommands.line(ctx.getSource(),
               "- " + z.getName() + " @ " + z.getDimension().location() + " " + fmt(z.getMin()) + " -> " + fmt(z.getMax()),
               ChatFormatting.YELLOW);
      }
      return 1;
   }

   static int shopList(CommandContext<CommandSourceStack> ctx) {
      FShopSavedData data = FShopSavedData.get(ctx.getSource().getLevel().getServer());
      if (data.getShops().isEmpty()) {
         FShopCommands.line(ctx.getSource(), "[FShop] No hay tiendas registradas.", ChatFormatting.YELLOW);
         return 1;
      }
      FShopCommands.line(ctx.getSource(), "===== Tiendas (" + data.getShops().size() + ") =====", ChatFormatting.GOLD);
      data.getShops().values().forEach(shop -> FShopCommands.line(ctx.getSource(),
            "- \"" + shop.getName() + "\" de " + shop.getOwnerName() + " (" + shop.getOffers().size()
                  + " ofertas, ganancias: " + CoinEconomy.format(shop.getPendingEarnings()) + ")",
            ChatFormatting.YELLOW));
      return 1;
   }

   static int shopRemoveAll(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
      ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
      FShopSavedData data = FShopSavedData.get(ctx.getSource().getLevel().getServer());
      int removed = data.removeShopsByOwner(target.getUUID());
      FShopCommands.line(ctx.getSource(),
            "[FShop] Se eliminaron " + removed + " tienda(s) de " + target.getGameProfile().getName() + ".",
            ChatFormatting.GREEN);
      return 1;
   }

   static int coins(CommandContext<CommandSourceStack> ctx, boolean give)
         throws com.mojang.brigadier.exceptions.CommandSyntaxException {
      ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
      long amount = LongArgumentType.getLong(ctx, "amount");
      if (!CoinEconomy.available()) {
         FShopCommands.line(ctx.getSource(), "[FShop] FantasticCoins no esta instalado.", ChatFormatting.RED);
         return 0;
      }
      if (give) {
         CoinEconomy.deposit(target, amount);
         FShopCommands.line(ctx.getSource(),
               "[FShop] Diste " + CoinEconomy.format(amount) + " a " + target.getGameProfile().getName() + ".",
               ChatFormatting.GREEN);
         target.sendSystemMessage(Component.literal("[FShop] Recibiste " + CoinEconomy.format(amount) + ".")
               .withStyle(ChatFormatting.GREEN));
      } else {
         boolean ok = CoinEconomy.withdraw(target, amount);
         FShopCommands.line(ctx.getSource(), ok
               ? "[FShop] Quitaste " + CoinEconomy.format(amount) + " a " + target.getGameProfile().getName() + "."
               : "[FShop] " + target.getGameProfile().getName() + " no tiene suficientes monedas.",
               ok ? ChatFormatting.GREEN : ChatFormatting.RED);
      }
      return 1;
   }

   private static String fmt(BlockPos p) {
      return "(" + p.getX() + "," + p.getY() + "," + p.getZ() + ")";
   }
}
