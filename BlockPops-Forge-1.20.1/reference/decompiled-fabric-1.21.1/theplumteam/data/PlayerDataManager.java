package com.theplumteam.data;

import com.theplumteam.data.fabric.PlayerDataManagerImpl;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.injectables.annotations.ExpectPlatform.Transformed;
import java.util.WeakHashMap;
import net.minecraft.class_1657;
import net.minecraft.class_2487;

public class PlayerDataManager {
   public static final String DATA_KEY = "blockpops_discovery";
   private static final WeakHashMap<class_1657, PlayerDiscovery> cache = new WeakHashMap<>();

   public static IPlayerDiscovery getDiscovery(class_1657 player) {
      PlayerDiscovery cached = cache.get(player);
      if (cached != null) {
         return cached;
      } else {
         PlayerDiscovery discovery = new PlayerDiscovery();
         class_2487 persistentData = getPersistentData(player);
         if (persistentData.method_10573("blockpops_discovery", 10)) {
            discovery.deserializeNBT(persistentData.method_10562("blockpops_discovery"));
         }

         cache.put(player, discovery);
         return discovery;
      }
   }

   public static void saveDiscovery(class_1657 player) {
      PlayerDiscovery discovery = cache.get(player);
      if (discovery != null) {
         getPersistentData(player).method_10566("blockpops_discovery", discovery.serializeNBT());
      }
   }

   public static void markDirty(class_1657 player, IPlayerDiscovery discovery) {
      if (discovery instanceof PlayerDiscovery pd) {
         cache.put(player, pd);
         getPersistentData(player).method_10566("blockpops_discovery", pd.serializeNBT());
      }
   }

   public static void copyData(class_1657 from, class_1657 to) {
      class_2487 fromData = getPersistentData(from);
      if (fromData.method_10573("blockpops_discovery", 10)) {
         getPersistentData(to).method_10566("blockpops_discovery", fromData.method_10562("blockpops_discovery").method_10553());
         cache.remove(to);
      }
   }

   public static void clearCache(class_1657 player) {
      cache.remove(player);
   }

   @ExpectPlatform
   @Transformed
   public static class_2487 getPersistentData(class_1657 player) {
      return PlayerDataManagerImpl.getPersistentData(player);
   }
}
