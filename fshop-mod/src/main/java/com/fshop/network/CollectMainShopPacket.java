package com.fshop.network;

import com.fshop.data.FShopSavedData;
import com.fshop.economy.CoinEconomy;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopNet;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** Admin collects the pending earnings of the main server shop ("La Moneda de Oro"). */
public final class CollectMainShopPacket {
   /** If true, the creator GUI is reopened afterwards so the shown earnings reset. */
   private final boolean reopen;

   public CollectMainShopPacket(boolean reopen) {
      this.reopen = reopen;
   }

   public static void encode(CollectMainShopPacket packet, FriendlyByteBuf buf) {
      buf.writeBoolean(packet.reopen);
   }

   public static CollectMainShopPacket decode(FriendlyByteBuf buf) {
      return new CollectMainShopPacket(buf.readBoolean());
   }

   public static void handle(CollectMainShopPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null || !sender.hasPermissions(2)) {
            return;
         }
         if (!CoinEconomy.available()) {
            sender.sendSystemMessage(Component.literal("[FShop] FantasticCoins no está instalado.")
                  .withStyle(ChatFormatting.RED));
            return;
         }
         FShopSavedData data = FShopSavedData.get(sender.serverLevel());
         PlayerShop main = data.getMainShop();
         if (main == null || main.totalPendingEarnings() <= 0L) {
            sender.sendSystemMessage(Component.literal("[FShop] La tienda principal no tiene ganancias pendientes.")
                  .withStyle(ChatFormatting.YELLOW));
            if (packet.reopen && main != null) {
               ShopNet.openCreator(sender, main);
            }
            return;
         }
         long b = main.getPendingEarnings(0);
         long p = main.getPendingEarnings(1);
         long o = main.getPendingEarnings(2);
         for (int c = 0; c < 3; c++) {
            CoinEconomy.deposit(sender, c, main.getPendingEarnings(c));
         }
         main.clearEarnings();
         data.setDirty();
         sender.sendSystemMessage(Component.literal("[FShop] Cobraste de La Moneda de Oro: "
               + o + " oro, " + p + " plata, " + b + " bronce.").withStyle(ChatFormatting.GREEN));
         if (packet.reopen) {
            ShopNet.openCreator(sender, main);
         }
      });
      context.setPacketHandled(true);
   }
}
