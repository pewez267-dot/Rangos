package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PlayerPassData;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public final class OpenViewScreenPacket {
   private final PassDefinition pass;
   private final CompoundTag playerData;
   private final int pointsPerTier;

   public OpenViewScreenPacket(PassDefinition pass, PlayerPassData playerData, int pointsPerTier) {
      this.pass = pass;
      this.playerData = playerData.toNbt();
      this.pointsPerTier = pointsPerTier;
   }

   private OpenViewScreenPacket(PassDefinition pass, CompoundTag playerData, int pointsPerTier) {
      this.pass = pass;
      this.playerData = playerData;
      this.pointsPerTier = pointsPerTier;
   }

   public PassDefinition getPass() {
      return this.pass;
   }

   public PlayerPassData getPlayerData() {
      PlayerPassData data = new PlayerPassData();
      data.fromNbt(this.playerData);
      return data;
   }

   public int getPointsPerTier() {
      return this.pointsPerTier;
   }

   public static void encode(OpenViewScreenPacket packet, FriendlyByteBuf buf) {
      packet.pass.toBuf(buf);
      buf.writeNbt(packet.playerData);
      buf.writeVarInt(packet.pointsPerTier);
   }

   public static OpenViewScreenPacket decode(FriendlyByteBuf buf) {
      PassDefinition pass = PassDefinition.fromBuf(buf);
      CompoundTag tag = buf.readNbt();
      int pointsPerTier = buf.readVarInt();
      return new OpenViewScreenPacket(pass, tag == null ? new CompoundTag() : tag, pointsPerTier);
   }

   public static void handle(OpenViewScreenPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openViewScreen(packet)));
      context.setPacketHandled(true);
   }
}
