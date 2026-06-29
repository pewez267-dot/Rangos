package com.fantasticpass.interop;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public final class FantasticRanksInterop {
   private static Boolean loaded;
   private static boolean resolved;
   private static Method apiMethodByUuid;
   private static Method apiMethodByPlayer;
   private static Object apiInstance;

   private FantasticRanksInterop() {
   }

   public static boolean isLoaded() {
      if (loaded == null) {
         try {
            loaded = ModList.get() != null && ModList.get().isLoaded("fantasticranks");
         } catch (Throwable var1) {
            loaded = Boolean.FALSE;
         }
      }

      return loaded;
   }

   @Nullable
   public static String getFormattedRank(Player player) {
      if (player != null && isLoaded()) {
         resolveApi();

         try {
            if (apiMethodByPlayer != null) {
               Object result = apiMethodByPlayer.invoke(apiInstance, player);
               return asString(result);
            }

            if (apiMethodByUuid != null) {
               Object result = apiMethodByUuid.invoke(apiInstance, player.getUUID());
               return asString(result);
            }
         } catch (Throwable var2) {
         }

         return null;
      } else {
         return null;
      }
   }

   private static void resolveApi() {
      if (!resolved) {
         resolved = true;
         String[] candidateClasses = new String[]{
            "com.fantasticranks.api.FantasticRanksAPI", "com.fantasticranks.FantasticRanksAPI", "net.fantasticranks.api.FantasticRanksAPI"
         };

         for (String className : candidateClasses) {
            try {
               Class<?> clazz = Class.forName(className);
               apiInstance = resolveInstance(clazz);
               apiMethodByPlayer = findMethod(clazz, Player.class, "getFormattedRank", "getDisplayRank", "getRankPrefix", "getRankString");
               apiMethodByUuid = findMethod(clazz, UUID.class, "getFormattedRank", "getDisplayRank", "getRankPrefix", "getRankString");
               if (apiMethodByPlayer != null || apiMethodByUuid != null) {
                  return;
               }
            } catch (Throwable var6) {
            }
         }
      }
   }

   @Nullable
   private static Object resolveInstance(Class<?> clazz) {
      for (String getter : new String[]{"getInstance", "instance", "get"}) {
         try {
            Method m = clazz.getMethod(getter);
            if (Modifier.isStatic(m.getModifiers())) {
               return m.invoke(null);
            }
         } catch (Throwable var6) {
         }
      }

      return null;
   }

   @Nullable
   private static Method findMethod(Class<?> clazz, Class<?> paramType, String... names) {
      for (String name : names) {
         try {
            Method m = clazz.getMethod(name, paramType);
            m.setAccessible(true);
            return m;
         } catch (Throwable var8) {
         }
      }

      return null;
   }

   @Nullable
   private static String asString(@Nullable Object result) {
      if (result == null) {
         return null;
      } else {
         String s = result.toString();
         return s.isEmpty() ? null : s;
      }
   }
}
