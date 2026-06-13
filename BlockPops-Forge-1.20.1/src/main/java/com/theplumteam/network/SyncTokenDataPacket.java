package com.theplumteam.network;

import com.theplumteam.client.token.ClientTokenManager;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncTokenDataPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncTokenDataPacket.class);
   public static final ResourceLocation ID = new ResourceLocation("blockpops", "sync_token_data");
   private final int regularTokens;
   private final long ticksUntilNextRegular;
   private final boolean hasSpecialToken;
   private final long millisUntilNextSpecialReset;

   public SyncTokenDataPacket(int regularTokens, long ticksUntilNextRegular, boolean hasSpecialToken, long millisUntilNextSpecialReset) {
      this.regularTokens = regularTokens;
      this.ticksUntilNextRegular = ticksUntilNextRegular;
      this.hasSpecialToken = hasSpecialToken;
      this.millisUntilNextSpecialReset = millisUntilNextSpecialReset;
   }

   public FriendlyByteBuf encode() {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      buffer.writeInt(this.regularTokens);
      buffer.writeLong(this.ticksUntilNextRegular);
      buffer.writeBoolean(this.hasSpecialToken);
      buffer.writeLong(this.millisUntilNextSpecialReset);
      return buffer;
   }

   public static SyncTokenDataPacket decode(FriendlyByteBuf buffer) {
      int regularTokens = buffer.readInt();
      long ticksUntilNextRegular = buffer.readLong();
      boolean hasSpecialToken = buffer.readBoolean();
      long millisUntilNextSpecialReset = buffer.readLong();
      return new SyncTokenDataPacket(regularTokens, ticksUntilNextRegular, hasSpecialToken, millisUntilNextSpecialReset);
   }

   public static void handleClient(FriendlyByteBuf buf, PacketContext context) {
      SyncTokenDataPacket packet = decode(buf);
      context.queue(() -> {
         LOGGER.debug("Received token data sync: {} regular tokens, special token: {}", packet.regularTokens, packet.hasSpecialToken ? "available" : "used");
         ClientTokenManager.update(packet);
      });
   }

   public static void sendToPlayer(ServerPlayer player, int regularTokens, long ticksUntilNextRegular, boolean hasSpecialToken, long millisUntilNextSpecialReset) {
      SyncTokenDataPacket packet = new SyncTokenDataPacket(regularTokens, ticksUntilNextRegular, hasSpecialToken, millisUntilNextSpecialReset);
      NetworkManager.sendToPlayer(player, ID, packet.encode());
   }

   public int getRegularTokens() {
      return this.regularTokens;
   }

   public long getTicksUntilNextRegular() {
      return this.ticksUntilNextRegular;
   }

   public boolean hasSpecialToken() {
      return this.hasSpecialToken;
   }

   public long getMillisUntilNextSpecialReset() {
      return this.millisUntilNextSpecialReset;
   }
}
