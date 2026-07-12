package com.fantasticpass.afk;

import com.fantasticpass.config.PassConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class AfkTracker {
   private final Map<UUID, AfkSnapshot> snapshots = new HashMap<>();
   private long serverTick;

   public void serverTick(MinecraftServer server) {
      this.serverTick++;
      int interval = Math.max(1, (Integer)PassConfig.CHECK_INTERVAL_TICKS.get());
      if (this.serverTick % (long)interval == 0L) {
         double minRotation = (Double)PassConfig.MIN_ROTATION_CHANGE_DEGREES.get();

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            this.sample(player, minRotation);
         }
      }
   }

   private void sample(ServerPlayer player, double minRotation) {
      UUID id = player.getUUID();
      BlockPos pos = player.blockPosition();
      float yRot = player.getYRot();
      float xRot = player.getXRot();
      AfkSnapshot snapshot = this.snapshots.get(id);
      if (snapshot == null) {
         this.snapshots.put(id, new AfkSnapshot(pos, yRot, xRot, this.serverTick));
      } else {
         boolean moved = !snapshot.position().equals(pos);
         boolean rotatedHorizontal = angleDifference(snapshot.yRot(), yRot) >= minRotation;
         boolean rotatedVertical = angleDifference(snapshot.xRot(), xRot) >= minRotation;
         if (moved || rotatedHorizontal || rotatedVertical) {
            snapshot.updateBaseline(pos, yRot, xRot, this.serverTick);
         }
      }
   }

   public void registerInteraction(ServerPlayer player) {
      UUID id = player.getUUID();
      AfkSnapshot snapshot = this.snapshots.get(id);
      if (snapshot == null) {
         this.snapshots.put(id, new AfkSnapshot(player.blockPosition(), player.getYRot(), player.getXRot(), this.serverTick));
      } else {
         snapshot.markInteraction(this.serverTick);
      }
   }

   public boolean isActive(ServerPlayer player) {
      AfkSnapshot snapshot = this.snapshots.get(player.getUUID());
      if (snapshot == null) {
         return true;
      } else {
         long thresholdTicks = (long)Math.max(1, (Integer)PassConfig.AFK_THRESHOLD_SECONDS.get()) * 20L;
         return this.serverTick - snapshot.lastInteractionTick() < thresholdTicks;
      }
   }

   public void remove(UUID id) {
      this.snapshots.remove(id);
   }

   public void clear() {
      this.snapshots.clear();
   }

   private static double angleDifference(float a, float b) {
      return (double)Math.abs(Mth.degreesDifference(a, b));
   }
}
