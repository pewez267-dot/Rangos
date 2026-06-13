package com.theplumteam.data.fabric;

import com.theplumteam.BlockPopsMod;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.class_1657;
import net.minecraft.class_18;
import net.minecraft.class_2487;
import net.minecraft.class_26;
import net.minecraft.class_18.class_8645;
import net.minecraft.class_7225.class_7874;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class StateSaverAndLoader extends class_18 {
   private final HashMap<UUID, class_2487> players = new HashMap<>();

   @NotNull
   public class_2487 method_75(class_2487 tag, class_7874 registries) {
      class_2487 playersTag = new class_2487();
      this.players.forEach((uuid, playerData) -> playersTag.method_10566(uuid.toString(), playerData.method_10553()));
      tag.method_10566("players", playersTag);
      return tag;
   }

   public static StateSaverAndLoader createFromTag(class_2487 tag, class_7874 registries) {
      StateSaverAndLoader state = new StateSaverAndLoader();
      class_2487 playersTag = tag.method_10562("players");
      playersTag.method_10541().forEach(key -> {
         try {
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playersTag.method_10562(key).method_10553());
         } catch (IllegalArgumentException var4) {
            BlockPopsMod.LOGGER.warn("Invalid UUID in saved data: {}", key);
         }
      });
      return state;
   }

   public static StateSaverAndLoader getServerState(MinecraftServer server) {
      class_26 persistentStateManager = server.method_30002().method_17983();
      StateSaverAndLoader state = (StateSaverAndLoader)persistentStateManager.method_17924(
         new class_8645(StateSaverAndLoader::new, StateSaverAndLoader::createFromTag, null), "blockpops_player_data"
      );
      state.method_80();
      return state;
   }

   public static class_2487 getPlayerState(class_1657 player) {
      if (player.method_37908().method_8608()) {
         return new class_2487();
      } else {
         MinecraftServer server = player.method_5682();
         if (server == null) {
            return new class_2487();
         } else {
            StateSaverAndLoader serverState = getServerState(server);
            return serverState.players.computeIfAbsent(player.method_5667(), uuid -> new class_2487());
         }
      }
   }
}
