package com.fshop.zone;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Server-side registry of transient wand selections, keyed by player UUID. */
public final class SelectionManager {
   private static final Map<UUID, PlayerSelection> SELECTIONS = new ConcurrentHashMap<>();

   private SelectionManager() {
   }

   public static PlayerSelection get(ServerPlayer player) {
      return SELECTIONS.computeIfAbsent(player.getUUID(), id -> new PlayerSelection());
   }

   public static void clear(ServerPlayer player) {
      SELECTIONS.remove(player.getUUID());
   }
}
