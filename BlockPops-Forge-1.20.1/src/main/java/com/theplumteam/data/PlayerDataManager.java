package com.theplumteam.data;

import com.theplumteam.data.forge.StateSaverAndLoader;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public class PlayerDataManager {
   public static final String DATA_KEY = "blockpops_discovery";
   private static final WeakHashMap<Player, PlayerDiscovery> cache = new WeakHashMap<>();

   public static IPlayerDiscovery getDiscovery(Player player) {
      PlayerDiscovery cached = cache.get(player);
      if (cached != null) {
         return cached;
      } else {
         PlayerDiscovery discovery = new PlayerDiscovery();
         CompoundTag persistentData = getPersistentData(player);
         if (persistentData.contains("blockpops_discovery", Tag.TAG_COMPOUND)) {
            discovery.deserializeNBT(persistentData.getCompound("blockpops_discovery"));
         }

         cache.put(player, discovery);
         return discovery;
      }
   }

   public static void saveDiscovery(Player player) {
      PlayerDiscovery discovery = cache.get(player);
      if (discovery != null) {
         getPersistentData(player).put("blockpops_discovery", discovery.serializeNBT());
      }
   }

   public static void markDirty(Player player, IPlayerDiscovery discovery) {
      if (discovery instanceof PlayerDiscovery pd) {
         cache.put(player, pd);
         getPersistentData(player).put("blockpops_discovery", pd.serializeNBT());
      }
   }

   public static void copyData(Player from, Player to) {
      CompoundTag fromData = getPersistentData(from);
      if (fromData.contains("blockpops_discovery", Tag.TAG_COMPOUND)) {
         getPersistentData(to).put("blockpops_discovery", fromData.getCompound("blockpops_discovery").copy());
         cache.remove(to);
      }
   }

   public static void clearCache(Player player) {
      cache.remove(player);
   }

   public static CompoundTag getPersistentData(Player player) {
      return StateSaverAndLoader.getPlayerState(player);
   }
}
