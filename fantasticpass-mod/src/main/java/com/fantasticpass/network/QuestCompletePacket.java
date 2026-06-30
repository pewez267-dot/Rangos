package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.quest.QuestType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Sent server -> client when a quest is completed so the client can show a
 * clean, compact "quest complete" toast (instead of a chat line). Carries the
 * quest type + target (to build the localized description on the client) and
 * the points awarded.
 */
public final class QuestCompletePacket {
   private final String questType;
   private final int target;
   private final int points;
   private final boolean premium;

   public QuestCompletePacket(QuestType type, int target, int points, boolean premium) {
      this.questType = type.name();
      this.target = target;
      this.points = points;
      this.premium = premium;
   }

   private QuestCompletePacket(String questType, int target, int points, boolean premium) {
      this.questType = questType;
      this.target = target;
      this.points = points;
      this.premium = premium;
   }

   public QuestType getType() {
      return QuestType.byName(this.questType);
   }

   public int getTarget() {
      return this.target;
   }

   public int getPoints() {
      return this.points;
   }

   public boolean isPremium() {
      return this.premium;
   }

   public static void encode(QuestCompletePacket packet, FriendlyByteBuf buf) {
      buf.writeUtf(packet.questType);
      buf.writeVarInt(packet.target);
      buf.writeVarInt(packet.points);
      buf.writeBoolean(packet.premium);
   }

   public static QuestCompletePacket decode(FriendlyByteBuf buf) {
      return new QuestCompletePacket(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
   }

   public static void handle(QuestCompletePacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onQuestComplete(packet)));
      context.setPacketHandled(true);
   }
}
