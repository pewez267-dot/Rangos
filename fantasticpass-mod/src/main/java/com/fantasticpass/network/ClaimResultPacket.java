package com.fantasticpass.network;

import com.fantasticpass.client.ClientPacketHandler;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.progression.RewardDispatcher;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Sent server -> client after a tier claim attempt. Carries the authoritative,
 * post-attempt player data plus the result code and which track (free/premium)
 * was claimed so the open view screen can refresh and play the right sound.
 */
public final class ClaimResultPacket {
   private final int resultOrdinal;
   private final int tier;
   private final boolean premium;
   private final CompoundTag playerData;

   public ClaimResultPacket(RewardDispatcher.ClaimResult result, int tier, boolean premium, PlayerPassData data) {
      this.resultOrdinal = result.ordinal();
      this.tier = tier;
      this.premium = premium;
      this.playerData = data.toNbt();
   }

   private ClaimResultPacket(int resultOrdinal, int tier, boolean premium, CompoundTag playerData) {
      this.resultOrdinal = resultOrdinal;
      this.tier = tier;
      this.premium = premium;
      this.playerData = playerData;
   }

   public RewardDispatcher.ClaimResult getResult() {
      RewardDispatcher.ClaimResult[] values = RewardDispatcher.ClaimResult.values();
      return this.resultOrdinal >= 0 && this.resultOrdinal < values.length ? values[this.resultOrdinal] : RewardDispatcher.ClaimResult.INVALID_TIER;
   }

   public int getTier() {
      return this.tier;
   }

   public boolean isPremium() {
      return this.premium;
   }

   public PlayerPassData getPlayerData() {
      PlayerPassData data = new PlayerPassData();
      data.fromNbt(this.playerData);
      return data;
   }

   public static void encode(ClaimResultPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.resultOrdinal);
      buf.writeVarInt(packet.tier);
      buf.writeBoolean(packet.premium);
      buf.writeNbt(packet.playerData);
   }

   public static ClaimResultPacket decode(FriendlyByteBuf buf) {
      int resultOrdinal = buf.readVarInt();
      int tier = buf.readVarInt();
      boolean premium = buf.readBoolean();
      CompoundTag tag = buf.readNbt();
      return new ClaimResultPacket(resultOrdinal, tier, premium, tag == null ? new CompoundTag() : tag);
   }

   public static void handle(ClaimResultPacket packet, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onClaimResult(packet)));
      context.setPacketHandled(true);
   }
}
