package com.theplumteam.client.discovery;

import com.theplumteam.BlockPopsMod;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDiscoveryManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(ClientDiscoveryManager.class);
   private static final Set<String> discoveredFigures = new HashSet<>();
   private static final Map<String, String> figureSkins = new HashMap<>();
   private static final Map<String, String> figureQuickSkins = new HashMap<>();

   public static void setData(Set<String> figures, Map<String, String> skins, Map<String, String> quickSkins) {
      discoveredFigures.clear();
      discoveredFigures.addAll(figures);
      figureSkins.clear();
      figureSkins.putAll(skins);
      figureQuickSkins.clear();
      figureQuickSkins.putAll(quickSkins);
      LOGGER.debug(
         "Discovery data synced: {} figures, {} skins, {} quick skins", new Object[]{discoveredFigures.size(), figureSkins.size(), figureQuickSkins.size()}
      );
   }

   @Deprecated
   public static void setData(Set<String> figures, Map<String, String> skins) {
      setData(figures, skins, Collections.emptyMap());
   }

   @Deprecated
   public static void setData(Set<String> figures) {
      setData(figures, Collections.emptyMap(), Collections.emptyMap());
   }

   public static void unlock(String figureId) {
      if (discoveredFigures.add(figureId)) {
         BlockPopsMod.logDebug("Figure unlocked: {}", figureId);
      }
   }

   public static boolean isDiscovered(String figureId) {
      return discoveredFigures.contains(figureId);
   }

   public static Set<String> getAllDiscovered() {
      return Collections.unmodifiableSet(discoveredFigures);
   }

   public static void clear() {
      discoveredFigures.clear();
      figureSkins.clear();
      figureQuickSkins.clear();
      LOGGER.debug("Discovery data cleared");
   }

   public static void saveFigureSkin(String figureId, String skinUrl) {
      figureSkins.put(figureId, skinUrl);
   }

   @Nullable
   public static String getFigureSkin(String figureId) {
      return figureSkins.get(figureId);
   }

   public static void saveFigureQuickSkin(String figureId, String quickSkinId) {
      figureQuickSkins.put(figureId, quickSkinId);
   }

   @Nullable
   public static String getFigureQuickSkin(String figureId) {
      return figureQuickSkins.get(figureId);
   }
}
