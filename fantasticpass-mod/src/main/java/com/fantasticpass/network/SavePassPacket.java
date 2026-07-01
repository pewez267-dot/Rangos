package com.fantasticpass.network;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class SavePassPacket {
   private final PassDefinition pass;

   public SavePassPacket(PassDefinition pass) {
      this.pass = pass;
   }

   public static void encode(SavePassPacket packet, FriendlyByteBuf buf) {
      packet.pass.toBuf(buf);
   }

   public static SavePassPacket decode(FriendlyByteBuf buf) {
      return new SavePassPacket(PassDefinition.fromBuf(buf));
   }

   public static void handle(SavePassPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer sender = context.getSender();
         if (sender == null || !sender.hasPermissions(4)) {
            return;
         }
         String id = packet.pass.getId();
         if (id == null || id.isEmpty()) {
            sender.sendSystemMessage(Component.translatable("fantasticpass.msg.pass_id_required")
               .withStyle(net.minecraft.ChatFormatting.RED));
            return;
         }
         MinecraftServer server = sender.getServer();
         if (server == null) {
            return;
         }
         PassSavedData saved = PassSavedData.get(server);
         saved.putPass(packet.pass);
         sender.sendSystemMessage(Component.translatable("fantasticpass.msg.pass_saved", new Object[]{id})
            .withStyle(net.minecraft.ChatFormatting.GREEN));

         // A pass that was just created/edited only affects players once it is
         // the ACTIVE pass. If nothing is active yet, auto-activate this one so
         // rewards (and their NBT) apply immediately with no extra step. If a
         // DIFFERENT pass is already active, remind the admin how to switch.
         String activeId = saved.getActivePassId();
         if (activeId == null || activeId.isEmpty()) {
            saved.setActivePassId(id);
            sender.sendSystemMessage(Component.translatable("fantasticpass.msg.pass_activated", new Object[]{id})
               .withStyle(net.minecraft.ChatFormatting.GOLD));
         } else if (!activeId.equals(id)) {
            sender.sendSystemMessage(Component.translatable("fantasticpass.msg.pass_saved_not_active", id)
               .withStyle(net.minecraft.ChatFormatting.YELLOW));
         }
      });
      context.setPacketHandled(true);
   }
}
