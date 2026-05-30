package com.theplumteam.data.forge;

import com.theplumteam.BlockPopsMod;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

/**
 * UUID-keyed persistent storage for per-player BlockPops data, attached to the
 * overworld's data storage. This mirrors the Fabric implementation and survives
 * death/dimension changes automatically (data is keyed by player UUID, not entity).
 */
public class StateSaverAndLoader extends SavedData {
   private final HashMap<UUID, CompoundTag> players = new HashMap<>();

   @NotNull
   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag playersTag = new CompoundTag();
      this.players.forEach((uuid, playerData) -> playersTag.put(uuid.toString(), playerData.copy()));
      tag.put("players", playersTag);
      return tag;
   }

   public static StateSaverAndLoader createFromTag(CompoundTag tag) {
      StateSaverAndLoader state = new StateSaverAndLoader();
      CompoundTag playersTag = tag.getCompound("players");
      playersTag.getAllKeys().forEach(key -> {
         try {
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playersTag.getCompound(key).copy());
         } catch (IllegalArgumentException var4) {
            BlockPopsMod.LOGGER.warn("Invalid UUID in saved data: {}", key);
         }
      });
      return state;
   }

   public static StateSaverAndLoader getServerState(MinecraftServer server) {
      DimensionDataStorage persistentStateManager = server.overworld().getDataStorage();
      StateSaverAndLoader state = persistentStateManager.computeIfAbsent(
         new SavedData.Factory<>(StateSaverAndLoader::new, StateSaverAndLoader::createFromTag, null), "blockpops_player_data"
      );
      state.setDirty();
      return state;
   }

   /**
    * Returns the stored data compound for a given UUID without creating a new entry,
    * or an empty compound if none exists. Used to look up offline players' data.
    */
   public static CompoundTag getExistingPlayerState(MinecraftServer server, UUID uuid) {
      StateSaverAndLoader state = getServerState(server);
      CompoundTag existing = state.players.get(uuid);
      return existing != null ? existing : new CompoundTag();
   }

   public static CompoundTag getPlayerState(Player player) {
      if (player.level().isClientSide()) {
         return new CompoundTag();
      } else {
         MinecraftServer server = player.getServer();
         if (server == null) {
            return new CompoundTag();
         } else {
            StateSaverAndLoader serverState = getServerState(server);
            return serverState.players.computeIfAbsent(player.getUUID(), uuid -> new CompoundTag());
         }
      }
   }
}
