package com.theplumteam.network;

import com.theplumteam.client.token.ClientTokenManager;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5455;
import net.minecraft.class_9129;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncTokenDataPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(SyncTokenDataPacket.class);
   public static final class_2960 ID = class_2960.method_60655("blockpops", "sync_token_data");
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

   public class_9129 encode() {
      class_9129 buffer = new class_9129(Unpooled.buffer(), class_5455.field_40585);
      buffer.method_53002(this.regularTokens);
      buffer.method_52974(this.ticksUntilNextRegular);
      buffer.method_52964(this.hasSpecialToken);
      buffer.method_52974(this.millisUntilNextSpecialReset);
      return buffer;
   }

   public static SyncTokenDataPacket decode(class_2540 buffer) {
      int regularTokens = buffer.readInt();
      long ticksUntilNextRegular = buffer.readLong();
      boolean hasSpecialToken = buffer.readBoolean();
      long millisUntilNextSpecialReset = buffer.readLong();
      return new SyncTokenDataPacket(regularTokens, ticksUntilNextRegular, hasSpecialToken, millisUntilNextSpecialReset);
   }

   public static void handleClient(class_2540 buf, PacketContext context) {
      SyncTokenDataPacket packet = decode(buf);
      context.queue(() -> {
         LOGGER.debug("Received token data sync: {} regular tokens, special token: {}", packet.regularTokens, packet.hasSpecialToken ? "available" : "used");
         ClientTokenManager.update(packet);
      });
   }

   public static void sendToPlayer(class_3222 player, int regularTokens, long ticksUntilNextRegular, boolean hasSpecialToken, long millisUntilNextSpecialReset) {
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
